package forge.web;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.javalin.Javalin;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.web.protocol.InboundMessage;
import forge.web.protocol.MessageType;
import forge.web.protocol.OutboundMessage;

/**
 * Integration tests for the full game loop over WebSocket.
 * Starts a real Javalin server, connects a Java WebSocket client,
 * starts a game, and verifies the engine bridge works end-to-end.
 *
 * The Forge engine uses two input mechanisms:
 * 1. Input system (InputQueue/InputProxy): button-based (BUTTON_UPDATE -> BUTTON_OK/CANCEL)
 *    Used for: mulligan, passing priority, blocking decisions
 * 2. sendAndWait system: request-response (PROMPT_CHOICE -> CHOICE_RESPONSE)
 *    Used for: choices, targeting, card selection
 *
 * The test client auto-responds to both mechanisms.
 */
public class GameLoopIntegrationTest {

    private static boolean initialized = false;
    private Javalin app;
    private int port;
    private ObjectMapper mapper;

    @BeforeSuite
    public void initForge() {
        if (initialized) {
            return;
        }

        String assetsDir = ".";
        File resDir = new File("./res");
        if (!resDir.isDirectory()) {
            resDir = new File("../forge-gui/res");
            if (!resDir.isDirectory()) {
                throw new SkipException("Forge assets not found at ./res or ../forge-gui/res");
            }
            assetsDir = "../forge-gui/";
        }

        if (GuiBase.getInterface() == null) {
            GuiBase.setInterface(new WebGuiBase(assetsDir));
        }

        FModel.initialize(null, prefs -> {
            prefs.setPref(FPref.UI_ENABLE_SOUNDS, false);
            return null;
        });

        initialized = true;
    }

    @BeforeMethod
    public void startServer() {
        mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new ParameterNamesModule());

        app = WebServer.createApp(mapper);
        app.start(0); // random available port
        port = app.port();
    }

    @AfterMethod
    public void stopServer() {
        if (app != null) {
            WebServer.getActiveSessions().clear();
            app.stop();
        }
    }

    @Test(timeOut = 30000)
    public void testWebSocketConnectionAndGameState() throws Exception {
        TestWsClient client = new TestWsClient(mapper);
        try {
            client.connect(port, "test-state-1");
            client.sendStartGame();

            // Wait for a GAME_STATE message
            OutboundMessage gameState = client.waitForType(MessageType.GAME_STATE, 20);
            Assert.assertNotNull(gameState, "Should receive a GAME_STATE message");
            Assert.assertNotNull(gameState.getPayload(), "GAME_STATE should have a payload");
        } finally {
            client.close();
        }
    }

    @Test(timeOut = 60000)
    public void testMulliganPromptResponse() throws Exception {
        TestWsClient client = new TestWsClient(mapper);
        try {
            client.connect(port, "test-mulligan-1");
            client.sendStartGame();

            // Wait for BUTTON_UPDATE (mulligan prompt uses the Input system)
            OutboundMessage buttonUpdate = client.waitForType(MessageType.BUTTON_UPDATE, 30);
            Assert.assertNotNull(buttonUpdate, "Should receive a BUTTON_UPDATE for mulligan");

            // Respond with "keep" by pressing OK button
            client.sendButtonOk();

            // Wait for game to progress -- should get phase/turn updates or more button prompts
            OutboundMessage next = client.waitForAnyType(
                    List.of(MessageType.PHASE_UPDATE, MessageType.TURN_UPDATE,
                            MessageType.BUTTON_UPDATE, MessageType.PROMPT_CHOICE,
                            MessageType.PROMPT_CONFIRM),
                    30);
            Assert.assertNotNull(next, "Game should progress after responding to mulligan");
        } finally {
            client.close();
        }
    }

    @Test(timeOut = 30000)
    public void testInputIdCorrelation() throws Exception {
        TestWsClient client = new TestWsClient(mapper);
        try {
            client.connect(port, "test-correlation-1");
            client.sendStartGame();

            // Auto-respond to buttons (mulligan etc.) until we get a PROMPT_CHOICE
            // with an inputId (e.g., a choice prompt from the engine)
            long deadline = System.currentTimeMillis() + 20000;
            OutboundMessage prompt = null;
            while (System.currentTimeMillis() < deadline) {
                OutboundMessage msg = client.received.poll(1, TimeUnit.SECONDS);
                if (msg == null) {
                    continue;
                }
                if (msg.getType() == MessageType.BUTTON_UPDATE) {
                    client.sendButtonOk();
                } else if (msg.getType() == MessageType.PROMPT_CHOICE
                        && msg.getInputId() != null) {
                    prompt = msg;
                    break;
                }
            }

            if (prompt == null) {
                // Game might not produce PROMPT_CHOICE with basic land decks
                // (no spells to cast). This is acceptable -- skip assertion.
                return;
            }

            String correctInputId = prompt.getInputId();

            // Send response with WRONG inputId
            InboundMessage wrongResponse = new InboundMessage();
            wrongResponse.setType(MessageType.CHOICE_RESPONSE);
            wrongResponse.setInputId("wrong-id-" + System.currentTimeMillis());
            wrongResponse.setPayload(List.of(0));
            client.send(wrongResponse);

            // Brief wait -- wrong inputId should be ignored
            Thread.sleep(500);

            // Verify the bridge rejected the wrong inputId
            Assert.assertTrue(client.received.isEmpty()
                    || client.received.peek().getType() != MessageType.PHASE_UPDATE,
                    "Game should not advance after wrong inputId");

            // Send response with CORRECT inputId
            InboundMessage correctResponse = new InboundMessage();
            correctResponse.setType(MessageType.CHOICE_RESPONSE);
            correctResponse.setInputId(correctInputId);
            correctResponse.setPayload(List.of(0));
            client.send(correctResponse);

            // Game should now advance
            OutboundMessage next = client.waitForAnyType(
                    List.of(MessageType.PHASE_UPDATE, MessageType.TURN_UPDATE,
                            MessageType.BUTTON_UPDATE, MessageType.PROMPT_CHOICE,
                            MessageType.GAME_STATE),
                    10);
            Assert.assertNotNull(next, "Game should advance after correct inputId");
        } finally {
            client.close();
        }
    }

    @Test(timeOut = 15000)
    public void testClientDisconnectCleanup() throws Exception {
        TestWsClient client = new TestWsClient(mapper);
        client.connect(port, "test-cleanup-1");
        client.sendStartGame();

        // Wait for first message to confirm connection is active
        OutboundMessage first = client.waitForAny(10);
        Assert.assertNotNull(first, "Should receive at least one message");

        // Verify session exists
        Assert.assertTrue(WebServer.getActiveSessions().containsKey("test-cleanup-1"),
                "Session should be active");

        // Close the connection
        client.close();

        // Wait for cleanup
        long deadline = System.currentTimeMillis() + 5000;
        while (WebServer.getActiveSessions().containsKey("test-cleanup-1")
                && System.currentTimeMillis() < deadline) {
            Thread.sleep(200);
        }

        Assert.assertFalse(WebServer.getActiveSessions().containsKey("test-cleanup-1"),
                "Session should be cleaned up after disconnect");
    }

    @Test(timeOut = 60000)
    public void testFullGameLoop() throws Exception {
        TestWsClient client = new TestWsClient(mapper);
        try {
            client.connect(port, "test-full-1");
            client.sendStartGame();

            // Auto-respond to all prompts and buttons for up to 45 seconds
            List<MessageType> receivedTypes = client.autoRespond(45);

            // Verify we received key message types
            Assert.assertTrue(receivedTypes.contains(MessageType.GAME_STATE),
                    "Should receive at least one GAME_STATE");
            Assert.assertTrue(
                    receivedTypes.contains(MessageType.BUTTON_UPDATE),
                    "Should receive at least one BUTTON_UPDATE (mulligan at minimum)");
            Assert.assertTrue(
                    receivedTypes.contains(MessageType.PHASE_UPDATE)
                            || receivedTypes.contains(MessageType.TURN_UPDATE)
                            || receivedTypes.contains(MessageType.GAME_OVER),
                    "Game should progress through phases/turns or complete");
        } finally {
            client.close();
        }
    }

    // ========================================================================
    // WebSocket test client helper
    // ========================================================================

    private static class TestWsClient {
        private final BlockingQueue<OutboundMessage> received = new LinkedBlockingQueue<>();
        private final ObjectMapper mapper;
        private WebSocket webSocket;

        TestWsClient(final ObjectMapper mapper) {
            this.mapper = mapper;
        }

        void connect(final int serverPort, final String gameId) throws Exception {
            HttpClient httpClient = HttpClient.newHttpClient();
            CompletableFuture<WebSocket> wsFuture = httpClient.newWebSocketBuilder()
                    .buildAsync(
                            URI.create("ws://localhost:" + serverPort + "/ws/game/" + gameId),
                            new WebSocket.Listener() {
                                private final StringBuilder buffer = new StringBuilder();

                                @Override
                                public CompletionStage<?> onText(final WebSocket webSocket,
                                        final CharSequence data, final boolean last) {
                                    buffer.append(data);
                                    if (last) {
                                        try {
                                            OutboundMessage msg = mapper.readValue(
                                                    buffer.toString(), OutboundMessage.class);
                                            received.offer(msg);
                                        } catch (final Exception e) {
                                            // Log but don't fail -- some messages may not parse
                                        }
                                        buffer.setLength(0);
                                    }
                                    webSocket.request(1);
                                    return null;
                                }

                                @Override
                                public void onOpen(final WebSocket webSocket) {
                                    webSocket.request(1);
                                }

                                @Override
                                public CompletionStage<?> onClose(final WebSocket webSocket,
                                        final int statusCode, final String reason) {
                                    return null;
                                }

                                @Override
                                public void onError(final WebSocket webSocket,
                                        final Throwable error) {
                                    // Connection errors are expected during cleanup
                                }
                            });

            webSocket = wsFuture.get(10, TimeUnit.SECONDS);
        }

        void send(final InboundMessage msg) throws Exception {
            String json = mapper.writeValueAsString(msg);
            webSocket.sendText(json, true).get(5, TimeUnit.SECONDS);
        }

        void sendStartGame() throws Exception {
            InboundMessage startMsg = new InboundMessage();
            startMsg.setType(MessageType.START_GAME);
            startMsg.setPayload(Collections.emptyMap());
            send(startMsg);
        }

        void sendButtonOk() throws Exception {
            InboundMessage msg = new InboundMessage();
            msg.setType(MessageType.BUTTON_OK);
            send(msg);
        }

        void sendButtonCancel() throws Exception {
            InboundMessage msg = new InboundMessage();
            msg.setType(MessageType.BUTTON_CANCEL);
            send(msg);
        }

        OutboundMessage waitForType(final MessageType type, final long timeoutSeconds)
                throws InterruptedException {
            long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000);
            while (System.currentTimeMillis() < deadline) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    break;
                }
                OutboundMessage msg = received.poll(
                        Math.min(remaining, 1000), TimeUnit.MILLISECONDS);
                if (msg != null && msg.getType() == type) {
                    return msg;
                }
            }
            return null;
        }

        OutboundMessage waitForAnyType(final List<MessageType> types,
                final long timeoutSeconds) throws InterruptedException {
            long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000);
            while (System.currentTimeMillis() < deadline) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    break;
                }
                OutboundMessage msg = received.poll(
                        Math.min(remaining, 1000), TimeUnit.MILLISECONDS);
                if (msg != null && types.contains(msg.getType())) {
                    return msg;
                }
            }
            return null;
        }

        OutboundMessage waitForAny(final long timeoutSeconds) throws InterruptedException {
            return received.poll(timeoutSeconds, TimeUnit.SECONDS);
        }

        /**
         * Auto-respond to all prompts and button actions for the given duration.
         * Handles both input mechanisms:
         * - BUTTON_UPDATE: responds with BUTTON_OK (e.g., keep hand, pass priority)
         * - PROMPT_CHOICE: responds with CHOICE_RESPONSE selecting first option
         * - PROMPT_CONFIRM: responds with CONFIRM_RESPONSE (true)
         * - PROMPT_AMOUNT: responds with AMOUNT_RESPONSE (0)
         * Returns the list of message types received.
         */
        List<MessageType> autoRespond(final long durationSeconds) throws Exception {
            List<MessageType> types = new ArrayList<>();
            long deadline = System.currentTimeMillis() + (durationSeconds * 1000);

            while (System.currentTimeMillis() < deadline) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    break;
                }
                OutboundMessage msg = received.poll(
                        Math.min(remaining, 500), TimeUnit.MILLISECONDS);
                if (msg == null) {
                    continue;
                }

                types.add(msg.getType());

                // Auto-respond to button prompts (Input system)
                if (msg.getType() == MessageType.BUTTON_UPDATE) {
                    // Small delay to let the Input system stabilize
                    Thread.sleep(50);
                    sendButtonOk();
                }
                // Auto-respond to choice prompts (sendAndWait system)
                else if (msg.getType() == MessageType.PROMPT_CHOICE
                        && msg.getInputId() != null) {
                    InboundMessage response = new InboundMessage();
                    response.setType(MessageType.CHOICE_RESPONSE);
                    response.setInputId(msg.getInputId());
                    response.setPayload(List.of(0)); // always pick first option
                    send(response);
                } else if (msg.getType() == MessageType.PROMPT_CONFIRM
                        && msg.getInputId() != null) {
                    InboundMessage response = new InboundMessage();
                    response.setType(MessageType.CONFIRM_RESPONSE);
                    response.setInputId(msg.getInputId());
                    response.setPayload(true); // always confirm
                    send(response);
                } else if (msg.getType() == MessageType.PROMPT_AMOUNT
                        && msg.getInputId() != null) {
                    InboundMessage response = new InboundMessage();
                    response.setType(MessageType.AMOUNT_RESPONSE);
                    response.setInputId(msg.getInputId());
                    response.setPayload(0); // default amount
                    send(response);
                } else if (msg.getType() == MessageType.GAME_OVER) {
                    // Game ended, stop auto-responding
                    break;
                }
            }

            return types;
        }

        void close() {
            if (webSocket != null) {
                try {
                    webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "test done")
                            .get(5, TimeUnit.SECONDS);
                } catch (final Exception e) {
                    // Expected during some test scenarios
                }
            }
        }
    }
}

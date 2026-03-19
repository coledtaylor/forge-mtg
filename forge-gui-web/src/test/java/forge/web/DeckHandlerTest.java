package forge.web;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

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
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 * Integration tests for the deck CRUD REST endpoints.
 * Requires FModel initialization. Creates and cleans up test decks.
 */
public class DeckHandlerTest {

    private static final String TEST_DECK_NAME = "GSD Test Deck";
    private static boolean initialized = false;
    private Javalin app;
    private int port;
    private ObjectMapper mapper;
    private HttpClient httpClient;

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
                throw new SkipException(
                        "Forge assets not found at ./res or ../forge-gui/res");
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
        app.start(0);
        port = app.port();
        httpClient = HttpClient.newHttpClient();
    }

    @AfterMethod
    public void stopServer() {
        // Clean up test deck file
        File testDeck = new File(
                ForgeConstants.DECK_CONSTRUCTED_DIR, TEST_DECK_NAME + ".dck");
        if (testDeck.exists()) {
            testDeck.delete();
        }

        if (app != null) {
            WebServer.getActiveSessions().clear();
            app.stop();
        }
    }

    @Test(timeOut = 30000)
    @SuppressWarnings("unchecked")
    public void testCreateAndListDeck() throws Exception {
        // Create deck
        String createBody = mapper.writeValueAsString(
                Map.of("name", TEST_DECK_NAME));
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/decks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createBody))
                .build();

        HttpResponse<String> createResp = httpClient.send(
                createReq, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(createResp.statusCode(), 201,
                "Create should return 201");

        // List decks
        HttpRequest listReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/decks"))
                .GET().build();

        HttpResponse<String> listResp = httpClient.send(
                listReq, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(listResp.statusCode(), 200);

        List<Map<String, Object>> decks = mapper.readValue(
                listResp.body(), List.class);
        boolean found = decks.stream()
                .anyMatch(d -> TEST_DECK_NAME.equals(d.get("name")));
        Assert.assertTrue(found,
                "Deck list should contain '" + TEST_DECK_NAME + "'");
    }

    @Test(timeOut = 30000)
    @SuppressWarnings("unchecked")
    public void testGetDeck() throws Exception {
        // Create first
        String createBody = mapper.writeValueAsString(
                Map.of("name", TEST_DECK_NAME));
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/decks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createBody))
                .build();
        httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());

        // Get deck
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/api/decks/" + TEST_DECK_NAME.replace(" ", "%20")))
                .GET().build();

        HttpResponse<String> getResp = httpClient.send(
                getReq, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(getResp.statusCode(), 200);

        Map<String, Object> deck = mapper.readValue(
                getResp.body(), Map.class);
        Assert.assertEquals(deck.get("name"), TEST_DECK_NAME);
        Assert.assertNotNull(deck.get("main"), "main section should exist");
    }

    @Test(timeOut = 30000)
    public void testDeleteDeck() throws Exception {
        // Create first
        String createBody = mapper.writeValueAsString(
                Map.of("name", TEST_DECK_NAME));
        HttpRequest createReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/api/decks"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(createBody))
                .build();
        httpClient.send(createReq, HttpResponse.BodyHandlers.ofString());

        // Delete
        HttpRequest deleteReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/api/decks/" + TEST_DECK_NAME.replace(" ", "%20")))
                .DELETE().build();

        HttpResponse<String> deleteResp = httpClient.send(
                deleteReq, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(deleteResp.statusCode(), 204,
                "Delete should return 204");

        // Verify gone
        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/api/decks/" + TEST_DECK_NAME.replace(" ", "%20")))
                .GET().build();

        HttpResponse<String> getResp = httpClient.send(
                getReq, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(getResp.statusCode(), 404,
                "Get after delete should return 404");
    }

    @Test(timeOut = 30000)
    public void testGetNonexistentDeck() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port
                        + "/api/decks/NoSuchDeck"))
                .GET().build();

        HttpResponse<String> resp = httpClient.send(
                req, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(resp.statusCode(), 404,
                "Should return 404 for non-existent deck");
    }
}

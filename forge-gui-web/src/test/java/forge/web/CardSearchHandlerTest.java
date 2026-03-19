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
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 * Integration tests for the card search REST endpoint.
 * Requires FModel initialization with real card data.
 */
public class CardSearchHandlerTest {

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
        if (app != null) {
            WebServer.getActiveSessions().clear();
            app.stop();
        }
    }

    @Test(timeOut = 30000)
    @SuppressWarnings("unchecked")
    public void testSearchByName() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + port + "/api/cards?q=lightning"))
                .GET().build();

        HttpResponse<String> resp =
                httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(resp.statusCode(), 200);

        Map<String, Object> body = mapper.readValue(
                resp.body(), Map.class);
        List<Map<String, Object>> cards =
                (List<Map<String, Object>>) body.get("cards");
        int total = ((Number) body.get("total")).intValue();

        Assert.assertTrue(total > 0, "Should find cards with 'lightning'");
        Assert.assertFalse(cards.isEmpty(), "Cards list should not be empty");

        String firstName = ((String) cards.get(0).get("name")).toLowerCase();
        Assert.assertTrue(firstName.contains("lightning"),
                "First card name should contain 'lightning', got: " + firstName);
    }

    @Test(timeOut = 30000)
    @SuppressWarnings("unchecked")
    public void testSearchPagination() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + port
                                + "/api/cards?q=a&page=1&limit=5"))
                .GET().build();

        HttpResponse<String> resp =
                httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(resp.statusCode(), 200);

        Map<String, Object> body = mapper.readValue(
                resp.body(), Map.class);
        List<Map<String, Object>> cards =
                (List<Map<String, Object>>) body.get("cards");
        int totalPages = ((Number) body.get("totalPages")).intValue();

        Assert.assertTrue(cards.size() <= 5,
                "Should return at most 5 cards");
        Assert.assertTrue(totalPages > 1,
                "Should have more than 1 page");
    }

    @Test(timeOut = 30000)
    @SuppressWarnings("unchecked")
    public void testSearchWithNoResults() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + port
                                + "/api/cards?q=xyznonexistent123"))
                .GET().build();

        HttpResponse<String> resp =
                httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(resp.statusCode(), 200);

        Map<String, Object> body = mapper.readValue(
                resp.body(), Map.class);
        List<Map<String, Object>> cards =
                (List<Map<String, Object>>) body.get("cards");
        int total = ((Number) body.get("total")).intValue();

        Assert.assertTrue(cards.isEmpty(), "Cards list should be empty");
        Assert.assertEquals(total, 0, "Total should be 0");
    }

    @Test(timeOut = 30000)
    @SuppressWarnings("unchecked")
    public void testCardSearchDtoHasScryfallFields() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://localhost:" + port
                                + "/api/cards?q=lightning+bolt&limit=1"))
                .GET().build();

        HttpResponse<String> resp =
                httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        Assert.assertEquals(resp.statusCode(), 200);

        Map<String, Object> body = mapper.readValue(
                resp.body(), Map.class);
        List<Map<String, Object>> cards =
                (List<Map<String, Object>>) body.get("cards");

        Assert.assertFalse(cards.isEmpty(),
                "Should find Lightning Bolt");
        Map<String, Object> card = cards.get(0);
        Assert.assertNotNull(card.get("setCode"),
                "setCode should not be null");
        Assert.assertNotNull(card.get("collectorNumber"),
                "collectorNumber should not be null");
    }
}

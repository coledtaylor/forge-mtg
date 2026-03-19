package forge.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;

import org.tinylog.Logger;

import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 * Entry point for the Forge web server.
 * Initializes the engine in correct order: GuiBase -> FModel -> Javalin.
 */
public class WebServer {

    private WebServer() { } // no instances

    public static void main(String[] args) {
        // Step 1 (MUST BE FIRST): Set up the GUI interface before any Forge class loading
        String assetsDir = determineAssetsDir(args);
        Logger.info("Setting up WebGuiBase with assets dir: {}", assetsDir);
        GuiBase.setInterface(new WebGuiBase(assetsDir));

        // Step 2: Initialize FModel with headless preferences
        Logger.info("Initializing FModel...");
        FModel.initialize(null, preferences -> {
            preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
            preferences.setPref(FPref.UI_LANGUAGE, "en-US");
            preferences.setPref(FPref.UI_ENABLE_SOUNDS, false);
            return null;
        });
        Logger.info("FModel initialized successfully");

        // Step 3: Create and start Javalin server
        Logger.info("Starting Javalin on port 8080...");
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .registerModule(new ParameterNamesModule());

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson(mapper, false));
            config.routes.get("/health", ctx -> ctx.result("ok"));
        });

        app.start(8080);

        // Shutdown hook for clean teardown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("Shutting down...");
            app.stop();
            ((WebGuiBase) GuiBase.getInterface()).shutdown();
        }));

        Logger.info("Forge web server started on port 8080");
    }

    private static String determineAssetsDir(String[] args) {
        if (args.length > 0) {
            return args[0];
        }
        String sysProp = System.getProperty("forge.assets.dir");
        if (sysProp != null) {
            return sysProp;
        }
        return ".";
    }
}

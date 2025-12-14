/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view;

import forge.GuiDesktop;
import forge.Singletons;
import forge.error.ExceptionHandler;
import forge.gui.GuiBase;
import forge.gui.card.CardReaderExperiments;
import forge.util.BuildInfo;
import io.sentry.Sentry;

/**
 * Main class for Forge's swing application view.
 */
public final class Main {
    /**
     * Main entry point for Forge
     */
    public static void main(final String[] args) {
        try {
            Sentry.init(options -> {
                options.setEnableExternalConfiguration(true);
                options.setRelease(BuildInfo.getVersionString());
                options.setEnvironment(System.getProperty("os.name"));
                options.setTag("Java Version", System.getProperty("java.version"));
                options.setShutdownTimeoutMillis(5000);
                if (options.getDsn() == null || options.getDsn().isEmpty())
                    options.setDsn("https://87bc8d329e49441895502737c069067b@sentry.cardforge.org//3");
            }, true);

            System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
            System.setProperty("sun.java2d.d3d", "false");

            // Debug: Print working directory
            System.out.println("Working Directory: " + System.getProperty("user.dir"));
            System.out.println("Java Version: " + System.getProperty("java.version"));

            GuiBase.setInterface(new GuiDesktop());

            ExceptionHandler.registerErrorHandling();

            if (args.length == 0) {
                Singletons.initializeOnce(true);
                Singletons.getControl().initialize();

                // Debug: Print JUMPSTART info (after full initialization)
//                javax.swing.SwingUtilities.invokeLater(() -> {
//                    System.out.println("\n=== JUMPSTART DEBUG INFO ===");
//                    System.out.println("ASSETS_DIR: " + forge.localinstance.properties.ForgeConstants.ASSETS_DIR);
//                    System.out.println("RES_DIR: " + forge.localinstance.properties.ForgeConstants.RES_DIR);
//
//                    // Debug: Check if editor.xml exists and contains JUMPSTART
//                    java.io.File editorXml = new java.io.File(forge.localinstance.properties.ForgeConstants.RES_DIR + "defaults" + java.io.File.separator + "editor.xml");
//                    System.out.println("editor.xml exists: " + editorXml.exists());
//                    System.out.println("editor.xml path: " + editorXml.getAbsolutePath());
//                    if (editorXml.exists()) {
//                        try {
//                            String content = new String(java.nio.file.Files.readAllBytes(editorXml.toPath()));
//                            System.out.println("editor.xml contains JUMPSTART: " + content.contains("EDITOR_JUMPSTART"));
//                        } catch (Exception e) {
//                            System.err.println("Error reading editor.xml: " + e.getMessage());
//                        }
//                    }
//
//                    // Debug: Check EDocID enum (safe now that everything is initialized)
//                    System.out.println("EDITOR_JUMPSTART enum exists: " + (forge.gui.framework.EDocID.EDITOR_JUMPSTART != null));
//                    System.out.println("All EDocID values: ");
//                    for (EDocID id : forge.gui.framework.EDocID.values()) {
//                        if (id.name().contains("JUMPSTART") || id.name().contains("COMMANDER") || id.name().contains("BRAWL")) {
//                            System.out.println("  - " + id.name() + " -> " + id.getDoc());
//                        }
//                    }
//                    System.out.println("=== END DEBUG INFO ===\n");
//                });

                return;
            }

            String mode = args[0].toLowerCase();
            switch (mode) {
                case "sim":
                    SimulateMatch.simulate(args);
                    break;
                case "parse":
                    CardReaderExperiments.parseAllCards(args);
                    break;
                case "server":
                    System.out.println("Dedicated server mode.\nNot implemented.");
                    break;
                default:
                    System.out.println("Unknown mode.\nKnown mode is 'sim', 'parse' ");
                    break;
            }
            System.exit(0);

        } catch (Throwable t) {
            System.err.println("=== FATAL ERROR DURING STARTUP ===");
            t.printStackTrace(System.err);

            // Get the root cause
            Throwable cause = t;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            System.err.println("\n=== ROOT CAUSE ===");
            cause.printStackTrace(System.err);

            // Write to crash log file
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter("forge-crash.log", true))) {
                pw.println("\n=== Crash at " + new java.util.Date() + " ===");
                t.printStackTrace(pw);
                pw.println("\n=== ROOT CAUSE ===");
                cause.printStackTrace(pw);
            } catch (Exception ignored) {}

            System.exit(1);
        }
    }


    @SuppressWarnings("deprecation")
    @Override
    protected void finalize() throws Throwable {
        try {
            ExceptionHandler.unregisterErrorHandling();
        } finally {
            super.finalize();
        }
    }

    // disallow instantiation
    private Main() {
    }
}

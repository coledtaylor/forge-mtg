package forge.web;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

/**
 * Integration test that proves FModel initializes headlessly
 * using WebGuiBase without any Swing or SoundSystem dependencies.
 */
public class HeadlessInitTest {

    private static boolean initialized = false;

    @BeforeSuite
    public void initForge() {
        if (initialized) {
            return;
        }

        // Determine assets directory: try current dir, then parent forge-gui
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

    @Test
    public void testGuiBaseSetBeforeForgeConstants() {
        Assert.assertNotNull(GuiBase.getInterface(),
                "GuiBase interface should be set");
        Assert.assertTrue(GuiBase.getInterface() instanceof WebGuiBase,
                "GuiBase interface should be a WebGuiBase instance");
    }

    @Test
    public void testStaticDataLoaded() {
        Assert.assertNotNull(StaticData.instance(),
                "StaticData instance should not be null");
        Assert.assertNotNull(StaticData.instance().getCommonCards(),
                "Common cards database should not be null");
        Assert.assertTrue(StaticData.instance().getCommonCards().getAllCards().size() > 0,
                "Card database should contain cards");
    }

    @Test
    public void testFModelPreferencesSet() {
        Assert.assertEquals(FModel.getPreferences().getPref(FPref.UI_ENABLE_SOUNDS), "false",
                "UI_ENABLE_SOUNDS should be false");
    }

    @Test
    public void testEdtThreadIsNotGameThread() {
        AtomicReference<String> threadName = new AtomicReference<>();

        FThreads.invokeInEdtAndWait(() ->
                threadName.set(Thread.currentThread().getName()));

        Assert.assertEquals(threadName.get(), "Web-EDT",
                "EDT runnable should execute on Web-EDT thread");
        Assert.assertFalse(threadName.get().startsWith("Game"),
                "EDT thread should not start with 'Game'");
    }
}

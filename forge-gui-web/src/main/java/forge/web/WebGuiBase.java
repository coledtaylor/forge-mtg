package forge.web;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.jupnp.UpnpServiceConfiguration;
import org.tinylog.Logger;

import forge.gamemodes.match.HostedMatch;
import forge.gui.download.GuiDownloadService;
import forge.gui.interfaces.IGuiBase;
import forge.gui.interfaces.IGuiGame;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.sound.IAudioClip;
import forge.sound.IAudioMusic;
import forge.util.FSerializableFunction;
import forge.util.ImageFetcher;

/**
 * Headless IGuiBase implementation for the web server context.
 * Uses a single-threaded executor as a pseudo-EDT (named "Web-EDT")
 * instead of Swing's Event Dispatch Thread.
 */
public class WebGuiBase implements IGuiBase {

    private final String assetsDir;
    private final ExecutorService edtExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Web-EDT");
        t.setDaemon(true);
        return t;
    });

    public WebGuiBase(String assetsDir) {
        this.assetsDir = assetsDir;
    }

    public void shutdown() {
        edtExecutor.shutdownNow();
    }

    // ========== EDT / Threading ==========

    @Override
    public boolean isGuiThread() {
        return Thread.currentThread().getName().equals("Web-EDT");
    }

    @Override
    public void invokeInEdtLater(Runnable runnable) {
        edtExecutor.submit(runnable);
    }

    @Override
    public void invokeInEdtAndWait(Runnable proc) {
        if (isGuiThread()) {
            proc.run();
        } else {
            try {
                edtExecutor.submit(proc).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void invokeInEdtNow(Runnable runnable) {
        runnable.run();
    }

    // ========== Identity ==========

    @Override
    public boolean isRunningOnDesktop() {
        return false;
    }

    @Override
    public boolean isLibgdxPort() {
        return false;
    }

    @Override
    public String getCurrentVersion() {
        return "forge-gui-web";
    }

    @Override
    public String getAssetsDir() {
        return assetsDir;
    }

    @Override
    public ImageFetcher getImageFetcher() {
        return null;
    }

    // ========== Skin / Image (no-op for headless) ==========

    @Override
    public ISkinImage getSkinIcon(FSkinProp skinProp) {
        return null;
    }

    @Override
    public ISkinImage getUnskinnedIcon(String path) {
        return null;
    }

    @Override
    public ISkinImage getCardArt(PaperCard card) {
        return null;
    }

    @Override
    public ISkinImage getCardArt(PaperCard card, boolean backFace) {
        return null;
    }

    @Override
    public ISkinImage createLayeredImage(PaperCard card, FSkinProp background,
            String overlayFilename, float opacity) {
        return null;
    }

    // ========== Dialogs (log and return defaults) ==========

    @Override
    public void showBugReportDialog(String title, String text, boolean showExitAppBtn) {
        Logger.error("Bug Report - {}: {}", title, text);
    }

    @Override
    public void showImageDialog(ISkinImage image, String message, String title) {
        Logger.info("Image Dialog - {}: {}", title, message);
    }

    @Override
    public int showOptionDialog(String message, String title, FSkinProp icon,
            List<String> options, int defaultOption) {
        Logger.info("Option Dialog - {}: {}", title, message);
        return defaultOption;
    }

    @Override
    public String showInputDialog(String message, String title, FSkinProp icon,
            String initialInput, List<String> inputOptions, boolean isNumeric) {
        Logger.info("Input Dialog - {}: {}", title, message);
        if (initialInput != null) {
            return initialInput;
        }
        if (inputOptions != null && !inputOptions.isEmpty()) {
            return inputOptions.get(0);
        }
        return isNumeric ? "0" : "";
    }

    @Override
    public <T> List<T> getChoices(String message, int min, int max,
            Collection<T> choices, Collection<T> selected,
            FSerializableFunction<T, String> display) {
        Logger.info("Choices Dialog: {}", message);
        return Collections.emptyList();
    }

    @Override
    public <T> List<T> order(String title, String top,
            int remainingObjectsMin, int remainingObjectsMax,
            List<T> sourceChoices, List<T> destChoices) {
        Logger.info("Order Dialog: {}", title);
        return sourceChoices != null ? sourceChoices : Collections.emptyList();
    }

    @Override
    public String showFileDialog(String title, String defaultDir) {
        return null;
    }

    @Override
    public File getSaveFile(File defaultFile) {
        return null;
    }

    // ========== Downloads / UI actions ==========

    @Override
    public void download(GuiDownloadService service, Consumer<Boolean> callback) {
        Logger.info("Download requested (no-op in web mode)");
    }

    @Override
    public void refreshSkin() {
        // no-op
    }

    @Override
    public void showCardList(String title, String message, List<PaperCard> list) {
        Logger.info("Card List - {}: {} cards", title, list != null ? list.size() : 0);
    }

    @Override
    public boolean showBoxedProduct(String title, String message, List<PaperCard> list) {
        Logger.info("Boxed Product - {}: {} cards", title, list != null ? list.size() : 0);
        return false;
    }

    @Override
    public PaperCard chooseCard(String title, String message, List<PaperCard> list) {
        Logger.info("Choose Card - {}: {}", title, message);
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    @Override
    public int getAvatarCount() {
        return 0;
    }

    @Override
    public int getSleevesCount() {
        return 0;
    }

    @Override
    public void copyToClipboard(String text) {
        Logger.info("Copy to clipboard (no-op in web mode)");
    }

    @Override
    public void browseToUrl(String url) {
        Logger.info("Browse to URL: {}", url);
    }

    // ========== Audio (disabled in headless) ==========

    @Override
    public boolean isSupportedAudioFormat(File file) {
        return false;
    }

    @Override
    public IAudioClip createAudioClip(String filename) {
        return null;
    }

    @Override
    public IAudioMusic createAudioMusic(String filename) {
        return null;
    }

    @Override
    public void startAltSoundSystem(String filename, boolean isSynchronized) {
        // no-op
    }

    @Override
    public void clearImageCache() {
        // no-op
    }

    // ========== Game hosting ==========

    @Override
    public void showSpellShop() {
        Logger.info("Spell Shop requested (no-op in web mode)");
    }

    @Override
    public void showBazaar() {
        Logger.info("Bazaar requested (no-op in web mode)");
    }

    @Override
    public IGuiGame getNewGuiGame() {
        return null; // Will be implemented in Phase 4
    }

    @Override
    public HostedMatch hostMatch() {
        return new HostedMatch();
    }

    @Override
    public void runBackgroundTask(String message, Runnable task) {
        new Thread(task, "Web-Background").start();
    }

    @Override
    public String encodeSymbols(String str, boolean formatReminderText) {
        return str; // no symbol encoding in web mode
    }

    @Override
    public void preventSystemSleep(boolean preventSleep) {
        // no-op
    }

    @Override
    public float getScreenScale() {
        return 1.0f;
    }

    @Override
    public UpnpServiceConfiguration getUpnpPlatformService() {
        return null;
    }

    @Override
    public boolean hasNetGame() {
        return false;
    }
}

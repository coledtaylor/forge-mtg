package forge.web;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for WebGuiBase pseudo-EDT behavior.
 */
public class WebGuiBaseTest {

    private WebGuiBase guiBase;

    @BeforeClass
    public void setUp() {
        guiBase = new WebGuiBase("./test-assets");
    }

    @AfterClass
    public void tearDown() {
        guiBase.shutdown();
    }

    @Test
    public void testIsGuiThreadReturnsFalseOnTestThread() {
        Assert.assertFalse(guiBase.isGuiThread(),
                "isGuiThread() should return false on the test thread");
    }

    @Test
    public void testInvokeInEdtLaterExecutesOnWebEdtThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> threadName = new AtomicReference<>();

        guiBase.invokeInEdtLater(() -> {
            threadName.set(Thread.currentThread().getName());
            latch.countDown();
        });

        Assert.assertTrue(latch.await(5, TimeUnit.SECONDS),
                "Runnable should complete within 5 seconds");
        Assert.assertEquals(threadName.get(), "Web-EDT",
                "invokeInEdtLater should execute on the Web-EDT thread");
    }

    @Test
    public void testInvokeInEdtAndWaitExecutesOnWebEdtThread() {
        AtomicReference<String> threadName = new AtomicReference<>();

        guiBase.invokeInEdtAndWait(() ->
                threadName.set(Thread.currentThread().getName()));

        Assert.assertEquals(threadName.get(), "Web-EDT",
                "invokeInEdtAndWait should execute on the Web-EDT thread");
    }

    @Test
    public void testInvokeInEdtNowRunsOnCallingThread() {
        AtomicReference<String> threadName = new AtomicReference<>();
        String callingThread = Thread.currentThread().getName();

        guiBase.invokeInEdtNow(() ->
                threadName.set(Thread.currentThread().getName()));

        Assert.assertEquals(threadName.get(), callingThread,
                "invokeInEdtNow should execute on the calling thread");
    }

    @Test
    public void testIsRunningOnDesktopReturnsFalse() {
        Assert.assertFalse(guiBase.isRunningOnDesktop(),
                "isRunningOnDesktop() should return false for web context");
    }

    @Test
    public void testGetAssetsDirReturnsConstructorPath() {
        Assert.assertEquals(guiBase.getAssetsDir(), "./test-assets",
                "getAssetsDir() should return the path provided to the constructor");
    }
}

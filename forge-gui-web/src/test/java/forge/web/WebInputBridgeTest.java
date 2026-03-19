package forge.web;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class WebInputBridgeTest {

    private WebInputBridge bridge;

    @BeforeMethod
    public void setUp() {
        bridge = new WebInputBridge();
    }

    @Test
    public void testRegisterAndCompleteUnblocksWaitingThread() throws Exception {
        final String inputId = "test-input-1";
        final String response = "{\"choice\": 0}";

        final CompletableFuture<String> future = bridge.register(inputId);

        // Complete from another thread
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                bridge.complete(inputId, response);
            });

            // This should unblock once the other thread completes
            final String result = future.get(2, TimeUnit.SECONDS);
            Assert.assertEquals(result, response);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testCompleteWithUnknownInputIdReturnsFalse() {
        Assert.assertFalse(bridge.complete("nonexistent-id", "{}"));
    }

    @Test
    public void testCancelAllCompletesAllFuturesExceptionally() {
        final CompletableFuture<String> future1 = bridge.register("input-1");
        final CompletableFuture<String> future2 = bridge.register("input-2");

        bridge.cancelAll();

        Assert.assertTrue(future1.isCompletedExceptionally());
        Assert.assertTrue(future2.isCompletedExceptionally());
        Assert.assertEquals(bridge.pendingCount(), 0);

        // Verify the exception type
        try {
            future1.get();
            Assert.fail("Expected ExecutionException");
        } catch (ExecutionException e) {
            Assert.assertTrue(e.getCause() instanceof WebInputBridge.GameSessionExpiredException);
        } catch (InterruptedException e) {
            Assert.fail("Unexpected InterruptedException");
        }
    }

    @Test
    public void testPendingCountTracking() {
        Assert.assertEquals(bridge.pendingCount(), 0);

        bridge.register("input-1");
        Assert.assertEquals(bridge.pendingCount(), 1);

        bridge.register("input-2");
        Assert.assertEquals(bridge.pendingCount(), 2);

        bridge.complete("input-1", "{}");
        Assert.assertEquals(bridge.pendingCount(), 1);

        bridge.complete("input-2", "{}");
        Assert.assertEquals(bridge.pendingCount(), 0);
    }

    @Test
    public void testHasPending() {
        Assert.assertFalse(bridge.hasPending("input-1"));

        bridge.register("input-1");
        Assert.assertTrue(bridge.hasPending("input-1"));
        Assert.assertFalse(bridge.hasPending("input-2"));

        bridge.complete("input-1", "{}");
        Assert.assertFalse(bridge.hasPending("input-1"));
    }
}

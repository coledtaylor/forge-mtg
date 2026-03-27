package forge.web.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Tracks the lifecycle and results of a single simulation run.
 * Thread-safe: results are synchronized, listeners are copy-on-write.
 */
public final class SimulationJob {

    private final String id;
    private final String testDeckName;
    private final List<String> opponentDeckNames;
    private final int totalGames;
    private final java.util.Map<String, Double> cardPlaystyleScores;
    private final ManaProfile manaProfile;
    private final AtomicInteger completedGames = new AtomicInteger(0);
    private final List<SimulationResult> results = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean cancelled = false;
    private volatile boolean running = false;
    private volatile boolean complete = false;
    private final CopyOnWriteArrayList<Consumer<SimulationSummary>> progressListeners = new CopyOnWriteArrayList<>();

    public SimulationJob(final String id, final String testDeckName,
                         final List<String> opponentDeckNames, final int totalGames,
                         final java.util.Map<String, Double> cardPlaystyleScores,
                         final ManaProfile manaProfile) {
        this.id = id;
        this.testDeckName = testDeckName;
        this.opponentDeckNames = new ArrayList<>(opponentDeckNames);
        this.totalGames = totalGames;
        this.cardPlaystyleScores = cardPlaystyleScores != null
                ? new java.util.HashMap<>(cardPlaystyleScores)
                : new java.util.HashMap<>();
        this.manaProfile = manaProfile;
    }

    /**
     * Add a completed game result. Increments progress and notifies listeners.
     */
    public void addResult(final SimulationResult result) {
        results.add(result);
        completedGames.incrementAndGet();
        notifyListeners();
    }

    /**
     * Cancel the simulation. Remaining games will not be started.
     */
    public void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isComplete() {
        return complete;
    }

    public void setRunning(final boolean running) {
        this.running = running;
    }

    public void setComplete() {
        this.running = false;
        this.complete = true;
        notifyListeners();
    }

    /**
     * Get the current progress as a SimulationSummary snapshot.
     */
    public SimulationSummary getProgress() {
        final SimulationSummary summary;
        synchronized (results) {
            summary = SimulationSummary.computeFrom(new ArrayList<>(results), totalGames, cardPlaystyleScores, manaProfile);
        }
        summary.setCancelled(cancelled);
        return summary;
    }

    public void addProgressListener(final Consumer<SimulationSummary> listener) {
        progressListeners.add(listener);
    }

    public void removeProgressListener(final Consumer<SimulationSummary> listener) {
        progressListeners.remove(listener);
    }

    private void notifyListeners() {
        if (progressListeners.isEmpty()) {
            return;
        }
        final SimulationSummary snapshot = getProgress();
        for (final Consumer<SimulationSummary> listener : progressListeners) {
            try {
                listener.accept(snapshot);
            } catch (final Exception e) {
                // Don't let a failing listener break the simulation
            }
        }
    }

    // Getters
    public String getId() { return id; }
    public String getTestDeckName() { return testDeckName; }
    public List<String> getOpponentDeckNames() { return Collections.unmodifiableList(opponentDeckNames); }
    public int getTotalGames() { return totalGames; }
    public int getCompletedGames() { return completedGames.get(); }
    public java.util.Map<String, Double> getCardPlaystyleScores() { return java.util.Collections.unmodifiableMap(cardPlaystyleScores); }
    public ManaProfile getManaProfile() { return manaProfile; }
}

package forge.web;

import java.util.concurrent.ConcurrentHashMap;

import forge.trackable.TrackableObject;

/**
 * Maps TrackableObject IDs to their instances for lookup during DTO conversion
 * and WebSocket message handling.
 */
public class ViewRegistry {

    private final ConcurrentHashMap<Integer, TrackableObject> registry = new ConcurrentHashMap<>();

    /**
     * Register a TrackableObject by its ID.
     *
     * @param obj the trackable object to register
     */
    public void register(final TrackableObject obj) {
        registry.put(obj.getId(), obj);
    }

    /**
     * Resolve a TrackableObject by ID and cast to the expected type.
     *
     * @param id the TrackableObject ID
     * @param type the expected class
     * @param <T> the expected type
     * @return the resolved object
     * @throws IllegalArgumentException if the ID is not registered
     * @throws ClassCastException if the object is not of the expected type
     */
    public <T extends TrackableObject> T resolve(final int id, final Class<T> type) {
        final TrackableObject obj = registry.get(id);
        if (obj == null) {
            throw new IllegalArgumentException("No TrackableObject registered with ID: " + id);
        }
        return type.cast(obj);
    }

    /**
     * Remove a TrackableObject by ID.
     *
     * @param id the ID to remove
     */
    public void remove(final int id) {
        registry.remove(id);
    }

    /**
     * Clear all registered objects.
     */
    public void clear() {
        registry.clear();
    }

    /**
     * @return the number of registered objects
     */
    public int size() {
        return registry.size();
    }
}

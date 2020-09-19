package pl.airq.prediction.ga.cache;

import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

abstract class CacheWithFallback<K, V> implements Cache<K, V> {
    private final Map<K, V> cache = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Override
    public Uni<V> get(K key) {
        return Uni.createFrom().item(cache.getOrDefault(key, null))
                  .onItem().ifNull().switchTo(findLatestAndSave(key));
    }

    @Override
    public V getBlocking(K key) {
        return intoBlocking(get(key));
    }

    @Override
    public Uni<Void> upsert(K key, V value) {
        return Uni.createFrom().item(cache.put(key, value))
                  .onItem().castTo(Void.class);
    }

    @Override
    public void upsertBlocking(K key, V value) {
        intoBlocking(upsert(key, value));
    }

    @Override
    public Uni<Void> remove(K key) {
        return Uni.createFrom().item(cache.remove(key))
                  .onItem().castTo(Void.class);
    }

    @Override
    public void removeBlocking(K key) {
        intoBlocking(remove(key));
    }

    @Override
    public Uni<Void> clear() {
        return Uni.createFrom().voidItem().invoke(ignore -> cache.clear());
    }

    @Override
    public void clearBlocking() {
        intoBlocking(clear());
    }

    protected <T> T intoBlocking(Uni<T> uni) {
        return uni.emitOn(executor)
                  .await().atMost(Duration.ofSeconds(5));
    }

    protected Uni<V> findLatestAndSave(K key) {
        return findLatest(key)
                .onItem().ifNotNull().invoke(value -> cache.put(key, value));
    }

    abstract Uni<V> findLatest(K key);
}

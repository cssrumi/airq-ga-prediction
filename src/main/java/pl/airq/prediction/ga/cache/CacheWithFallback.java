package pl.airq.prediction.ga.cache;

import io.smallrye.mutiny.Uni;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.airq.common.process.MutinyUtils;

abstract class CacheWithFallback<K, V> implements Cache<K, V> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheWithFallback.class);
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
                  .onItem().transformToUni(MutinyUtils::ignoreUniResult);
    }

    @Override
    public void upsertBlocking(K key, V value) {
        intoBlocking(upsert(key, value));
    }

    @Override
    public Uni<Void> remove(K key) {
        return Uni.createFrom().item(cache.remove(key))
                  .onItem().transformToUni(MutinyUtils::ignoreUniResult);
    }

    @Override
    public void removeBlocking(K key) {
        intoBlocking(remove(key));
    }

    @Override
    public Uni<Void> clear() {
        return MutinyUtils.uniFromRunnable(cache::clear)
                          .invoke(ignore -> LOGGER.info("{} cleared.", cacheName()));
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

    abstract String cacheName();
}

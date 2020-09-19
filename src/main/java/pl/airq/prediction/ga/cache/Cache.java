package pl.airq.prediction.ga.cache;

import io.smallrye.mutiny.Uni;

public interface Cache<K, V> {

    Uni<V> get(K key);

    V getBlocking(K key);

    Uni<Void> upsert(K key, V value);

    void upsertBlocking(K key, V value);

    Uni<Void> remove(K key);

    void removeBlocking(K key);
}

package moe.nea.zwirn;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class Index<K, V> {
    Map<K, Set<V>> map = new HashMap<>();

    public void addToIndex(K k, V v) {
        find(k).add(v);
    }

    public Set<K> keys() {
        return map.keySet();
    }

    public <T> void loadFrom(Iterable<T> it, Function<T, K> keyExtractor, Function<T, V> valueExtractor) {
        for (T t : it) {
            addToIndex(keyExtractor.apply(t), valueExtractor.apply(t));
        }
    }

    public Set<V> find(K k) {
        return map.computeIfAbsent(k, ignored -> new HashSet<>());
    }

}
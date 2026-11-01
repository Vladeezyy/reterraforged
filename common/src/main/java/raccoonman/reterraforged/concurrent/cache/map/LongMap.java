package raccoonman.reterraforged.concurrent.cache.map;

import java.util.function.LongFunction;
import java.util.function.Predicate;
import java.util.function.Consumer;

public interface LongMap<T> {
    int size();
    
    void clear();
    
    void remove(long key);
    
    void remove(long key, Consumer<T> ifPreset);
    
    int removeIf(Predicate<T> predicate);
    
    void put(long key, T value);
    
    T get(long key);
    
    T computeIfAbsent(long key, LongFunction<T> computer);
    
}

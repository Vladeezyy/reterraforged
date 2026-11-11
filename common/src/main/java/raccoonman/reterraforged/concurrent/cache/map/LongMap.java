package raccoonman.reterraforged.concurrent.cache.map;

import java.util.function.LongFunction;
import java.util.function.Predicate;

public interface LongMap<T> {
    int size();
    
    void clear();
    
    void remove(long key);
    
    // whether it was there: only the value given leaves
    boolean remove(long key, T value);
    
    
    int removeIf(Predicate<T> predicate);
    
    T get(long key);
    
    T computeIfAbsent(long key, LongFunction<T> computer);
    
}

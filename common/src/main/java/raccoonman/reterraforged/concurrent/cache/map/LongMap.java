package raccoonman.reterraforged.concurrent.cache.map;

import java.util.function.LongFunction;
import java.util.function.Predicate;

public interface LongMap<T> {
    int size();
    
    void clear();
    
    void remove(long key);
    
    
    int removeIf(Predicate<T> predicate);
    
    T get(long key);
    
    T computeIfAbsent(long key, LongFunction<T> computer);
    
}

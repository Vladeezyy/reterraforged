package raccoonman.reterraforged.concurrent.cache.map;

import java.util.function.LongFunction;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.function.Predicate;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.concurrent.locks.StampedLock;

/**
 * A long-keyed map behind a StampedLock: bounded (the oldest entry leaves when it is full, the tile cache) or unbounded
 * (the river cache). It was two copies of the same class.
 */
public class StampedLongMap<T> implements LongMap<T> {
	private final int capacity;
	private final Predicate<T> evictable;
	private final StampedLock lock;
	private final Long2ObjectLinkedOpenHashMap<T> map;

	private StampedLongMap(int size, int capacity, Predicate<T> evictable) {
		this.capacity = capacity;
		this.evictable = evictable;
		this.lock = new StampedLock();
		this.map = new Long2ObjectLinkedOpenHashMap<>(size);
	}

	/** At most size entries: the oldest leaves for a new one */
	public static <T> StampedLongMap<T> bounded(int size) {
		return bounded(size, (t) -> true);
	}

	/**
	 * At most size entries: the oldest one that is evictable leaves for a new one, the oldest of all when none is. The tile
	 * cache keeps the tiles still being generated: thrown out, the next chunk of one started it again
	 */
	public static <T> StampedLongMap<T> bounded(int size, Predicate<T> evictable) {
		return new StampedLongMap<>(size, size, evictable);
	}

	/** Sized for size entries at first, grows as needed */
	public static <T> StampedLongMap<T> unbounded(int size) {
		return new StampedLongMap<>(size, Integer.MAX_VALUE, (t) -> true);
	}

	@Override
	public int size() {
		long stamp = this.lock.readLock();
		try {
			return this.map.size();
		} finally {
			this.lock.unlockRead(stamp);
		}
	}

	@Override
	public void clear() {
		long stamp = this.lock.writeLock();
		try {
			this.map.clear();
		} finally {
			this.lock.unlockWrite(stamp);
		}
	}

	@Override
	public void remove(long key) {
		long stamp = this.lock.writeLock();
		try {
			this.map.remove(key);
		} finally {
			this.lock.unlockWrite(stamp);
		}
	}

	// only this value: another thread may have put a new one under the key since this one was taken
	@Override
	public boolean remove(long key, T value) {
		long stamp = this.lock.writeLock();
		try {
			if (this.map.get(key) != value) {
				return false;
			}
			this.map.remove(key);
			return true;
		} finally {
			this.lock.unlockWrite(stamp);
		}
	}

	@Override
	public int removeIf(Predicate<T> predicate) {
		long stamp = this.lock.writeLock();
		try {
			int startSize = this.map.size();
			ObjectIterator<Long2ObjectMap.Entry<T>> iterator = this.map.long2ObjectEntrySet().fastIterator();
			while (iterator.hasNext()) {
				Long2ObjectMap.Entry<T> entry = iterator.next();
				if (predicate.test(entry.getValue())) {
					iterator.remove();
				}
			}
			return startSize - this.map.size();
		} finally {
			this.lock.unlockWrite(stamp);
		}
	}

	@Override
	public T get(long key) {
		long stamp = this.lock.readLock();
		try {
			return this.map.get(key);
		} finally {
			this.lock.unlockRead(stamp);
		}
	}

	@Override
	public T computeIfAbsent(long key, LongFunction<T> func) {
		long readStamp = this.lock.readLock();
		try {
			T t = this.map.get(key);
			if (t != null) {
				return t;
			}
		} finally {
			this.lock.unlockRead(readStamp);
		}
		long writeStamp = this.lock.writeLock();
		try {
			// another thread may have put it between the locks: then nothing is made, and nothing leaves for it
			T t = this.map.get(key);
			if (t != null) {
				return t;
			}
			if (this.map.size() >= this.capacity) {
				this.evict();
			}
			t = func.apply(key);
			if (t != null) {
				this.map.put(key, t);
			}
			return t;
		} finally {
			this.lock.unlockWrite(writeStamp);
		}
	}

	// under the write lock
	private void evict() {
		ObjectIterator<Long2ObjectMap.Entry<T>> iterator = this.map.long2ObjectEntrySet().fastIterator();
		while (iterator.hasNext()) {
			if (this.evictable.test(iterator.next().getValue())) {
				iterator.remove();
				return;
			}
		}
		this.map.removeFirst();
	}
}

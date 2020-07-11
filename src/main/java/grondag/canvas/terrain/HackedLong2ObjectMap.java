package grondag.canvas.terrain;

import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Predicate;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;

class HackedLong2ObjectMap<T> extends Long2ObjectOpenHashMap<T> {
	private final StampedLock lock = new StampedLock();
	private final Consumer<T> clearHandler;
	private final LongArrayList pruned = new LongArrayList();

	HackedLong2ObjectMap(int expectedSize, float fillFactor, Consumer<T> clearHandler)  {
		super(expectedSize, fillFactor);
		this.clearHandler = clearHandler;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void clear() {
		final long stamp = lock.writeLock();
		final  int limit = n;
		final Object[] values = value;

		for (int i = 0; i < limit; ++i) {
			final Object val  = values[i];

			if (val != null) {
				clearHandler.accept((T) val);
			}
		}

		super.clear();
		lock.unlock(stamp);
	}

	@Override
	public T computeIfAbsent(final long k, final java.util.function.LongFunction<? extends T> mappingFunction) {
		long stamp = lock.tryOptimisticRead();
		T result;

		if (stamp != 0) {
			result = super.get(k);

			if (result != null && lock.validate(stamp)) {
				return result;
			}
		}

		stamp = lock.writeLock();

		try {
			result = super.computeIfAbsent(k, mappingFunction);
		} finally {
			lock.unlockWrite(stamp);
		}

		return result;
	}

	@Override
	public T get(final long k) {
		long stamp = lock.tryOptimisticRead();
		T result;

		if (stamp != 0) {
			result = super.get(k);

			if (result != null && lock.validate(stamp)) {
				return result;
			}
		}

		stamp = lock.readLock();

		try {
			result = super.get(k);
		} finally {
			lock.unlockRead(stamp);
		}

		return result;
	}

	@Override
	public T remove(final long k) {
		T result;
		final long stamp = lock.writeLock();

		try {
			result = super.remove(k);
		} finally {
			lock.unlockWrite(stamp);
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public void prune(Predicate<T> pruner) {
		final LongArrayList pruned = this.pruned;
		final int limit = n;
		final Object[] values = value;
		final long[] keys = key;
		final long stamp = lock.writeLock();

		pruned.clear();

		try {
			for (int i  = 0; i < limit; ++i) {
				final Object val = values[i];

				if (val != null && pruner.test((T) val)) {
					pruned.add(keys[i]);
				}
			}

			final int pruneCount = pruned.size();

			if (pruneCount != 0) {
				for (int i  = 0; i < pruneCount; ++i) {
					super.remove(pruned.getLong(i));
				}
			}
		} finally {
			lock.unlockWrite(stamp);
		}

	}
}
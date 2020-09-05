package grondag.canvas;

import java.util.Random;
import java.util.function.IntUnaryOperator;

import it.unimi.dsi.fastutil.HashCommon;
import org.junit.jupiter.api.Test;

import net.minecraft.util.math.BlockPos;

import grondag.canvas.terrain.FastFloorMod;

class ChunkAddressPerf {
	static final int COUNT = 100000000;
	static int[] data = new int[COUNT * 3];

	static long doHash() {
		final long n = System.nanoTime();

		long sink = 0;

		for (int i = 0; i < COUNT; ) {
			sink += HashCommon.mix(BlockPos.asLong(data[i++], data[i++], data[i++]));
		}

		for (int i = 0; i < COUNT; ) {
			sink += HashCommon.mix(BlockPos.asLong(data[i++], data[i++], data[i++]));
		}

		for (int i = 0; i < COUNT; ) {
			sink += HashCommon.mix(BlockPos.asLong(data[i++], data[i++], data[i++]));
		}

		for (int i = 0; i < COUNT; ) {
			sink += HashCommon.mix(BlockPos.asLong(data[i++], data[i++], data[i++]));
		}

		for (int i = 0; i < COUNT; ) {
			sink += HashCommon.mix(BlockPos.asLong(data[i++], data[i++], data[i++]));
		}

		System.out.println("Hash time = " + ((System.nanoTime() - n) * 1000 / COUNT));

		return sink;
	}

	static long doMod() {

		final long n = System.nanoTime();

		long sink = 0;

		for (int i = 0; i < COUNT; ++i) {
			sink += getRegionIndex((data[i++] >> 4) % 65, data[i++] >> 4, (data[i++] >> 4) % 65);
		}

		for (int i = 0; i < COUNT; ++i) {
			sink += getRegionIndex((data[i++] >> 4) % 65, data[i++] >> 4, (data[i++] >> 4) % 65);
		}

		for (int i = 0; i < COUNT; ++i) {
			sink += getRegionIndex((data[i++] >> 4) % 65, data[i++] >> 4, (data[i++] >> 4) % 65);
		}

		for (int i = 0; i < COUNT; ++i) {
			sink += getRegionIndex((data[i++] >> 4) % 65, data[i++] >> 4, (data[i++] >> 4) % 65);
		}

		for (int i = 0; i < COUNT; ++i) {
			sink += getRegionIndex((data[i++] >> 4) % 65, data[i++] >> 4, (data[i++] >> 4) % 65);
		}

		System.out.println("Modulo time = " + ((System.nanoTime() - n) * 1000 / COUNT));

		return sink;
	}

	static long doFastMod() {
		final IntUnaryOperator mod = FastFloorMod.get(32);

		final long n = System.nanoTime();

		long sink = 0;

		for (int i = 0; i < COUNT; ++i) {
			sink += getRegionIndex(mod.applyAsInt(data[i++] >> 4), data[i++] >> 4, mod.applyAsInt(data[i++] >> 4));
		}

		for (int i = 0; i < COUNT; ++i) {
			sink += getRegionIndex(mod.applyAsInt(data[i++] >> 4), data[i++] >> 4, mod.applyAsInt(data[i++] >> 4));
		}

		for (int i = 0; i < COUNT; ++i) {
			sink += getRegionIndex(mod.applyAsInt(data[i++] >> 4), data[i++] >> 4, mod.applyAsInt(data[i++] >> 4));
		}

		for (int i = 0; i < COUNT; ++i) {
			sink += getRegionIndex(mod.applyAsInt(data[i++] >> 4), data[i++] >> 4, mod.applyAsInt(data[i++] >> 4));
		}

		for (int i = 0; i < COUNT; ++i) {
			sink += getRegionIndex(mod.applyAsInt(data[i++] >> 4), data[i++] >> 4, mod.applyAsInt(data[i++] >> 4));
		}

		System.out.println("Fast Modulo time = " + ((System.nanoTime() - n) * 1000 / COUNT));

		return sink;
	}

	private static int getRegionIndex(int x, int y, int z) {
		return (((z * 65) + x) << 4) + y;
	}

	@Test
	void test() {
		final Random r = new Random();

		for (int i = 0; i < COUNT; ) {
			data[i++] = r.nextInt();
			data[i++] = r.nextInt();
			data[i++] = r.nextInt();
		}

		doHash();
		doHash();
		doHash();

		doMod();
		doMod();
		doMod();

		doFastMod();
		doFastMod();
		doFastMod();

		doHash();
		doHash();
		doHash();

		doMod();
		doMod();
		doMod();

		doHash();
		doHash();
		doHash();

		doMod();
		doMod();
		doMod();

		doFastMod();
		doFastMod();
		doFastMod();
	}

}

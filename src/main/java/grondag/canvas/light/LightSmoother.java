package grondag.canvas.light;

import grondag.canvas.apiimpl.util.ChunkRendererRegionExt;
import grondag.fermion.world.PackedBlockPos;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ExtendedBlockView;

public class LightSmoother {
    public static final int OPAQUE = -1;
    private static final int BLUR_RADIUS = 2;
    private static final int MARGIN = BLUR_RADIUS + 2;
    private static final int POS_DIAMETER = 16 + MARGIN * 2;
    private static final int POS_COUNT = POS_DIAMETER * POS_DIAMETER * POS_DIAMETER;
    private static final int Y_INC = POS_DIAMETER;
    private static final int Z_INC = POS_DIAMETER * POS_DIAMETER;
//    private static final float BOOST = 1.02f;

    private static class Helper {
        private final BlockPos.Mutable smoothPos = new BlockPos.Mutable();
        private final float a[] = new float[POS_COUNT];
        private final float b[] = new float[POS_COUNT];
        private final float c[] = new float[POS_COUNT];
    }

    private static final ThreadLocal<Helper> helpers = ThreadLocal.withInitial(Helper::new);

    public static void computeSmoothedBrightness(BlockPos chunkOrigin, ExtendedBlockView blockViewIn, Long2IntOpenHashMap output) {
        final Helper help = helpers.get();
        final BlockPos.Mutable smoothPos = help.smoothPos;
        float[] sky = help.a;
        float[] block = help.b;

        final int minX = chunkOrigin.getX() - MARGIN;
        final int minY = chunkOrigin.getY() - MARGIN;
        final int minZ = chunkOrigin.getZ() - MARGIN;

        ExtendedBlockView view = (ExtendedBlockView) ((ChunkRendererRegionExt)blockViewIn).canvas_worldHack();

        for(int x = 0; x < POS_DIAMETER; x++) {
            for(int y = 0; y < POS_DIAMETER; y++) {
                for(int z = 0; z < POS_DIAMETER; z++) {
                    smoothPos.set(x + minX, y + minY, z + minZ);
                    
                    // PERF: make better use of cached blocked state in ChunkRenderer view
                    BlockState state = view.getBlockState(smoothPos);
                    final int packedLight = state.getBlockBrightness(view, smoothPos);

                    //PERF: still needed for Ao calc?
                    final boolean opaque = state.isFullOpaque(view, smoothPos);
//                    //                    subtractedCache.put(packedPos, (short) subtracted);

                    final int i = index(x, y , z);
                    if(opaque) {
                        block[i] = OPAQUE;
                        sky[i] = OPAQUE;
                    } else 
                    if(packedLight == 0) {
                        block[i] = 0;
                        sky[i] = 0;
                    } else {
                        // PERF try integer math?
                        // if pack both into same long could probably do addition concurrently
                        block[i] = (packedLight & 0xFF) * 0.0625f;
                        sky[i] = ((packedLight >>> 16) & 0xFF) * 0.0625f;
                    }
                }
            }
        }

        float[] work = help.c;
        smooth(BLUR_RADIUS + 1, block, work);
        smooth(BLUR_RADIUS, work, block);
        //        smooth(1, block, work);
        //        float[] swap = block;
        //        block = work;
        //        work = swap;

        smooth(BLUR_RADIUS + 1, sky, work);
        smooth(BLUR_RADIUS, work, sky);
        //        smooth(1, sky, work);
        //        swap = sky;
        //        sky = work;
        //        work = swap;

        final int limit = 16 + MARGIN + 1;
        for(int x = MARGIN - 2; x < limit; x++) {
            for(int y = MARGIN - 2; y < limit; y++) {
                for(int z = MARGIN - 2; z < limit; z++) {
                    final long packedPos = PackedBlockPos.pack(x + minX, y + minY, z + minZ);
                    final int i = index(x, y , z);
                    final int b = Math.round(Math.max(0, block[i]) * 16 * 1.04f);
                    final int k = Math.round(Math.max(0, sky[i]) * 16 * 1.04f);
                    output.put(packedPos, (Math.min(b, 240) & 0b11111100) | ((Math.min(k, 240) & 0b11111100)  << 16));
                }
            }
        }
    }

    private static int index(int x, int y, int z) {
        return x + y * Y_INC + z * Z_INC;
    }

    private static final float INNER_DIST = 0.44198f;
    private static final float OUTER_DIST = (1.0f - INNER_DIST) / 2f;
    private static final float INNER_PLUS = INNER_DIST + OUTER_DIST;
    private static void smooth(int margin, float[] src, float[] dest) {
        final int base = MARGIN - margin;
        final int limit = POS_DIAMETER - MARGIN + margin;

        // PERF iterate array directly and use pre-computed per-axis offsets

        // X PASS
        for(int x = base; x < limit; x++) {
            for(int y = base; y < limit; y++) {
                for(int z = base; z < limit; z++) {
                    int i = index(x, y, z);

                    float c = src[i];
                    if(c == OPAQUE) {
                        dest[i] = OPAQUE;
                        continue;
                    }

                    float a = src[index(x + 1, y, z)];
                    float b = src[index(x - 1, y, z)];

                    if(a == OPAQUE) {
                        if(b == OPAQUE) {
                            dest[i] = c;
                        } else {
                            dest[i] = b * OUTER_DIST + c * INNER_PLUS;
                        }
                    } else if(b == OPAQUE) {
                        dest[i] = a * OUTER_DIST + c * INNER_PLUS;
                    } else {
                        dest[i] = a * OUTER_DIST + b * OUTER_DIST + c * INNER_DIST;
                    }
                }
            }
        }

        // Y PASS
        for(int x = base; x < limit; x++) {
            for(int y = base; y < limit; y++) {
                for(int z = base; z < limit; z++) {
                    int i = index(x, y, z);

                    // Note arrays are swapped here
                    float c = dest[i];
                    if(c == OPAQUE) {
                        src[i] = OPAQUE;
                        continue;
                    }

                    float a = dest[index(x, y - 1, z)];
                    float b = dest[index(x, y + 1, z)];

                    if(a == OPAQUE) {
                        if(b == OPAQUE) {
                            src[i] = c;
                        } else {
                            src[i] = b * OUTER_DIST + c * INNER_PLUS;
                        }
                    } else if(b == OPAQUE) {
                        src[i] = a * OUTER_DIST + c * INNER_PLUS;
                    } else {
                        src[i] = a * OUTER_DIST + b * OUTER_DIST + c * INNER_DIST;
                    }
                }
            }
        }
        // Z PASS
        for(int x = base; x < limit; x++) {
            for(int y = base; y < limit; y++) {
                for(int z = base; z < limit; z++) {
                    int i = index(x, y, z);

                    // Arrays are swapped back to original roles here
                    float c = src[i];
                    if(c == OPAQUE) {
                        dest[i] = OPAQUE;
                        continue;
                    }

                    float a = src[index(x, y, z - 1)];
                    float b = src[index(x, y, z + 1)];

                    if(a == OPAQUE) {
                        if(b == OPAQUE) {
                            dest[i] = c;
                        } else {
                            dest[i] = b * OUTER_DIST + c * INNER_PLUS;
                        }
                    } else if(b == OPAQUE) {
                        dest[i] = a * OUTER_DIST + c * INNER_PLUS;
                    } else {
                        dest[i] = a * OUTER_DIST + b * OUTER_DIST + c * INNER_DIST;
                    }
                }
            }
        }
    }
}

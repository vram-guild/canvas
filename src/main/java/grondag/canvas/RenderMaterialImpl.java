/*
 * Copyright (c) 2016, 2017, 2018 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package grondag.canvas;

import grondag.canvas.core.PipelineManager;
import grondag.canvas.core.RenderPipeline;
import grondag.fermion.varia.BitPacker64;
import grondag.fermion.varia.BitPacker64.BooleanElement;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.client.model.fabric.MaterialFinder;
import net.fabricmc.fabric.api.client.model.fabric.RenderMaterial;
import net.minecraft.block.BlockRenderLayer;

public abstract class RenderMaterialImpl {
    private static final BitPacker64<RenderMaterialImpl> BITPACKER = new BitPacker64<RenderMaterialImpl>(RenderMaterialImpl::getBits, RenderMaterialImpl::setBits);
    public static final int MAX_SPRITE_DEPTH = 3;

    // Following are indexes into the array of boolean elements.
    // They are NOT the index of the bits themselves.  Used to
    // efficiently access flags based on sprite layer. "_START"
    // index is the flag for sprite layer 0, with additional layers
    // offset additively by sprite index.
    private static final int EMISSIVE_INDEX_START = 0;
    private static final int DIFFUSE_INDEX_START = EMISSIVE_INDEX_START + MAX_SPRITE_DEPTH;
    private static final int AO_INDEX_START = DIFFUSE_INDEX_START + MAX_SPRITE_DEPTH;
    private static final int CUTOUT_INDEX_START = AO_INDEX_START + MAX_SPRITE_DEPTH;
    private static final int MIPPED_INDEX_START = CUTOUT_INDEX_START + MAX_SPRITE_DEPTH;
    private static final int COLOR_DISABLE_INDEX_START = MIPPED_INDEX_START + MAX_SPRITE_DEPTH;
    
    @SuppressWarnings("unchecked")
    private static final BitPacker64<RenderMaterialImpl>.BooleanElement[] FLAGS = new BooleanElement[COLOR_DISABLE_INDEX_START + MAX_SPRITE_DEPTH];
    
    @SuppressWarnings("unchecked")
    private static final BitPacker64<RenderMaterialImpl>.NullableEnumElement<BlockRenderLayer> BLEND_MODES[] = new BitPacker64.NullableEnumElement[MAX_SPRITE_DEPTH];
    
    private static final BitPacker64<RenderMaterialImpl>.IntElement SPRITE_DEPTH;
    static {
        // First 16 bits of material bits are sent directly to the shader as control flags.
        // Bit order is optimized for shader usage. In particular, representation
        // of cutout and mipped handling is redundant of blend mode but is easier
        // to consume in the shader as simple on/off flags.
        FLAGS[EMISSIVE_INDEX_START + 0] = BITPACKER.createBooleanElement();
        FLAGS[EMISSIVE_INDEX_START + 1] = BITPACKER.createBooleanElement();
        FLAGS[EMISSIVE_INDEX_START + 2] = BITPACKER.createBooleanElement();
        
        // this one is padding, reserved for future use
        // needed to ensure other flags align to convenient boundaries
        FLAGS[EMISSIVE_INDEX_START + 3] = BITPACKER.createBooleanElement();
        
        FLAGS[DIFFUSE_INDEX_START + 0] = BITPACKER.createBooleanElement();
        FLAGS[AO_INDEX_START + 0] = BITPACKER.createBooleanElement();
        FLAGS[CUTOUT_INDEX_START + 0] = BITPACKER.createBooleanElement();
        FLAGS[MIPPED_INDEX_START + 0] = BITPACKER.createBooleanElement();
        
        FLAGS[DIFFUSE_INDEX_START + 1] = BITPACKER.createBooleanElement();
        FLAGS[AO_INDEX_START + 1] = BITPACKER.createBooleanElement();
        FLAGS[CUTOUT_INDEX_START + 1] = BITPACKER.createBooleanElement();
        FLAGS[MIPPED_INDEX_START + 1] = BITPACKER.createBooleanElement();
        
        FLAGS[DIFFUSE_INDEX_START + 2] = BITPACKER.createBooleanElement();
        FLAGS[AO_INDEX_START + 2] = BITPACKER.createBooleanElement();
        FLAGS[CUTOUT_INDEX_START + 2] = BITPACKER.createBooleanElement();
        FLAGS[MIPPED_INDEX_START + 2] = BITPACKER.createBooleanElement();
        
        // remaining elements are not part of shader control flags...
        
        FLAGS[COLOR_DISABLE_INDEX_START + 0] = BITPACKER.createBooleanElement();
        FLAGS[COLOR_DISABLE_INDEX_START + 1] = BITPACKER.createBooleanElement();
        FLAGS[COLOR_DISABLE_INDEX_START + 2] = BITPACKER.createBooleanElement();
        
        BLEND_MODES[0] = BITPACKER.createNullableEnumElement(BlockRenderLayer.class);
        BLEND_MODES[1] = BITPACKER.createNullableEnumElement(BlockRenderLayer.class);
        BLEND_MODES[2] = BITPACKER.createNullableEnumElement(BlockRenderLayer.class);
        
        SPRITE_DEPTH = BITPACKER.createIntElement(1, MAX_SPRITE_DEPTH);
    }

    static private final ObjectArrayList<Value> LIST = new ObjectArrayList<>();
    static private final Long2ObjectOpenHashMap<Value> MAP = new Long2ObjectOpenHashMap<>();
    
    public static RenderMaterialImpl.Value byIndex(int index) {
        return LIST.get(index);
    }

    protected long bits;

    private long getBits() {
        return bits;
    }
    
    private void setBits(long bits) {
        this.bits = bits;
    }
    
    public BlockRenderLayer blendMode(int spriteIndex) {
        return BLEND_MODES[spriteIndex].getValue(this);
    }

    public boolean disableColorIndex(int spriteIndex) {
        return FLAGS[COLOR_DISABLE_INDEX_START + spriteIndex].getValue(this);
    }

    public int spriteDepth() {
        return SPRITE_DEPTH.getValue(this);
    }

    public boolean emissive(int spriteIndex) {
        return FLAGS[EMISSIVE_INDEX_START + spriteIndex].getValue(this);
    }

    public boolean disableDiffuse(int spriteIndex) {
        return FLAGS[DIFFUSE_INDEX_START + spriteIndex].getValue(this);
    }

    public boolean disableAo(int spriteIndex) {
        return FLAGS[AO_INDEX_START + spriteIndex].getValue(this);
    }

    public static class Value extends RenderMaterialImpl implements RenderMaterial {
        private final int index;

        /**
         * True if any texture wants AO shading. Simplifies check made by renderer at
         * buffer-time.
         */
        public final boolean hasAo;

        /**
         * Sprite index ordinal flags indicating texture wants emissive lighting. 
         */
        public final int emissiveFlags;

        /** 
         * Determine which buffer we use - derived from base layer.
         * If base layer is solid, then any additional sprites are handled
         * as decals and render in solid pass.  If base layer is trasnlucent
         * then all sprite layers render as translucent.
         */
        public final int renderLayerIndex;
        
        public final RenderPipeline pipeline;
        
        private Value(int index, long bits) {
            this.index = index;
            this.bits = bits;
            hasAo = !disableAo(0) || (spriteDepth() > 1 && !disableAo(1)) || (spriteDepth() == 3 && !disableAo(2));
            emissiveFlags = (emissive(0) ? 1 : 0) | (emissive(1) ? 2 : 0) | (emissive(2) ? 4 : 0);
            this.renderLayerIndex = this.blendMode(0) == BlockRenderLayer.TRANSLUCENT ? BlockRenderLayer.TRANSLUCENT.ordinal() : BlockRenderLayer.SOLID.ordinal();
            this.pipeline = PipelineManager.INSTANCE.getDefaultPipeline(this.spriteDepth());
        }

        public int index() {
            return index;
        }
    }

    public static class Finder extends RenderMaterialImpl implements MaterialFinder {
        @Override
        public synchronized Value find() {
            Value result = MAP.get(bits);
            if (result == null) {
                result = new Value(LIST.size(), bits);
                LIST.add(result);
                MAP.put(bits, result);
            }
            return result;
        }

        @Override
        public Finder clear() {
            bits = 0;
            return this;
        }
        
        @Override
        public Finder blendMode(int spriteIndex, BlockRenderLayer blendMode) {
            BLEND_MODES[spriteIndex].setValue(blendMode, this);
            return this;
        }

        @Override
        public Finder disableColorIndex(int spriteIndex, boolean disable) {
            FLAGS[COLOR_DISABLE_INDEX_START + spriteIndex].setValue(disable, this);
            return this;
        }

        @Override
        public Finder spriteDepth(int depth) {
            if (depth < 1 || depth > MAX_SPRITE_DEPTH) {
                throw new IndexOutOfBoundsException("Invalid sprite depth: " + depth);
            }
            SPRITE_DEPTH.setValue(depth, this);
            return this;
        }

        @Override
        public Finder emissive(int spriteIndex, boolean isEmissive) {
            FLAGS[EMISSIVE_INDEX_START + spriteIndex].setValue(isEmissive, this);
            return this;
        }

        @Override
        public Finder disableDiffuse(int spriteIndex, boolean disable) {
            FLAGS[DIFFUSE_INDEX_START + spriteIndex].setValue(disable, this);
            return this;
        }

        @Override
        public Finder disableAo(int spriteIndex, boolean disable) {
            FLAGS[AO_INDEX_START + spriteIndex].setValue(disable, this);
            return this;
        }
    }
}

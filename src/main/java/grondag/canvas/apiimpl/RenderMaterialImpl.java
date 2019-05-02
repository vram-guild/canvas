/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package grondag.canvas.apiimpl;

import grondag.canvas.material.ShaderManager;
import grondag.fermion.varia.BitPacker64;
import grondag.fermion.varia.BitPacker64.BooleanElement;
import grondag.frex.api.material.RenderMaterial;
import grondag.frex.api.material.MaterialFinder;
import grondag.frex.api.material.MaterialShader;
import grondag.frex.api.material.MaterialCondition;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.BlockRenderLayer;

public abstract class RenderMaterialImpl {
    private static final BitPacker64<RenderMaterialImpl> BITPACKER = new BitPacker64<RenderMaterialImpl>(RenderMaterialImpl::getBits, RenderMaterialImpl::setBits);
    public static final int MAX_SPRITE_DEPTH = 3;
    private static final BlockRenderLayer[] LAYERS = BlockRenderLayer.values();
    
    // Following are indexes into the array of boolean elements.
    // They are NOT the index of the bits themselves.  Used to
    // efficiently access flags based on sprite layer. "_START"
    // index is the flag for sprite layer 0, with additional layers
    // offset additively by sprite index.
    private static final int EMISSIVE_INDEX_START = 0;
    private static final int DIFFUSE_INDEX_START = EMISSIVE_INDEX_START + MAX_SPRITE_DEPTH;
    private static final int AO_INDEX_START = DIFFUSE_INDEX_START + MAX_SPRITE_DEPTH;
    private static final int CUTOUT_INDEX_START = AO_INDEX_START + MAX_SPRITE_DEPTH;
    private static final int UNMIPPED_INDEX_START = CUTOUT_INDEX_START + MAX_SPRITE_DEPTH;
    private static final int COLOR_DISABLE_INDEX_START = UNMIPPED_INDEX_START + MAX_SPRITE_DEPTH;
    
    @SuppressWarnings("unchecked")
    private static final BitPacker64<RenderMaterialImpl>.BooleanElement[] FLAGS = new BooleanElement[COLOR_DISABLE_INDEX_START + MAX_SPRITE_DEPTH];
    
    @SuppressWarnings("unchecked")
    private static final BitPacker64<RenderMaterialImpl>.NullableEnumElement<BlockRenderLayer> BLEND_MODES[] = new BitPacker64.NullableEnumElement[MAX_SPRITE_DEPTH];
    
    private static final BitPacker64<RenderMaterialImpl>.IntElement SPRITE_DEPTH;
    
    private static final BitPacker64<RenderMaterialImpl>.IntElement SHADER;
    
    private static final BitPacker64<RenderMaterialImpl>.IntElement CONDITION;
    
    private static final long DEFAULT_BITS;
    
    static private final ObjectArrayList<Value> LIST = new ObjectArrayList<>();
    static private final Long2ObjectOpenHashMap<Value> MAP = new Long2ObjectOpenHashMap<>();

    
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
        FLAGS[UNMIPPED_INDEX_START + 0] = BITPACKER.createBooleanElement();
        
        FLAGS[DIFFUSE_INDEX_START + 1] = BITPACKER.createBooleanElement();
        FLAGS[AO_INDEX_START + 1] = BITPACKER.createBooleanElement();
        FLAGS[CUTOUT_INDEX_START + 1] = BITPACKER.createBooleanElement();
        FLAGS[UNMIPPED_INDEX_START + 1] = BITPACKER.createBooleanElement();
        
        FLAGS[DIFFUSE_INDEX_START + 2] = BITPACKER.createBooleanElement();
        FLAGS[AO_INDEX_START + 2] = BITPACKER.createBooleanElement();
        FLAGS[CUTOUT_INDEX_START + 2] = BITPACKER.createBooleanElement();
        FLAGS[UNMIPPED_INDEX_START + 2] = BITPACKER.createBooleanElement();
        
        // remaining elements are not part of shader control flags...
        
        FLAGS[COLOR_DISABLE_INDEX_START + 0] = BITPACKER.createBooleanElement();
        FLAGS[COLOR_DISABLE_INDEX_START + 1] = BITPACKER.createBooleanElement();
        FLAGS[COLOR_DISABLE_INDEX_START + 2] = BITPACKER.createBooleanElement();
        
        BLEND_MODES[0] = BITPACKER.createNullableEnumElement(BlockRenderLayer.class);
        BLEND_MODES[1] = BITPACKER.createNullableEnumElement(BlockRenderLayer.class);
        BLEND_MODES[2] = BITPACKER.createNullableEnumElement(BlockRenderLayer.class);
        
        SPRITE_DEPTH = BITPACKER.createIntElement(1, MAX_SPRITE_DEPTH);
        SHADER = BITPACKER.createIntElement(ShaderManager.MAX_SHADERS);
        CONDITION = BITPACKER.createIntElement(MaterialConditionImpl.MAX_CONDITIONS);
        
        long defaultBits = 0;
        defaultBits = BLEND_MODES[0].setValue(null, defaultBits);
        defaultBits = BLEND_MODES[1].setValue(null, defaultBits);
        defaultBits = BLEND_MODES[2].setValue(null, defaultBits);
        DEFAULT_BITS = defaultBits;
    }
    
    public static RenderMaterialImpl.Value byIndex(int index) {
        return LIST.get(index);
    }

    protected long bits = DEFAULT_BITS;

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
    
    public int shaderFlags() {
        return (int) (bits & 0xFFFF);
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
         * as decals and render in solid pass.  If base layer is translucent
         * then all sprite layers render as translucent.
         */
        public final BlockRenderLayer renderLayer;
        
        public final MaterialConditionImpl condition;
        
        public MaterialShaderImpl shader;
        
        private final Value[] blockLayerVariants = new Value[4];
        
        protected Value(int index, long bits, MaterialShaderImpl shader) {
            this.index = index;
            this.bits = bits;
            this.shader = shader;
            this.condition = MaterialConditionImpl.fromIndex(CONDITION.getValue(bits));
            setupBlockLayerVariants();
            hasAo = !disableAo(0) || (spriteDepth() > 1 && !disableAo(1)) || (spriteDepth() == 3 && !disableAo(2));
            emissiveFlags = (emissive(0) ? 1 : 0) | (emissive(1) ? 2 : 0) | (emissive(2) ? 4 : 0);
            this.renderLayer = this.blendMode(0) == BlockRenderLayer.TRANSLUCENT ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.SOLID;
        }

        /**
         * Only called if this has no null blend modes - will be meaningless otherwise.
         * Materials will null blend modes shouldn't be used - get correct materials
         * via {@link #forRenderLayer(int)}.
         */
        private void setupShaderFlags() {
            switch(blendMode(0)) {
            case CUTOUT:
                FLAGS[UNMIPPED_INDEX_START + 0].setValue(true, this);
            case MIPPED_CUTOUT:
                FLAGS[CUTOUT_INDEX_START + 0].setValue(true, this);
            default:
                break;
            }
            
            final int depth = this.spriteDepth();
            if(depth > 1) {
                switch(blendMode(1)) {
                case CUTOUT:
                    FLAGS[UNMIPPED_INDEX_START + 1].setValue(true, this);
                case MIPPED_CUTOUT:
                    FLAGS[CUTOUT_INDEX_START + 1].setValue(true, this);
                default:
                    break;
                }
                
                if(depth == 3) {
                switch(blendMode(2)) {
                    case CUTOUT:
                        FLAGS[UNMIPPED_INDEX_START + 2].setValue(true, this);
                    case MIPPED_CUTOUT:
                        FLAGS[CUTOUT_INDEX_START + 2].setValue(true, this);
                    default:
                        break;
                    }
                }
            }
        }
        
        private static final ThreadLocal<Finder> variantFinder = ThreadLocal.withInitial(Finder::new); 
        
        private void setupBlockLayerVariants() {
            boolean needsVariant = blendMode(0) == null;
            final int depth = spriteDepth();
            if(!needsVariant && depth > 1) {
                needsVariant = blendMode(1) == null;
                if(!needsVariant && depth == 3) {
                    needsVariant = blendMode(2) == null;
                }
            }
            
            if(needsVariant) {
                final Finder finder = variantFinder.get();
                for(int i = 0; i < 4; i++) {
                    BlockRenderLayer layer = LAYERS[i];
                    finder.bits = this.bits;
                    finder.shader = this.shader;
                    if(finder.blendMode(0) == null) {
                        finder.blendMode(0, layer);
                    }
                    if(depth > 1) {
                        if(finder.blendMode(1) == null) {
                            finder.blendMode(1, layer);
                        }
                        if(depth == 3 && finder.blendMode(2) == null) {
                            finder.blendMode(2, layer);
                        }
                    }
                    blockLayerVariants[i] = finder.find();
                }
            } else {
                // we are a renderable material, so set up control flags needed by shader
                setupShaderFlags();
                for(int i = 0; i < 4; i++) {
                    blockLayerVariants[i] = this;
                }
            }
        }
        
        /**
         * If this material has one or more null blend modes, this returns
         * a material with any such blend modes set to the given input. Typically
         * this is only used for vanilla default materials that derive their
         * blend mode from the block render layer, but it is also possible to
         * specify materials with null blend modes to achieve the same behavior.<p>
         * 
         * If a non-null blend mode is specified for every sprite layer, this
         * will always return the current instance.<p>
         * 
         * We need shader flags to accurately reflect the effective blend mode
         * and we need that to be fast, and we also want the buffering logic to
         * remain simple.  This solves all those problems.<p>
         */
        public Value forRenderLayer(int layerIndex) {
            return blockLayerVariants[layerIndex];
        }
        
        public int index() {
            return index;
        }
    }

    public static class Finder extends RenderMaterialImpl implements MaterialFinder {
        private MaterialShaderImpl shader = null;
        
        @Override
        public synchronized Value find() {
            MaterialShaderImpl s = shader == null ? ShaderManager.INSTANCE.getDefault() : shader;
            SHADER.setValue(s.getIndex(), this);
            Value result = MAP.get(bits);
            if (result == null) {
                result = new Value(LIST.size(), bits, s);
                LIST.add(result);
                MAP.put(result.bits, result);
            }
            return result;
        }

        @Override
        public Finder clear() {
            bits = 0;
            shader = null;
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

        @Override
        public Finder shader(MaterialShader shader) {
            this.shader = (MaterialShaderImpl) shader;
            return this;
        }

        @Override
        public MaterialFinder condition(MaterialCondition condition) {
            CONDITION.setValue(((MaterialConditionImpl)condition).index, this);
            return this;
        }
    }
}

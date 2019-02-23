package grondag.acuity.core;

import grondag.acuity.api.IBlockInfo;
import grondag.acuity.api.IPipelinedQuad;
import grondag.acuity.api.IRenderPipeline;
import grondag.acuity.api.RenderPipeline;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.pipeline.LightUtil;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class VanillaVertexLighter extends CompoundVertexLighter
{
    private class ChildLighter extends PipelinedVertexLighter
    {
        private float combinedShade = 1f;
        
        protected ChildLighter(IRenderPipeline pipeline)
        {
            super(pipeline);
        }
        
        @Override
        protected void resetForNewQuad(IPipelinedQuad quad)
        {
            super.resetForNewQuad(quad);
        }

        @Override
        public final IBlockInfo getBlockInfo()
        {
            return blockInfo;
        }
        
        @Override
        public final VertexCollector getVertexCollector()
        {
            return target.getVertexCollector(this.pipeline);
        }
        
        @Override
        protected void reportOutput()
        {
            didOutput = true;
        }
        
        @Override
        public void setBlockLightMap(int blockLightRGBF)
        {
            // convert to straight 0-255 luminance value for vanilla light map
            super.setBlockLightMap(Math.round(
                     (blockLightRGBF & 0xFF) * 0.2126f
                   + ((blockLightRGBF >> 8) & 0xFF) * 0.7152f
                   + ((blockLightRGBF >> 16) & 0xFF) * 0.0722f));
        }

        /**
         * Encode flags for emissive rendering, cutout and mipped handling.
         * This allows MC CUTOUT and CUTOUT_MIPPED quads to be backed into a single buffer
         * and rendered in the same draw command.  If cutout is on, then any fragment in
         * the base layer with a (base) texture alpha value less than 0.5 will be discarded.<p>
         * 
         * Layered quads don't generally use cutout textures, but if a model does supply
         * a base texture with holes and the quad is set to use a cutout layer, then the
         * discard will also affect overlay textures.  In other words, if the base texture has 
         * a hole,  the hole will not be covered by an overlay texture, even if the overlay is 
         * fully opaque.  (This could change in the future.)
         * 
         */
        private int encodeFlags()
        {
            int result = this.glowFlags; // bits 0-2
                    
            // send cutout and mipped indicators
            if(!this.isCurrentQuadMipped)
                result |= 0b00001000;
            
            if(this.isCurrentQuadCutout)
                result |= 0b00010000;
            
            return result;
        }
        
        @Override
        protected VertexCollector startVertex(
                float posX,
                float posY,
                float posZ,
                float normX,
                float normY,
                float normZ,
                int unlitColorARGB0,
                float u0,
                float v0)
        {
                
            final IBlockInfo blockInfo = getBlockInfo();
            
            // local position is vertex, + block-state-driven shift (if any);
            posX += blockInfo.shiftX();
            posY += blockInfo.shiftY();
            posZ += blockInfo.shiftZ();
            
            final VertexCollector output = getVertexCollector();
            final BlockPos pos = blockInfo.blockPos();
    
            // Compute light
            int blockLight = 0, skyLight = 0;
            float shade = 1.0f;
            
            // avoid computation if no shading
            if(this.areAllLayersEmissive())
            {
                blockLight = 255;
                skyLight = 255;
            }
            else
            {
                final float lightX = posX - .5f + normX * .5f;
                final float lightY = posY - .5f + normY * .5f;
                final float lightZ = posZ - .5f + normZ * .5f;
                
                if(this.enableDiffuse)
                    shade = LightUtil.diffuseLight(normX, normY, normZ);
                
                if(this.enableAmbientOcclusion)
                {
                    shade *= getAo(blockInfo, lightX, lightY, lightZ);
                    blockLight = Math.round(calcLightmap(blockInfo.getBlockLight(), lightX, lightY, lightZ) * LIGHTMAP_TO_255);
                    skyLight = Math.round(calcLightmap(blockInfo.getSkyLight(), lightX, lightY, lightZ) * LIGHTMAP_TO_255);
                }
                else
                {
                    // what we get back is raw (0-15) sky << 20 | block << 4
                    // we want to output 0-255
                    final int packedLight =  this.calcPackedLight(blockInfo, normX, normY, normZ, lightX, lightY, lightZ);
                    blockLight = ((packedLight >> 4) & 0xF) * 17;
                    skyLight = ((packedLight >> 20) & 0xF) * 17;
                }
            
                blockLight = Math.max(blockLight, this.blockLightMap);
                skyLight = Math.max(skyLight, this.skyLightMap);
            }
            
            // save for other layers
            this.combinedShade = shade;
            
            // POSITION_3F
            output.pos(pos, posX, posY, posZ);
            
            // BASE_RGBA_4UB
            output.add((this.glowFlags & 1) == 1 ? AcuityColorHelper.swapRedBlue(unlitColorARGB0) : AcuityColorHelper.shadeColorAndSwapRedBlue(unlitColorARGB0, shade));
            
            // BASE_TEX_2F
            output.add(u0);
            output.add(v0);
            
            // NORMAL_3UB
//            int normAo = Math.round(normX * 127 + 127);
//            normAo |= (Math.round(normY * 127 + 127) << 8);
//            normAo |= (Math.round(normZ * 127 + 127) << 16);
//            // AO 1UB
//            normAo |= (ao << 24);
//            output.add(normAo);
            
            //LIGHTMAP
            output.add(blockLight | (skyLight << 8) | (encodeFlags() << 16));
            return output;
        }

        @Override
        protected void addSecondaryLayer(VertexCollector target, int unlitColorARGB1, float u1, float v1)
        {
            target.add((this.glowFlags & 2) == 2 ? AcuityColorHelper.swapRedBlue(unlitColorARGB1) : AcuityColorHelper.shadeColorAndSwapRedBlue(unlitColorARGB1, combinedShade));
            target.add(u1);
            target.add(v1);
        }

        @Override
        protected void addTertiaryLayer(VertexCollector target, int unlitColorARGB2, float u2, float v2)
        {
            target.add((this.glowFlags & 4) == 4 ? AcuityColorHelper.swapRedBlue(unlitColorARGB2) : AcuityColorHelper.shadeColorAndSwapRedBlue(unlitColorARGB2, combinedShade));
            target.add(u2);
            target.add(v2);
        }
    }

    
    
    @Override
    protected PipelinedVertexLighter createChildLighter(RenderPipeline pipeline)
    {
        return new ChildLighter(pipeline);
    }
}

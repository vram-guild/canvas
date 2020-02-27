/*******************************************************************************
 * Copyright 2019, 2020 grondag
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


package grondag.canvas.apiimpl.rendercontext;

import java.util.Random;
import java.util.function.Supplier;

import net.minecraft.block.BlockState;

import grondag.canvas.buffer.encoding.VertexEncodingContext;

/**
 * Consumer for vanilla baked models. Generally intended to give visual results matching a vanilla render,
 * however there could be subtle (and desirable) lighting variations so is good to be able to render
 * everything consistently.
 *
 * <p>Also, the API allows multi-part models that hold multiple vanilla models to render them without
 * combining quad lists, but the vanilla logic only handles one model per block. To route all of
 * them through vanilla logic would require additional hooks.
 *
 *  <p>Works by copying the quad data to an "editor" quad held in the instance,
 *  where all transformations are applied before buffering. Transformations should be
 *  the same as they would be in a vanilla render - the editor is serving mainly
 *  as a way to access vertex data without magical numbers. It also allows a consistent interface
 *  for downstream tesselation routines.
 *
 *  <p>Another difference from vanilla render is that all transformation happens before the
 *  vertex data is sent to the byte buffer.  Generally POJO array access will be faster than
 *  manipulating the data via NIO.
 */
public class BlockFallbackConsumer extends AbstractFallbackConsumer {
	private final BlockRenderInfo blockInfo;

	public BlockFallbackConsumer(VertexEncodingContext encodingContext, BlockRenderInfo blockInfo) {
		super(encodingContext);
		this.blockInfo = blockInfo;
	}

	@Override
	protected Supplier<Random> randomSupplier() {
		return blockInfo.randomSupplier;
	}

	@Override
	protected boolean defaultAo() {
		return blockInfo.defaultAo;
	}

	@Override
	protected BlockState blockState() {
		return blockInfo.blockState;
	}
}

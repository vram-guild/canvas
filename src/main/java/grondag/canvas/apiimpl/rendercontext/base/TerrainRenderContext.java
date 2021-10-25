/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
 */

package grondag.canvas.apiimpl.rendercontext.base;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;

import io.vram.frex.api.math.MatrixStack;

import grondag.canvas.apiimpl.rendercontext.encoder.QuadEncoder;

/**
 * Implementation of RenderContext used during terrain rendering.
 * Dispatches calls from models during chunk rebuild to the appropriate consumer,
 * and holds/manages all of the state needed by them.
 */
public abstract class TerrainRenderContext<T extends BlockAndTintGetter, E extends QuadEncoder> extends AbstractBlockRenderContext<T, E> {
	// Reused each build to prevent needless allocation
	public final ObjectOpenHashSet<BlockEntity> nonCullBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> addedBlockEntities = new ObjectOpenHashSet<>();
	public final ObjectOpenHashSet<BlockEntity> removedBlockEntities = new ObjectOpenHashSet<>();
	// WIP: remove?
	public final MatrixStack matrixStack = MatrixStack.cast(new PoseStack());

	public TerrainRenderContext(E encoder) {
		super("TerrainRenderContext", encoder);
	}
}

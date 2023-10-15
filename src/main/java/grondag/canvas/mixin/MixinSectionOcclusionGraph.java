/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.world.level.ChunkPos;

import grondag.canvas.CanvasMod;

@Mixin(SectionOcclusionGraph.class)
public class MixinSectionOcclusionGraph {
	@Unique
	private static boolean shouldWarnOnChunkLoaded = true;

	@Inject(at = @At("HEAD"), method = "onChunkLoaded", cancellable = true)
	private void onOnChunkLoaded(ChunkPos chunkPos, CallbackInfo ci) {
		if (shouldWarnOnChunkLoaded) {
			CanvasMod.LOG.warn("[Canvas] SectionOcclusionGraph.onChunkLoaded() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnChunkLoaded = false;
		}
	}

	@Unique
	private static boolean shouldWarnGetRelativeFrom = true;

	@Inject(at = @At("HEAD"), method = "getRelativeFrom", cancellable = true)
	private void onGetRelativeFrom(CallbackInfoReturnable<SectionRenderDispatcher.RenderSection> ci) {
		if (shouldWarnGetRelativeFrom) {
			CanvasMod.LOG.warn("[Canvas] SectionOcclusionGraph.getRelativeFrom() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.setReturnValue(null);
			shouldWarnGetRelativeFrom = false;
		}
	}

	@Unique
	private static boolean shouldWarnOnUpdateRenderChunks = true;

	@Inject(at = @At("HEAD"), method = "runUpdates", cancellable = true)
	private void onUpdateRenderChunks(CallbackInfo ci) {
		if (shouldWarnOnUpdateRenderChunks) {
			CanvasMod.LOG.warn("[Canvas] SectionOcclusionGraph.runUpdates() called unexpectedly. This probably indicates a mod incompatibility.");
			ci.cancel();
			shouldWarnOnUpdateRenderChunks = false;
		}
	}
}

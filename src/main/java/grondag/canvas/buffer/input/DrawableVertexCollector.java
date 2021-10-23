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

package grondag.canvas.buffer.input;

import java.nio.IntBuffer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.phys.Vec3;

import grondag.canvas.buffer.render.TransferBuffer;
import grondag.canvas.buffer.util.DrawableStream;
import grondag.canvas.material.state.RenderState;
import grondag.canvas.render.terrain.TerrainSectorMap.RegionRenderSector;

public interface DrawableVertexCollector extends VertexCollector {
	RenderState renderState();

	void toBuffer(IntBuffer intBuffer, int targetIndex);

	void toBuffer(TransferBuffer targetBuffer, int bufferTargetIndex);

	void sortIfNeeded();

	boolean sorted();

	void draw(boolean clear);

	/**
	 * Single-buffer draw, minimizes state changes.
	 * Assumes all collectors are non-empty.
	 */
	static void draw(ObjectArrayList<? extends DrawableVertexCollector> drawList) {
		final DrawableStream buffer = new DrawableStream(drawList);
		buffer.draw(false);
		buffer.close();
	}

	boolean sortTerrainQuads(Vec3 sortPos, RegionRenderSector sector);

	@Nullable
	int[] saveState(@Nullable int[] translucentState);

	void loadState(int[] state);

	VertexBucket[] vertexBuckets();
}

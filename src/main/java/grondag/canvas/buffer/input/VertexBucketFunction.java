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

package grondag.canvas.buffer.input;

import io.vram.frex.api.material.RenderMaterial;
import io.vram.frex.base.renderer.mesh.BaseQuadView;

/**
 * Classifies quads into one or more buckets for which vertex data
 * should be segregated.  Used in terrain rendering to handle dynamic
 * face culling as view changes, and to eliminate non-shadow quads in
 * shadow pass when shadow maps are enabled.
 *
 * <p>In non-terrain rendering it's intended use is for shadow-pass segregation.
 *
 * <p>Should NOT be used to filter quads that will never render because by the
 * time this is called a quad has been fully transformed and lit - filtering
 * should happen earlier for sake of performance.
 */
@FunctionalInterface
public interface VertexBucketFunction {
	int computeBucket(BaseQuadView quad, RenderMaterial mat);

	VertexBucketFunction TERRAIN = (q, m) -> q.effectiveCullFaceId();
	int TERRAIN_BUCKET_COUNT = 7;
}

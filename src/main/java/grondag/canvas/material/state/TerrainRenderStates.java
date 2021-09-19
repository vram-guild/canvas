/*
 *  Copyright 2019, 2020 grondag
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License.  You may obtain a copy
 *  of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */

package grondag.canvas.material.state;

import java.util.function.Predicate;
import net.minecraft.client.renderer.RenderType;
import grondag.canvas.material.property.MaterialTarget;

/**
 * Here to defer initialization when referencing classes are loaded.
 */
public final class TerrainRenderStates {
	private TerrainRenderStates() { }

	public static final RenderState TRANSLUCENT = RenderLayerHelper.TRANSLUCENT_TERRAIN.renderState;
	public static final RenderState SOLID = RenderLayerHelper.copyFromLayer(RenderType.solid()).renderState;

	public static final Predicate<RenderState> TRANSLUCENT_PREDICATE = m -> m.target == MaterialTarget.TRANSLUCENT && m.primaryTargetTransparency;
	public static final Predicate<RenderState> SOLID_PREDICATE = m -> !TRANSLUCENT_PREDICATE.test(m);
}

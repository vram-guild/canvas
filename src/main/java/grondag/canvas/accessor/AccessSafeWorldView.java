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

package grondag.canvas.accessor;

import grondag.canvas.render.TerrainRenderContext;

/**
 * Used to stash block renderer reference in local scope during chunk rebuild,
 * thus avoiding repeated thread-local lookups.
 */
public interface AccessSafeWorldView {
    TerrainRenderContext fabric_getRenderer();

    void fabric_setRenderer(TerrainRenderContext renderer);
}

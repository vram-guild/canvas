/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.material.state;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;

// WIP: use this for entity material maps
public class RenderContextState {
	/**
	 * Set via world rendered when incoming vertices are for an entity.
	 * Meant to enable entity material maps
	 */
	private @Nullable Entity currentEntity;

	public @Nullable Entity getCurrentEntity() {
		return currentEntity;
	}

	public void setCurrentEntity(@Nullable Entity currentEntity) {
		this.currentEntity = currentEntity;
	}
}

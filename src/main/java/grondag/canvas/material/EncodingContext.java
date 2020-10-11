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

package grondag.canvas.material;

/**
 * Describes the type of render context
 * and controls how materials are packed to buffers.
 * <p>
 * WIP2: remove after conversion
 */
public enum EncodingContext {
	TERRAIN(true, false, false),
	BLOCK(true, false, false),
	ITEM(false, true, false),
	ENTITY_BLOCK(true, false, false),
	ENTITY_ITEM(false, true, false),
	ENTITY_ENTITY(false, false, false),
	PROCESS(false, false, false);

	public final boolean isBlock;
	public final boolean isItem;
	public final boolean isGui;
	public final boolean isWorld;

	EncodingContext(boolean isBlock, boolean isItem, boolean isGui) {
		this.isBlock = isBlock;
		this.isItem = isItem;
		this.isGui = isGui;
		isWorld = !isGui;
	}
}

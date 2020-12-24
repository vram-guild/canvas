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

package grondag.canvas.pipeline.config;

import net.minecraft.util.Identifier;

public class ImageConfig {
	public Identifier id;
	public boolean depth;
	public int internalFormat;
	public int minFilter;
	public int maxFilter;
	public int lod;

	public static ImageConfig of(Identifier id, boolean depth, int internalFormat, int minFilter, int maxFilter, int lod) {
		final ImageConfig result = new ImageConfig();
		result.id = id;
		result.depth = depth;
		result.internalFormat = internalFormat;
		result.lod = lod;
		result.minFilter = minFilter;
		result.maxFilter = maxFilter;
		return result;
	}

	public static ImageConfig[] array(ImageConfig... configs) {
		return configs;
	}
}

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

package grondag.canvas.pipeline;

import java.util.ArrayList;

import net.minecraft.util.Identifier;

class PipelineConfig {
	static class ImageConfig {
		Identifier id;
		boolean hdr;
		boolean blur;
		int lod;

		static ImageConfig of(Identifier id, boolean hdr, boolean blur, int lod) {
			final ImageConfig result = new ImageConfig();
			result.id = id;
			result.hdr = hdr;
			result.lod = lod;
			result.blur = blur;
			return result;
		}
	}

	ImageConfig[] images;

	{
		final ArrayList<ImageConfig> imgList = new ArrayList<>();
		imgList.add(ImageConfig.of(IMG_EMISSIVE, false, true, 0));
		imgList.add(ImageConfig.of(IMG_EMISSIVE_COLOR, false, true, 0));
		// don't want filtering when copy back from main
		imgList.add(ImageConfig.of(IMG_MAIN_COPY, false, false, 0));
		imgList.add(ImageConfig.of(IMG_BLOOM_DOWNSAMPLE, false, true, 6));
		imgList.add(ImageConfig.of(IMG_BLOOM_UPSAMPLE, false, true, 6));

		images = imgList.toArray(new ImageConfig[imgList.size()]);
	}

	static final Identifier IMG_EMISSIVE = new Identifier("canvas:emissive");
	static final Identifier IMG_EMISSIVE_COLOR = new Identifier("canvas:emissive_color");
	static final Identifier IMG_MAIN_COPY = new Identifier("canvas:main_copy");
	static final Identifier IMG_BLOOM_DOWNSAMPLE = new Identifier("canvas:bloom_downsample");
	static final Identifier IMG_BLOOM_UPSAMPLE = new Identifier("canvas:bloom_upsample");
}

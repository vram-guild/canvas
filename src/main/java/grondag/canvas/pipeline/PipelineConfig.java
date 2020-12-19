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

	static class DebugConfig {
		Identifier mainImage;
		Identifier sneakImage;
		int lod;
		String label;

		static DebugConfig of(Identifier mainImage, Identifier sneakImage, int lod, String label) {
			final DebugConfig result = new DebugConfig();
			result.mainImage = mainImage;
			result.sneakImage = sneakImage;
			result.lod = lod;
			result.label = label;
			return result;
		}
	}

	DebugConfig[] debugs;

	{
		final ArrayList<ImageConfig> imgList = new ArrayList<>();
		imgList.add(ImageConfig.of(IMG_EMISSIVE, false, true, 0));
		imgList.add(ImageConfig.of(IMG_EMISSIVE_COLOR, false, true, 0));
		// don't want filtering when copy back from main
		imgList.add(ImageConfig.of(IMG_MAIN_COPY, false, false, 0));
		imgList.add(ImageConfig.of(IMG_BLOOM_DOWNSAMPLE, false, true, 6));
		imgList.add(ImageConfig.of(IMG_BLOOM_UPSAMPLE, false, true, 6));
		images = imgList.toArray(new ImageConfig[imgList.size()]);

		final ArrayList<DebugConfig> debugList = new ArrayList<>();
		debugList.add(DebugConfig.of(PipelineConfig.IMG_EMISSIVE, PipelineConfig.IMG_EMISSIVE_COLOR, 0, "EMISSIVE/EMISSIVE_COLOR"));
		debugList.add(DebugConfig.of(PipelineConfig.IMG_BLOOM_UPSAMPLE, PipelineConfig.IMG_BLOOM_DOWNSAMPLE, 0, "BLOOM LOD 0 UPSAMPLE/DOWNSAMPLE"));
		debugList.add(DebugConfig.of(PipelineConfig.IMG_BLOOM_UPSAMPLE, PipelineConfig.IMG_BLOOM_DOWNSAMPLE, 1, "BLOOM LOD 1 UPSAMPLE/DOWNSAMPLE"));
		debugList.add(DebugConfig.of(PipelineConfig.IMG_BLOOM_UPSAMPLE, PipelineConfig.IMG_BLOOM_DOWNSAMPLE, 2, "BLOOM LOD 2 UPSAMPLE/DOWNSAMPLE"));
		debugList.add(DebugConfig.of(PipelineConfig.IMG_BLOOM_UPSAMPLE, PipelineConfig.IMG_BLOOM_DOWNSAMPLE, 3, "BLOOM LOD 3 UPSAMPLE/DOWNSAMPLE"));
		debugList.add(DebugConfig.of(PipelineConfig.IMG_BLOOM_UPSAMPLE, PipelineConfig.IMG_BLOOM_DOWNSAMPLE, 4, "BLOOM LOD 4 UPSAMPLE/DOWNSAMPLE"));
		debugList.add(DebugConfig.of(PipelineConfig.IMG_BLOOM_UPSAMPLE, PipelineConfig.IMG_BLOOM_DOWNSAMPLE, 5, "BLOOM LOD 5 UPSAMPLE/DOWNSAMPLE"));
		debugList.add(DebugConfig.of(PipelineConfig.IMG_BLOOM_UPSAMPLE, PipelineConfig.IMG_BLOOM_DOWNSAMPLE, 6, "BLOOM LOD 6 UPSAMPLE/DOWNSAMPLE"));
		debugs = debugList.toArray(new DebugConfig[debugList.size()]);
	}

	static final Identifier IMG_EMISSIVE = new Identifier("canvas:emissive");
	static final Identifier IMG_EMISSIVE_COLOR = new Identifier("canvas:emissive_color");
	static final Identifier IMG_MAIN_COPY = new Identifier("canvas:main_copy");
	static final Identifier IMG_BLOOM_DOWNSAMPLE = new Identifier("canvas:bloom_downsample");
	static final Identifier IMG_BLOOM_UPSAMPLE = new Identifier("canvas:bloom_upsample");
}

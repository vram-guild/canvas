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

package grondag.canvas.vf.stream;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import grondag.canvas.CanvasMod;
import grondag.canvas.vf.BufferWriter;

@Environment(EnvType.CLIENT)
public class VfStreamTexture {
	private final VfStreamSpec spec;
	protected VfStreamImage image = null;

	boolean logging = false;

	public VfStreamTexture(int textureUnit, int imageFormat, int imageCapacityBytes) {
		spec = new VfStreamSpec(textureUnit, imageFormat, imageCapacityBytes);
	}

	public void clear() {
		if (image != null) {
			image.clear();
			image = null;
		}
	}

	public VfStreamReference allocate(int byteCount, BufferWriter writer) {
		createImageIfNeeded();
		return image.allocate(byteCount, writer);
	}

	protected void createImageIfNeeded() {
		if (image == null) {
			try {
				image = new VfStreamImage(spec);
				image.logging = logging;
			} catch (final Exception e) {
				CanvasMod.LOG.warn("Unable to create vf texture due to error:", e);
				image = null;
			}
		}
	}
}

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

public class AttachmentConfig {
	public Identifier image;
	public int lod;
	public int clearColor;

	static AttachmentConfig of(
		Identifier image,
		int clearColor,
		int lod
	) {
		final AttachmentConfig result = new AttachmentConfig();
		result.image = image;
		result.lod = lod;
		result.clearColor = clearColor;
		return result;
	}

	public static AttachmentConfig of(
		String image,
		int clearColor,
		int lod
	) {
		return of(new Identifier(image), lod, clearColor);
	}

	public static AttachmentConfig[] array(AttachmentConfig... attachments) {
		return attachments;
	}
}

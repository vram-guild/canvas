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

public class FramebufferConfig {
	public Identifier id;
	public AttachmentConfig[] attachments;

	static FramebufferConfig of(
		Identifier id,
		AttachmentConfig... attachments
	) {
		final FramebufferConfig result = new FramebufferConfig();
		result.id = id;
		result.attachments = attachments;
		return result;
	}

	public static FramebufferConfig of(
		String id,
		AttachmentConfig... attachments
	) {
		return of(new Identifier(id), attachments);
	}

	public static FramebufferConfig[] array(FramebufferConfig... configs) {
		return configs;
	}
}

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

package grondag.canvas.pipeline.pass;

import grondag.canvas.pipeline.config.PassConfig;

// FEAT: complete or remove
public class BlitPass extends Pass {
	BlitPass(PassConfig config) {
		super(config);
	}

	@Override
	public void run(int width, int height) {
		//		if (fbo != null) {
		//			fbo.bind(width, height);
		//			fbo.clear();
		//
		//			 if (GlStateManager.supportsGl30()) {
		//	         GlStateManager.bindFramebuffer(36008, fbo.fboGlId);
		//	         GlStateManager.bindFramebuffer(36009, fbo);
		//	         GlStateManager.blitFramebuffer(0, 0, framebuffer.textureWidth, framebuffer.textureHeight, 0, 0, textureWidth, textureHeight, 256, 9728);
		//	      } else {
		//	         GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, fbo);
		//	         final int i = GlStateManager.getFramebufferDepthAttachment();
		//	         if (i != 0) {
		//	            final int j = GlStateManager.getActiveBoundTexture();
		//	            GlStateManager.bindTexture(i);
		//	            GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, framebuffer.fbo);
		//	            GlStateManager.copyTexSubImage2d(3553, 0, 0, 0, 0, 0, Math.min(textureWidth, framebuffer.textureWidth), Math.min(textureHeight, framebuffer.textureHeight));
		//	            GlStateManager.bindTexture(j);
		//	         }
		//	      }
		//
		//	      GlStateManager.bindFramebuffer(FramebufferInfo.FRAME_BUFFER, 0);
		//
		//		}
	}

	@Override
	public void close() {
		// NOOP
	}
}

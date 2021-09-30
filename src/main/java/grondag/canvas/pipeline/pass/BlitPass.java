/*
 * Copyright © Contributing Authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional copyright and licensing notices may apply for content that was
 * included from other projects. For more information, see ATTRIBUTION.md.
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

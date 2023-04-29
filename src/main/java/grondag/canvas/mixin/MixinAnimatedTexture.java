/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.renderer.texture.SpriteContents;

import grondag.canvas.mixinterface.AnimatedTextureExt;

@Mixin(SpriteContents.AnimatedTexture.class)
public class MixinAnimatedTexture implements AnimatedTextureExt {
	@Shadow(aliases = {"this$0", "a", "field_28469"})
	@Dynamic private SpriteContents parent;
	@Shadow @Final List<SpriteContents.FrameInfo> frames;
	@Shadow @Final private int frameRowSize;

	@Override
	public int canvas_frameCount() {
		return frameRowSize;
	}

	@Override
	public List<SpriteContents.FrameInfo> canvas_frames() {
		return frames;
	}
}

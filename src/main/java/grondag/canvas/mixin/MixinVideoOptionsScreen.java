/*******************************************************************************
 * Copyright 2019 grondag
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
 ******************************************************************************/

package grondag.canvas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import grondag.canvas.varia.CanvasButtonWidget;

@Mixin(VideoOptionsScreen.class)
public class MixinVideoOptionsScreen extends Screen {
	public MixinVideoOptionsScreen(Text title) {
		super(title);
	}

	@Inject(at = @At("RETURN"), method = "init()V")
	public void drawMenuButton(CallbackInfo info) {
		addButton(new CanvasButtonWidget(width - 100, 5, 90, 20, new TranslatableText("config.canvas.button"), this));
	}
}

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

package grondag.canvas.config;

import java.util.function.Consumer;
import java.util.function.Function;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.resource.language.I18n;

import grondag.canvas.CanvasMod;

public class FlawlessFramesController implements Consumer<Function<String, Consumer<Boolean>>> {
	private static ObjectArrayList<Consumer<Boolean>> activators = new ObjectArrayList<>();
	static boolean active = false;

	@Override
	public void accept(Function<String, Consumer<Boolean>> t) {
		activators.add(t.apply("renderbender"));
	}

	public static void handleToggle() {
		boolean newActive = active;

		while (CanvasMod.FLAWLESS_TOGGLE.wasPressed()) {
			newActive = !newActive;
			CanvasMod.LOG.info(I18n.translate("info.canvas.flawless_toggle", newActive));
		}

		if (active != newActive) {
			active = newActive;
			activators.forEach(a -> a.accept(active));
		}
	}
}

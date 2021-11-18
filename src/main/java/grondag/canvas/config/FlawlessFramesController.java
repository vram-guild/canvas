/*
 * Copyright © Original Authors
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

package grondag.canvas.config;

import java.util.function.Consumer;
import java.util.function.Function;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.client.resources.language.I18n;

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

		while (CanvasMod.FLAWLESS_TOGGLE.consumeClick()) {
			newActive = !newActive;
			CanvasMod.LOG.info(I18n.get("info.canvas.flawless_toggle", newActive));
		}

		if (active != newActive) {
			active = newActive;
			activators.forEach(a -> a.accept(active));
		}
	}
}

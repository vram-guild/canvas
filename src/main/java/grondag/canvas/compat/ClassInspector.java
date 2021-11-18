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

package grondag.canvas.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.renderer.LevelRenderer;

import grondag.canvas.CanvasMod;

class ClassInspector {
	static void inspect() {
		final Class<LevelRenderer> clazz = LevelRenderer.class;

		CanvasMod.LOG.info("");
		CanvasMod.LOG.info("WorldRenderer Class Summary - For Developer Use");
		CanvasMod.LOG.info("=============================================");
		CanvasMod.LOG.info("");
		CanvasMod.LOG.info("FIELDS");

		for (final Field f : clazz.getDeclaredFields()) {
			CanvasMod.LOG.info(f.toGenericString());
		}

		CanvasMod.LOG.info("");
		CanvasMod.LOG.info("METHODS");

		for (final Method m : clazz.getDeclaredMethods()) {
			CanvasMod.LOG.info(m.toGenericString());
		}

		CanvasMod.LOG.info("");
	}
}

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

package grondag.canvas.shader.data;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;

public final class FloatData {
	private FloatData() { }

	public static final int FLOAT_VECTOR_COUNT = 32;
	static final int FLOAT_VECTOR_LENGTH = FLOAT_VECTOR_COUNT * 4;
	public static final FloatBuffer FLOAT_VECTOR_DATA = BufferUtils.createFloatBuffer(FLOAT_VECTOR_LENGTH);

	static final int VEC_WORLD_TIME = 4 * 0;
	static final int RENDER_SECONDS = VEC_WORLD_TIME;
	static final int WORLD_TIME = VEC_WORLD_TIME + 1;
	static final int WORLD_DAYS = VEC_WORLD_TIME + 2;
	static final int MOON_SIZE = VEC_WORLD_TIME + 3;

	static final int VEC_AMBIENT_LIGHT = 4 * 1;
	static final int EMISSIVE_COLOR_RED = VEC_AMBIENT_LIGHT;
	static final int EMISSIVE_COLOR_GREEN = VEC_AMBIENT_LIGHT + 1;
	static final int EMISSIVE_COLOR_BLUE = VEC_AMBIENT_LIGHT + 2;
	static final int AMBIENT_INTENSITY = VEC_AMBIENT_LIGHT + 3;

	// carries view distance in spare slot
	static final int VEC_VANILLA_CLEAR_COLOR = 4 * 2;
	static final int VIEW_DISTANCE = VEC_VANILLA_CLEAR_COLOR + 3;

	static final int VEC_HELD_LIGHT = 4 * 3;
	static final int HELD_LIGHT_RED = VEC_HELD_LIGHT;
	static final int HELD_LIGHT_GREEN = VEC_HELD_LIGHT + 1;
	static final int HELD_LIGHT_BLUE = VEC_HELD_LIGHT + 2;
	static final int HELD_LIGHT_INTENSITY = VEC_HELD_LIGHT + 3;

	// camera position in world space
	// carries player mood in spare slot
	static final int VEC_CAMERA_POS = 4 * 4;
	static final int PLAYER_MOOD = VEC_CAMERA_POS + 3;

	// carries effect strength in spare slot
	static final int VEC_LAST_CAMERA_POS = 4 * 5;
	static final int NIGHT_VISION_STRENGTH = VEC_LAST_CAMERA_POS + 3;

	// camera view vector in world space
	// carries darkness scale in spare slot
	static final int VEC_CAMERA_VIEW = 4 * 6;
	static final int DARKNESS_FACTOR = VEC_CAMERA_VIEW + 3;

	// entity view vector in world space
	// carries sky flash strength in spare slot
	static final int VEC_ENTITY_VIEW = 4 * 7;
	static final int SKY_FLASH_STRENGTH = VEC_ENTITY_VIEW + 3;

	static final int VEC_VIEW_PARAMS = 4 * 8;
	static final int VIEW_WIDTH = VEC_VIEW_PARAMS;
	static final int VIEW_HEIGHT = VEC_VIEW_PARAMS + 1;
	static final int VIEW_ASPECT = VEC_VIEW_PARAMS + 2;
	static final int VIEW_BRIGHTNESS = VEC_VIEW_PARAMS + 3;

	static final int EYE_BRIGHTNESS = 4 * 9;
	static final int EYE_LIGHT_BLOCK = EYE_BRIGHTNESS;
	static final int EYE_LIGHT_SKY = EYE_BRIGHTNESS + 1;
	static final int SMOOTHED_EYE_LIGHT_BLOCK = EYE_BRIGHTNESS + 2;
	static final int SMOOTHED_EYE_LIGHT_SKY = EYE_BRIGHTNESS + 3;

	// last slot is EMPTY spare slot for now
	static final int EYE_POSITION = 4 * 10;

	static final int SKYLIGHT_VECTOR = 4 * 11;
	static final int SKY_ANGLE_RADIANS = SKYLIGHT_VECTOR + 3;

	static final int FOG_COLOR = 4 * 12;

	static final int ATMOSPHERIC_COLOR = 4 * 13;
	static final int SKYLIGHT_TRANSITION_FACTOR = ATMOSPHERIC_COLOR + 3;

	static final int SKYLIGHT_COLOR = 4 * 14;
	static final int SKYLIGHT_ILLUMINANCE = SKYLIGHT_COLOR + 3;

	// 15-18 reserved for cascades 0-3
	static final int SHADOW_CENTER = 4 * 15;

	static final int VEC_RENDER_INFO = 4 * 19;
	static final int FOG_START = VEC_RENDER_INFO;
	static final int FOG_END = VEC_RENDER_INFO + 1;
	static final int HELD_LIGHT_INNER_ANGLE = VEC_RENDER_INFO + 2;
	static final int HELD_LIGHT_OUTER_ANGLE = VEC_RENDER_INFO + 3;

	static final int VEC_WEATHER = 4 * 20;
	static final int RAIN_STRENGTH = VEC_WEATHER;
	static final int THUNDER_STRENGTH = VEC_WEATHER + 1;
	static final int SMOOTHED_RAIN_STRENGTH = VEC_WEATHER + 2;
	static final int SMOOTHED_THUNDER_STRENGTH = VEC_WEATHER + 3;

	static final int VEC_LIGHT_VOLUME_ORIGIN = 4 * 21;
	static final int LIGHT_DATA_ORIGIN = VEC_LIGHT_VOLUME_ORIGIN;
}

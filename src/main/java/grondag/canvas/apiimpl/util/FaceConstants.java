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

package grondag.canvas.apiimpl.util;

import net.minecraft.core.Direction;

import io.vram.frex.api.model.ModelHelper;

public class FaceConstants {
	public static final int NORTH_INDEX = ModelHelper.toFaceIndex(Direction.NORTH);
	public static final int SOUTH_INDEX = ModelHelper.toFaceIndex(Direction.SOUTH);
	public static final int EAST_INDEX = ModelHelper.toFaceIndex(Direction.EAST);
	public static final int WEST_INDEX = ModelHelper.toFaceIndex(Direction.WEST);
	public static final int UP_INDEX = ModelHelper.toFaceIndex(Direction.UP);
	public static final int DOWN_INDEX = ModelHelper.toFaceIndex(Direction.DOWN);
	public static final int UNASSIGNED_INDEX = ModelHelper.NULL_FACE_ID;

	public static final int NORTH_FLAG = 1 << NORTH_INDEX;
	public static final int SOUTH_FLAG = 1 << SOUTH_INDEX;
	public static final int EAST_FLAG = 1 << EAST_INDEX;
	public static final int WEST_FLAG = 1 << WEST_INDEX;
	public static final int UP_FLAG = 1 << UP_INDEX;
	public static final int DOWN_FLAG = 1 << DOWN_INDEX;
	public static final int UNASSIGNED_FLAG = 1 << UNASSIGNED_INDEX;

	public static final int ALL_REAL_FACE_FLAGS = NORTH_FLAG | SOUTH_FLAG | EAST_FLAG | WEST_FLAG | UP_FLAG | DOWN_FLAG;
}

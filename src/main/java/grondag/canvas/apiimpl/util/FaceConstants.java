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

package grondag.canvas.apiimpl.util;

import io.vram.frex.api.model.ModelHelper;

import net.minecraft.util.math.Direction;

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

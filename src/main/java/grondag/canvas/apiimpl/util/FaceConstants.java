/*
 * Copyright 2019, 2020 grondag
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
 */

package grondag.canvas.apiimpl.util;

import net.minecraft.util.math.Direction;

import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;

public class FaceConstants {

	public final static int NORTH_INDEX = ModelHelper.toFaceIndex(Direction.NORTH);
	public final static int SOUTH_INDEX = ModelHelper.toFaceIndex(Direction.SOUTH);
	public final static int EAST_INDEX = ModelHelper.toFaceIndex(Direction.EAST);
	public final static int WEST_INDEX = ModelHelper.toFaceIndex(Direction.WEST);
	public final static int UP_INDEX = ModelHelper.toFaceIndex(Direction.UP);
	public final static int DOWN_INDEX = ModelHelper.toFaceIndex(Direction.DOWN);

}

/*******************************************************************************
 * Copyright 2019, 2020 grondag
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
package grondag.canvas.mixinterface;

import net.minecraft.client.render.chunk.ChunkBuilder.BuiltChunk;
import net.minecraft.util.math.Direction;

public interface ChunkInfoExt {

	/**
	 * Direction from which chunk visibility search entered last time.
	 * Null if started from within chunk or from top or bottom of world.
	 */
	Direction canvas_entryFace();

	BuiltChunk canvas_chunk();

	/**
	 * True if the direction is the opposite of a back-facing direction.
	 */
	boolean canvas_isBacktrack(Direction opposite);

	/**
	 * Bits indicate chunk faces that are opposite the back-facing faces for this chunk.
	 * Bit positions are indexed by direction ordinals.
	 * Backfacing faces include the face from which the chunk was visited, plus
	 * the backfaces from the visiting chunk.
	 */
	byte canvas_backtrackState();

	/**
	 * Adds backtrack faces in bit flags and
	 * also marks the direction in as back-facing.
	 * All directions are the opposite of what they should be.
	 */
	void canvas_updateBacktrackState(byte cullState, Direction opposite);

	int canvas_propagationLevel();
}

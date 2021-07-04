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

package grondag.canvas.terrain.region.input;

public class SignalInputRegion extends PackedInputRegion {
	/**
	 * Signals that build was completed successfully, or has never been run. Nothing is scheduled.
	 */
	public static final PackedInputRegion IDLE = new SignalInputRegion();

	/**
	 * Signals that build is for resort only.
	 */
	public static final PackedInputRegion RESORT_ONLY = new SignalInputRegion();

	/**
	 * Signals that build has been cancelled or some other condition has made it unbuildable.
	 */
	public static final PackedInputRegion INVALID = new SignalInputRegion();

	/**
	 * Signals that build is for empty chunk.
	 */
	public static final PackedInputRegion EMPTY = new SignalInputRegion();

	@Override
	public void release() { }
}

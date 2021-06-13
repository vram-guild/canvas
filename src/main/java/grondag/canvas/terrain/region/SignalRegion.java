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

package grondag.canvas.terrain.region;

public class SignalRegion extends ProtoRenderRegion {
	/**
	 * Signals that build was completed successfully, or has never been run. Nothing is scheduled.
	 */
	public static final ProtoRenderRegion IDLE = new SignalRegion();

	/**
	 * Signals that build is for resort only.
	 */
	public static final ProtoRenderRegion RESORT_ONLY = new SignalRegion();

	/**
	 * Signals that build has been cancelled or some other condition has made it unbuildable.
	 */
	public static final ProtoRenderRegion INVALID = new SignalRegion();

	/**
	 * Signals that build is for empty chunk.
	 */
	public static final ProtoRenderRegion EMPTY = new SignalRegion();

	@Override
	public void release() { }
}

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

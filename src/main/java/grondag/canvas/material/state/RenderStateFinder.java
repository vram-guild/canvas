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

package grondag.canvas.material.state;

public class RenderStateFinder extends AbstractStateFinder<RenderStateFinder, RenderState> {
	@Override
	public synchronized RenderState findInner() {
		RenderState result = RenderState.MAP.get(bits);

		if (result == null) {
			result = new RenderState(bits);
			RenderState.MAP.put(bits, result);
			RenderState.STATES[result.index] = result;
		}

		return result;
	}

	private static ThreadLocal<RenderStateFinder> FINDER = ThreadLocal.withInitial(RenderStateFinder::new);

	public static RenderStateFinder threadLocal() {
		final RenderStateFinder result = FINDER.get();
		result.clear();
		return result;
	}
}

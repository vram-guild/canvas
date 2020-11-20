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

	@Override
	protected RenderState missing() {
		return RenderState.MISSING;
	}

	private static ThreadLocal<RenderStateFinder> FINDER = ThreadLocal.withInitial(RenderStateFinder::new);

	public static RenderStateFinder threadLocal() {
		final RenderStateFinder result = FINDER.get();
		result.clear();
		return result;
	}
}

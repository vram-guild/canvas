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

package grondag.canvas.render.region;

import java.util.function.Predicate;

import grondag.canvas.material.property.MaterialTarget;
import grondag.canvas.material.state.RenderState;

public interface DrawableRegion extends AutoCloseable {
	DrawableDelegate delegate();

	@Override
	void close();

	boolean isClosed();

	void bindIfNeeded();

	DrawableRegion EMPTY_DRAWABLE = new DrawableRegion() {
		@Override
		public DrawableDelegate delegate() {
			return null;
		}

		@Override
		public void close() {
			// NOOP
		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		public void bindIfNeeded() {
			// NOOP
		}
	};

	Predicate<RenderState> TRANSLUCENT = m -> m.target == MaterialTarget.TRANSLUCENT && m.primaryTargetTransparency;
	Predicate<RenderState> SOLID = m -> !TRANSLUCENT.test(m);
}

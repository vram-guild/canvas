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

package grondag.canvas.material.property;

public class BinaryMaterialState {
	private final Runnable enableAction;
	private final Runnable disableAction;

	public BinaryMaterialState(Runnable enableAction, Runnable disableAction) {
		this.enableAction = enableAction;
		this.disableAction = disableAction;
	}

	public void setEnabled(boolean enabled) {
		if (isActive && enabled == activeState) {
			return;
		}

		if (enabled) {
			enableAction.run();
		} else {
			disableAction.run();
		}

		isActive = true;
		activeState = enabled;
	}

	public void disable() {
		if (isActive) {
			if (activeState) {
				disableAction.run();
			}

			isActive = false;
		}
	}

	private boolean activeState;
	private boolean isActive;
}

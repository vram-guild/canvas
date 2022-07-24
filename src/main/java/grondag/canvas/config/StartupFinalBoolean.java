/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
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
 */

package grondag.canvas.config;

import java.util.function.Supplier;

/**
 * Config values that shouldn't change until the next MC restart.
 */
public class StartupFinalBoolean implements Supplier<Boolean> {
	private boolean startedUp = false;
	private boolean effective;

	/**
	 * Package-private variable represents current stored value.
	 */
	boolean current;

	public StartupFinalBoolean(boolean currentValue) {
		effective = current = currentValue;
	}

	/**
	 * Get effective value NOT current value.
	 *
	 * @return effective value.
	 */
	@Override
	public Boolean get() {
		assert startedUp : "Accessed startup persistent boolean before initialization.";
		return effective;
	}

	@SuppressWarnings("ConstantConditions")
	public void set(boolean currentValue, boolean isStartup) {
		assert (!isStartup || !startedUp) : "Initializing a startup persistent boolean more than once.";
		effective = (isStartup && !startedUp) ? currentValue : effective;
		startedUp |= isStartup;
		current = currentValue;
	}
}

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

package grondag.canvas.pipeline.config.util;

import java.util.function.Predicate;

import com.google.common.base.Predicates;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class NamedDependencyMap<T extends NamedConfig<T>> extends Object2ObjectOpenHashMap<String, T> {
	public final Predicate<String> builtInTest;

	public NamedDependencyMap(Predicate<String> builtInTest) {
		this.builtInTest = builtInTest;
	}

	public NamedDependencyMap() {
		this(Predicates.alwaysFalse());
	}

	public NamedDependency<T> createDependency(String name) {
		return new NamedDependency<>(this, name);
	}

	public boolean isValidReference(String name) {
		if (name == null || name.isEmpty()) {
			return false;
		}

		if (builtInTest.test(name)) {
			return true;
		}

		final T val = get(name);
		return val != null && val.validate();
	}

	public boolean isBuiltInReference(String name) {
		return builtInTest.test(name);
	}
}

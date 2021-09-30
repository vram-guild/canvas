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

package grondag.canvas;

import org.apache.logging.log4j.LogManager;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.Platform;
import org.lwjgl.system.jemalloc.JEmalloc;

public class CanvasPreLaunch {
	public static void init(boolean isDev) {
		// Hat tip to JellySquid for this...
		// LWJGL 3.2.3 ships Jemalloc 5.2.0 which seems to be broken on Windows and suffers from critical memory leak problems
		// Using the system allocator prevents memory leaks and other problems
		// See changelog here: https://github.com/jemalloc/jemalloc/releases/tag/5.2.1
		if (Platform.get() == Platform.WINDOWS && isJEmallocPotentiallyBuggy()) {
			if (!"system".equals(Configuration.MEMORY_ALLOCATOR.get())) {
				Configuration.MEMORY_ALLOCATOR.set("system");
				LogManager.getLogger("Canvas").info("Canvas configured LWJGL to use the system memory allocator due to a potential memory leak in JEmalloc.");
			}
		}

		if (isDev) {
			try {
				System.loadLibrary("renderdoc");
			} catch (final Throwable e) {
				// eat it
			}
		}
	}

	private static boolean isJEmallocPotentiallyBuggy() {
		// done this way to make eclipse shut up in dev
		int major = JEmalloc.JEMALLOC_VERSION_MAJOR;
		int minor = JEmalloc.JEMALLOC_VERSION_MINOR;
		int patch = JEmalloc.JEMALLOC_VERSION_BUGFIX;

		if (major == 5) {
			if (minor < 2) {
				return true;
			} else if (minor == 2) {
				return patch == 0;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
}

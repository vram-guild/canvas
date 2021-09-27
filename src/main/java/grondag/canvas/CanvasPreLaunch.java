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

package grondag.canvas;

import io.vram.frex.api.renderer.RendererInitializer;
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

		RendererInitializer.register("grondag.canvas.apiimpl.Canvas", "instance", Thread.currentThread().getContextClassLoader(), false);

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

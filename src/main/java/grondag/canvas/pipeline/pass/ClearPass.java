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

package grondag.canvas.pipeline.pass;

import grondag.canvas.pipeline.Pipeline;
import grondag.canvas.pipeline.config.PassConfig;
import grondag.canvas.varia.GFX;

public class ClearPass extends Pass {
	ClearPass(PassConfig config) {
		super(config);
	}

	@Override
	public void run() {
		if (fbo != null) {
			fbo.bind();
			GFX.viewport(0, 0, Pipeline.width(), Pipeline.height());
			fbo.clear();
		}
	}

	@Override
	public void close() {
		// NOOP
	}
}

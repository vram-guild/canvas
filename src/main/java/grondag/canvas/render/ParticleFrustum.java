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

package grondag.canvas.render;

import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ParticleFrustum extends CanvasFrustum {
	public void prepare(Matrix4f modelMatrix, float tickDelta, Camera camera, Matrix4f projectionMatrix) {
		final Vec3d vec = camera.getPos();
		lastViewXf = (float) vec.x;
		lastViewYf = (float) vec.y;
		lastViewZf = (float) vec.z;

		lastModelMatrix.set(modelMatrix);
		lastProjectionMatrix.set(projectionMatrix);

		mvpMatrix.loadIdentity();
		mvpMatrix.multiply(lastProjectionMatrix);
		mvpMatrix.multiply(lastModelMatrix);

		// depends on mvpMatrix being complete
		extractPlanes();
	}
}

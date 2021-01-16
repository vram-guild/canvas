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

package grondag.canvas.varia;

import net.minecraft.client.util.math.Vector3f;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Matrix4f;

@FunctionalInterface
public interface CelestialObjectFunction {
	void compute(CelestialObjectInput input, CelestialObjectOutput output);

	CelestialObjectFunction VANILLA_SUN = (input, output) -> {
		final float angle = input.world().getSkyAngle(input.tickDelta());
		final Matrix4f matrix = input.workingMatrix();
		matrix.loadIdentity();
		matrix.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-90.0F));
		matrix.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(angle * 360.0F));
		output.cameraToObject.set(0, 1, 0, 0);
		output.cameraToObject.transform(matrix);

		final float[] fs = input.world().getSkyProperties().getFogColorOverride(angle, input.tickDelta());

		if (fs == null) {
			output.atmosphericColorModifier.set(1, 1, 1);
		} else {
			output.atmosphericColorModifier.set(fs[0], fs[1], fs[2]);
		}

		output.lightColor.set(1, 1, 1);
		output.illuminance = 32000f;
	};

	CelestialObjectFunction VANILLA_MOON = (input, output) -> {
		final Matrix4f matrix = input.workingMatrix();
		matrix.loadIdentity();
		matrix.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-90.0F));
		matrix.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(input.world().getSkyAngle(input.tickDelta()) * 360.0F + 180.0F));
		output.cameraToObject.set(0, 1, 0, 0);
		output.cameraToObject.transform(matrix);

		output.atmosphericColorModifier.set(1, 1, 1);
		// based on vanilla sky lightmap at midnight
		// real moonlight is reddish but not so much
		output.lightColor.set(1, 0.5475f, 0.5475f);
		output.illuminance = 2000;
	};

	CelestialObjectFunction DEFAULT_SUN = (input, output) -> {
		final float angle = input.world().getSkyAngle(input.tickDelta());
		final Matrix4f matrix = input.workingMatrix();
		matrix.loadIdentity();
		matrix.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-90.0F));
		matrix.multiply(Vector3f.POSITIVE_Z.getDegreesQuaternion(20.0F));
		matrix.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(angle * 360.0F));
		output.cameraToObject.set(0, 1, 0, 0);
		output.cameraToObject.transform(matrix);

		final float[] fs = input.world().getSkyProperties().getFogColorOverride(angle, input.tickDelta());

		if (fs == null) {
			output.atmosphericColorModifier.set(1, 1, 1);
		} else {
			output.atmosphericColorModifier.set(fs[0], fs[1], fs[2]);
		}

		output.lightColor.set(1, 1, 1);
		output.illuminance = 32000f;
	};

	// Vanilla skylight 51%:
	// 0: dac7c7
	// noon: fbfbfb
	// 1000 (day): fbfbfb
	// 12500: bd9a9a
	// 13000 (night): 906262
	// 14000: 7e5151
	// 18000 (midnight): 7e5151

	// full brightness
	// noon: fcfcfc
	// midnight: b37676

	public interface CelestialObjectInput {
		ClientWorld world();
		float tickDelta();
		double cameraX();
		double cameraY();
		double cameraZ();

		/** Hold no input data - for implementations to prevent allocation. */
		Matrix4f workingMatrix();
	}

	class CelestialObjectOutput {
		public final Vector4f cameraToObject = new Vector4f();

		public final Vector3f lightColor = new Vector3f();

		public final Vector3f atmosphericColorModifier = new Vector3f();

		public float illuminance;
	}
}

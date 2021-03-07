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

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.Profiler;

import grondag.canvas.buffer.encoding.DrawableBuffer;
import grondag.canvas.mixinterface.WorldRendererExt;

class WorldRenderPassContext {
	public static final WorldRenderPassContext INSTANCE = new WorldRenderPassContext();

	public CanvasWorldRenderer canvasWorldRenderer;
	public Camera camera;
	public Vec3d cameraPos;
	public double cameraX;
	public double cameraY;
	public double cameraZ;
	public DrawableBuffer entityBuffer;
	public ClientWorld world;
	public Profiler profiler;
	public WorldRendererExt wr;
	public MinecraftClient mc;
	public float tickDelta;
	public GameRenderer gameRenderer;
	public float viewDistance;
	public boolean thickFog;
	MatrixStack viewMatrixStack;
	MatrixStack identityStack;

	public void setCamera(Camera camera) {
		this.camera = camera;
		cameraPos = camera.getPos();
		cameraX = cameraPos.getX();
		cameraY = cameraPos.getY();
		cameraZ = cameraPos.getZ();
	}
}

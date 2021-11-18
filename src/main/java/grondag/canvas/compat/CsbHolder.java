/*
 * Copyright © Original Authors
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

package grondag.canvas.compat;

//
//import java.lang.invoke.MethodHandle;
//import java.lang.invoke.MethodHandles;
//import java.lang.reflect.Method;
//
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//import net.minecraft.client.render.Camera;
//import net.minecraft.client.render.GameRenderer;
//import net.minecraft.client.render.LightmapTextureManager;
//import net.minecraft.client.render.VertexConsumerProvider;
//import net.minecraft.client.render.WorldRenderer;
//import net.minecraft.client.util.math.MatrixStack;
//import net.minecraft.util.math.Matrix4f;
//import net.minecraft.util.math.Vec3d;
//import net.minecraft.util.profiler.Profiler;
//
//import net.fabricmc.loader.api.FabricLoader;
//
//import grondag.canvas.CanvasMod;
//
// TODO: On hold until have response to CSB #20

class CsbHolder {
//	private static boolean warnRender = true;
//
//	public static CsbRender handler = (MatrixStack matrices, float delta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci) -> {};
//
//	static {
//		if (FabricLoader.getInstance().isModLoaded("dynocaps")) {
//
//			final MethodHandles.Lookup lookup = MethodHandles.lookup();
//
//			try {
//				final Class<?> clazz = WorldRenderer.class;
//
//				Method render = null;
//
//				final Method render = clazz.getDeclaredMethod("handler$zeh000$renderWorldBorder", MatrixStack.class, float.class, long.class, boolean.class, Camera.class, GameRenderer.class, LightmapTextureManager.class, Matrix4f.class, CallbackInfo.class);
//				final MethodHandle renderHandler = lookup.unreflect(render);
//
//				handler = (profiler, matrixStack, immediate, camPos) -> {
//					try  {
//						profiler.swap("dynocaps");
//						renderHandler.invokeExact(matrixStack, immediate, 1, camPos);
//					} catch (final Throwable e) {
//						if (warnRender) {
//							CanvasMod.LOG.warn("Unable to call DynoCaps BoxRenderer.render hook due to exception:", e);
//							CanvasMod.LOG.warn("Subsequent errors will be suppressed");
//							warnRender = false;
//						}
//					}
//				};
//
//				CanvasMod.LOG.info("Found DynoCaps - compatibility hook enabled");
//			} catch (final Exception e)  {
//				CanvasMod.LOG.warn("Unable to find DynoCaps render hook due to exception:", e);
//			}
//		}
//	}
//
//	public interface CsbRender {
//		void render(MatrixStack matrices, float delta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo ci);
//	}
//
//
//	private void net.minecraft.class_761.handler$zeh000$renderWorldBorder(net.minecraft.class_4587,float,long,boolean,net.minecraft.class_4184,net.minecraft.class_757,net.minecraft.class_765,net.minecraft.class_1159,org.spongepowered.asm.mixin.injection.callback.CallbackInfo)
//
}

//package grondag.canvas.compat;
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

//public class CsbHolder {
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
//}

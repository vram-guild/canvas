package grondag.canvas;

import net.minecraft.client.MinecraftClient;

//@LangKey("config.general")
//@Config(modid = Acuity.MODID, type = Type.INSTANCE)
public class Configurator {
//    @LangKey("config.max_pipelines")
//    @RequiresMcRestart
//    @Comment({"Maximum number of render pipelines that can be registered at runtime.",
//        " The value is fixed at startup to enable very fast lookups.",
//        " Smaller values will save slightly on memory overhead.  It isn't much but",
//        " is configurable for those folks who like to save every byte possible...."})
//    @RangeInt(min = 16, max = 1024)
    public static int maxPipelines = 64;

//    @LangKey("config.acuity_enable_vao")
//    @Comment({"Use Vertex Array Objects if available.",
//        " VAOs generally improve performance when they are supported."})
    public static boolean enable_vao = true;

//    @LangKey("config.acuity_fancy_fluids")
//    @Comment({"Enable fancy water and lava rendering.",
//        " This feature is currently work in progress and has no visible effect if enabled."})
    public static boolean fancyFluids = false;

//    @LangKey("config.enable_render_stats")
//    @RequiresMcRestart
//    @Comment({"When enabled, tracks and outputs timing statistics for rendering.",
//        " Has a small performance impact. Useful only for testing."})
    public static boolean enableRenderStats = false;

//    @LangKey("config.enable_block_stats")
//    @RequiresMcRestart
//    @Comment({"When enabled, tracks and outputs timing statistics for lighting ",
//        " and buffering block models during chunk rebuilds.",
//        " Has a small performance impact. Useful only for testing."})
    public static boolean enableBlockStats = false;

//    @LangKey("config.enable_fluid_stats")
//    @RequiresMcRestart
//    @Comment({"When enabled, tracks and outputs timing statistics for lighting ",
//        " and buffering fluid models during chunk rebuilds.",
//        " Has a small performance impact. Useful only for testing."})
    public static boolean enableFluidStats = false;

//    @LangKey("config.disable_yield")
//    @RequiresMcRestart
//    @Comment({"When enabled, disables the call to Thread.yield() in the main game loop ",
//        " that normally occurs right after display update. The call is probably meant",
//        " to give the OpenGL drivers time to process the command buffer, but in the multi-threaded game ",
//        " Minecraft has become, and with modern drivers, this basically invites other tasks to step on your framerate.",
//        " This patch is purely a performance optimization and is not required for Acuity to operate."})
    public static boolean disableYieldInGameLoop = true;

    public static void handleChange() // PostConfigChangedEvent event)
    {
        boolean oldFancyFluids = fancyFluids;
        boolean oldVAO = enable_vao;

//        ConfigManager.sync(Acuity.MODID, Config.Type.INSTANCE);
        if (oldFancyFluids != fancyFluids || oldVAO != enable_vao) {
            RendererImpl.INSTANCE.forceReload();

            // refresh appearances
            MinecraftClient.getInstance().worldRenderer.reload();
        }

    }
}
package grondag.canvas;

//import java.io.File;

public class LoadingConfig {
    public static LoadingConfig INSTANCE = new LoadingConfig(); // new File(Launch.minecraftHome, "config/canvas.cfg"));

    public final boolean disableYieldInGameLoop = true;
    public final boolean enableRenderStats = false;
    public final boolean enableFluidStats = false;
    public final boolean enableBlockStats = false;

//    private LoadingConfig(File file)
//    {
//        if (!file.exists())
//        {
//            disableYieldInGameLoop = false;
//            enableRenderStats = false;
//            enableFluidStats = false;
//            enableBlockStats = false;
//            return;
//        }
//
//        Configuration config = new Configuration(file);
//        disableYieldInGameLoop = config.get(Configuration.CATEGORY_GENERAL, "disableYieldInGameLoop", true).getBoolean();
//        enableRenderStats = config.get(Configuration.CATEGORY_GENERAL, "enableRenderStats", false).getBoolean();
//        enableFluidStats = config.get(Configuration.CATEGORY_GENERAL, "enableFluidStats", false).getBoolean();
//        enableBlockStats = config.get(Configuration.CATEGORY_GENERAL, "enableBlockStats", false).getBoolean();
//    }
}

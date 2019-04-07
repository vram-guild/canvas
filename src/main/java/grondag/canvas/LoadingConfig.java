/*******************************************************************************
 * Copyright 2019 grondag
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

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

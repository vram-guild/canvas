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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;
import grondag.canvas.apiimpl.RendererImpl;
import grondag.frex.api.RendererAccess;

public class Canvas implements ModInitializer {
    public static Canvas INSTANCE = new Canvas();

    @Override
    public void onInitialize() {
        RendererAccess.INSTANCE.registerRenderer(RendererImpl.INSTANCE);
    }

    private static Logger log;

    public Logger log() {
        Logger result = log;
        if (result == null) {
            result = LogManager.getLogger("Canvas");
            log = result;
        }
        return result;
    }
}

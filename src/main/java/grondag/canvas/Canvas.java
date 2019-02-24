/*******************************************************************************
 * Copyright (C) 2018 grondag
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package grondag.canvas;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import grondag.canvas.opengl.CanvasGlHelper;
import grondag.fermion.IGrondagMod;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;

public class Canvas implements ModInitializer, IGrondagMod {
    public static Canvas INSTANCE = new Canvas();
    private static boolean isEnabled = true;
    
    @Override
    public void onInitialize() {
        
    }

    private static Logger log;

    @Override
    public Logger getLog() {
        Logger result = log;
        if (result == null) {
            result = LogManager.getLogger("Canvas");
            log = result;
        }
        return result;
    }

    @Override
    public String modID() {
        return "canvas";
    }

    public static boolean isModEnabled() {
        return isEnabled;
    }
    
    public static void enable(boolean enable) {
        if(enable != isEnabled) {
            isEnabled = enable;
            // important to reload renderers immediately in case 
            // this results in change of vbo to/from  displaylists
            // or changes rendering pipline logic path
            MinecraftClient.getInstance().worldRenderer.reload();
            
            final boolean isEnabled = Canvas.isModEnabled();
            RendererImpl.INSTANCE.forEachListener(c -> c.onStatusChange(isEnabled));
            
            // Don't think this is needed because different interface for pipelined models
            // Minecraft.getMinecraft().refreshResources();
        }
    }
}

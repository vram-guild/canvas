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

package grondag.canvas.material;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.lwjgl.opengl.GL11;

import com.google.common.io.CharStreams;
import com.mojang.blaze3d.platform.GLX;

import grondag.canvas.Canvas;
import grondag.canvas.Configurator;
import grondag.canvas.varia.CanvasGlHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

abstract class AbstractGlShader {
    public final Identifier shaderSource;

    private final int shaderType;
    private final int shaderProps;
    private final ShaderContext context;

    private int glId = -1;
    private boolean needsLoad = true;
    private boolean isErrored = false;

    AbstractGlShader(Identifier shaderSource, int shaderType, int shaderProps, ShaderContext context) {
        this.shaderSource = shaderSource;
        this.shaderType = shaderType;
        this.shaderProps = shaderProps;
        this.context = context;
    }

    /**
     * Call after render / resource refresh to force shader reload.
     */
    public final void forceReload() {
        this.needsLoad = true;
    }

    public final int glId() {
        if (this.needsLoad)
            this.load();

        return this.isErrored ? -1 : this.glId;
    }

    private final void load() {
        this.needsLoad = false;
        this.isErrored = false;
        try {
            if (this.glId <= 0) {
                this.glId = GLX.glCreateShader(shaderType);
                if (this.glId == 0) {
                    this.glId = -1;
                    this.isErrored = true;
                    return;
                }
            }
            
            final String source = this.getSource();
            
            if(Configurator.enableShaderDebug) {
                final String key = shaderSource.toString().replace("/", "-") + "."  + context.toString() +  "." + shaderProps;
                File gameDir = FabricLoader.getInstance().getGameDirectory();
                File shaderDir = new File(gameDir.getAbsolutePath().replace(".", "canvas_shader_debug"));
                if(!shaderDir.exists()) {
                    shaderDir.mkdir();
                }
                if(shaderDir.exists()) {
                    FileWriter writer = new FileWriter(shaderDir.getAbsolutePath() + File.separator + key + ".glsl", false);
                    writer.write(source);
                    writer.close();
                }
            }
            
            GLX.glShaderSource(this.glId, source);
            GLX.glCompileShader(this.glId);

            if (GLX.glGetShaderi(this.glId, GLX.GL_COMPILE_STATUS) == GL11.GL_FALSE)
                throw new RuntimeException(CanvasGlHelper.getShaderInfoLog(this.glId));

        } catch (Exception e) {
            this.isErrored = true;
            if (this.glId > 0) {
                GLX.glDeleteShader(glId);
                this.glId = -1;
            }
            Canvas.LOG.error(I18n.translate("misc.fail_create_shader", this.shaderSource.toString(),
                    Integer.toString(this.shaderProps), e.getMessage()));
        }
    }

    public String buildSource(String librarySource) {
        String result = getShaderSource(this.shaderSource);
        result = result.replaceAll("#version\\s+120", "");
        result = librarySource + result;

        final int spriteDepth = ShaderProps.spriteDepth(shaderProps);
        
        if (spriteDepth > 1)
            result = result.replaceAll("#define LAYER_COUNT 1", String.format("#define LAYER_COUNT %d", spriteDepth));
        
        result = result.replaceAll("#define CONTEXT 0", "#define CONTEXT " + context.ordinal());

        if(!context.isBlock) {
            result = result.replaceAll("#define CONTEXT_IS_BLOCK", "#define CONTEXT_IS_NOT_BLOCK");
        }
        
        if(!context.isItem) {
            result = result.replaceAll("#define CONTEXT_IS_ITEM", "#define CONTEXT_IS_NOT_ITEM");
        }

        if(!Configurator.enableHdLightmaps || ((shaderProps & ShaderProps.SMOOTH_LIGHTMAPS) == 0)) {
            result = result.replaceAll("#define ENABLE_SMOOTH_LIGHT", "#define DISABLE_SMOOTH_LIGHT");
        }
        
        if(!Configurator.enableLightmapNoise || !Configurator.enableHdLightmaps) {
            result = result.replaceAll("#define ENABLE_LIGHT_NOISE", "#define DISABLE_LIGHT_NOISE");
        }
        
        if(!Configurator.enableAoShading) {
            result = result.replaceAll("#define ENABLE_AO_SHADING", "#define DISABLE_AO_SHADING");
        }
        
        if(!Configurator.enableSubtleAo) {
            result = result.replaceAll("#define ENABLE_SUBTLE_AO", "#define DISABLE_SUBTLE_AO");
        }
        
        if(!Configurator.enableDiffuseShading) {
            result = result.replaceAll("#define ENABLE_DIFFUSE_SHADING", "#define DISABLE_DIFFUSE_SHADING");
        }
        
        if(!CanvasGlHelper.useGpuShader4() ) {
            result = result.replaceAll("#extension GL_EXT_gpu_shader4 : enable", "");
        }
        
        if((shaderProps & ShaderProps.WHITE_0) == 0) {
            result = result.replaceAll("#define WHITE_0", "#define NOT_WHITE_0");
        }
        
        return result;
    }

    abstract String getSource();

    public static String getShaderSource(Identifier shaderSource) {
        try {
            ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
            InputStream in = rm.getResource(shaderSource).getInputStream();
            if (in == null)
                return "";
            final Reader reader = new InputStreamReader(in);
            return CharStreams.toString(reader);
        } catch (IOException e) {
            return "";
        }
    }
}

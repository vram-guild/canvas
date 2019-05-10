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

package grondag.canvas.apiimpl.util;

import java.io.IOException;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL21;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.resource.ResourceManager;

/**
 * Leaner adaptation of Minecraft NativeImageBackedTexture suitable for our needs.
 */
public class SimpleTexture extends AbstractTexture implements AutoCloseable, Texture {
    private SimpleImage image;

    public SimpleTexture(SimpleImage image, int internalFormat) {
        this.image = image;

        this.bindTexture();
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MIN_LOD, 0);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LOD, 0);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL21.GL_TEXTURE_LOD_BIAS, 0.0F);
        GlStateManager.texImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, image.width, image.height, 0, image.pixelDataFormat, image.pixelDataType, (IntBuffer)null);
    }

    @Override
    public void load(ResourceManager resourceManager) throws IOException {
    }

    public void upload() {
        this.bindTexture();
        this.image.upload(0, 0, 0, false);
    }

    public void uploadPartial(int x, int y, int width, int height) {
        this.bindTexture();
        this.image.upload(0, x, y, 0, 0, width, height, false);
    }

    @Nullable
    public SimpleImage getImage() {
        return this.image;
    }

    public void setImage(SimpleImage image) throws Exception {
        this.image.close();
        this.image = image;
    }

    @Override
    public void close() {
        this.image.close();
        this.clearGlId();
        this.image = null;
    }
}

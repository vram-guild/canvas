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

import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GlStateManager;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.UntrackMemoryUtil;

/**
 * Leaner adaptation of Minecraft NativeImage suitable for our needs.
 */
@Environment(EnvType.CLIENT)
public final class SimpleImage implements AutoCloseable {
    private final int width;
    private final int height;
    private long pointer;
    private final int sizeBytes;
    private final int bytesPerPixel;
    private final int pixelDataFormat;


    public SimpleImage(int bytesPerPixel, int pixelDataFormat, int width, int height, boolean calloc) {
        this.bytesPerPixel = bytesPerPixel;
        this.pixelDataFormat = pixelDataFormat;
        this.width = width;
        this.height = height;
        this.sizeBytes = width * height * bytesPerPixel;
        if (calloc) {
            this.pointer = MemoryUtil.nmemCalloc(1L, (long)this.sizeBytes);
        } else {
            this.pointer = MemoryUtil.nmemAlloc((long)this.sizeBytes);
        }

    }

    private static void setTextureClamp(boolean clamp) {
        final int wrap = clamp ? GL11.GL_CLAMP : GL11.GL_REPEAT;
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap);
    }

    private static void setTextureFilter(boolean interpolate, boolean mipmap) {
        if (interpolate) {
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mipmap ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR);
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        } else {
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mipmap ? GL11.GL_NEAREST_MIPMAP_LINEAR : GL11.GL_NEAREST);
            GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        }

    }

    @Override
    public void close() {
        if (this.pointer != 0L) {
            MemoryUtil.nmemFree(this.pointer);
        }
        this.pointer = 0L;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public int getPixelRGBA(int x, int y) {
        assert this.bytesPerPixel == 4;
        assert x <= this.width && y <= this.height;
        assert pointer != 0L : "Image not allocated.";
        return MemoryUtil.memIntBuffer(this.pointer, this.sizeBytes).get(x + y * this.width);
    }

    public void setPixelRGBA(int x, int y, int rgba) {
        assert this.bytesPerPixel == 4;
        assert x <= this.width && y <= this.height;
        assert pointer != 0L : "Image not allocated.";
        MemoryUtil.memIntBuffer(this.pointer, this.sizeBytes).put(x + y * this.width, rgba);
    }

    public void setLuminance(int u, int v, byte value) {
        assert this.bytesPerPixel == 1;
        assert u <= this.width && v <= this.height;
        assert pointer != 0L : "Image not allocated.";
        MemoryUtil.memByteBuffer(this.pointer, this.sizeBytes).put(u + v * this.width, value);
    }

    public void upload(int lod, int x, int y, boolean clamp) {
        this.upload(lod, x, y, 0, 0, this.width, this.height, clamp);
    }

    public void upload(int lod, int x, int y, int skipPixels, int skipRows, int width, int height, boolean clamp) {
        this.upload(lod, x, y, skipPixels, skipRows, width, height, false, false, clamp);
    }

    public void upload(int lod, int x, int y, int skipPixels, int skipRows, int width, int height, boolean interpolate, boolean clamp, boolean mipmap) {
        assert pointer != 0L : "Image not allocated.";
        setTextureFilter(interpolate, mipmap);
        setTextureClamp(clamp);
        if (width == this.width) {
            GlStateManager.pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
        } else {
            GlStateManager.pixelStore(GL11.GL_UNPACK_ROW_LENGTH, this.width);
        }

        GlStateManager.pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
        GlStateManager.pixelStore(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
        setUnpackAlignment();
        GlStateManager.texSubImage2D(GL11.GL_TEXTURE_2D, lod, x, y, width, height, pixelDataFormat, GL11.GL_UNSIGNED_BYTE, pointer);
    }

    public void untrack() {
        UntrackMemoryUtil.untrack(this.pointer);
    }

    private void setUnpackAlignment() {
        GlStateManager.pixelStore(3317, this.bytesPerPixel);
    }
}

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.UntrackMemoryUtil;

/**
 * Adapted from Minecraft NativeImage but suitable for our needs.
 * "Reliable" because won't change and I know exactly what it does.
 */
@Environment(EnvType.CLIENT)
public final class ReliableImage implements AutoCloseable {
   private static final Set<StandardOpenOption> WRITE_TO_FILE_OPEN_OPTIONS;
   private final ReliableImage.Format format;
   private final int width;
   private final int height;
   private final boolean isStbImage;
   private long pointer;
   private final int sizeBytes;

   public ReliableImage(int int_1, int int_2, boolean boolean_1) {
      this(ReliableImage.Format.RGBA, int_1, int_2, boolean_1);
   }

   public ReliableImage(ReliableImage.Format format, int width, int height, boolean calloc) {
      this.format = format;
      this.width = width;
      this.height = height;
      this.sizeBytes = width * height * format.getBytesPerPixel();
      this.isStbImage = false;
      if (calloc) {
         this.pointer = MemoryUtil.nmemCalloc(1L, (long)this.sizeBytes);
      } else {
         this.pointer = MemoryUtil.nmemAlloc((long)this.sizeBytes);
      }

   }

   private ReliableImage(ReliableImage.Format format, int width, int height, boolean isStb, long pointer) {
      this.format = format;
      this.width = width;
      this.height = height;
      this.isStbImage = isStb;
      this.pointer = pointer;
      this.sizeBytes = width * height * format.getBytesPerPixel();
   }

   @Override
   public String toString() {
      return "ReliableImage[" + this.format + " " + this.width + "x" + this.height + "@" + this.pointer + (this.isStbImage ? "S" : "N") + "]";
   }

   public static ReliableImage fromInputStream(InputStream inputStream_1) throws IOException {
      return fromInputStream(ReliableImage.Format.RGBA, inputStream_1);
   }

   public static ReliableImage fromInputStream(@Nullable ReliableImage.Format nativeImage$Format_1, InputStream inputStream_1) throws IOException {
      ByteBuffer byteBuffer_1 = null;

      ReliableImage var3;
      try {
         byteBuffer_1 = TextureUtil.readResource(inputStream_1);
         byteBuffer_1.rewind();
         var3 = fromByteBuffer(nativeImage$Format_1, byteBuffer_1);
      } finally {
         MemoryUtil.memFree(byteBuffer_1);
         IOUtils.closeQuietly(inputStream_1);
      }

      return var3;
   }

   public static ReliableImage fromByteBuffer(ByteBuffer byteBuffer_1) throws IOException {
      return fromByteBuffer(ReliableImage.Format.RGBA, byteBuffer_1);
   }

   public static ReliableImage fromByteBuffer(@Nullable ReliableImage.Format nativeImage$Format_1, ByteBuffer byteBuffer_1) throws IOException {
      if (nativeImage$Format_1 != null && !nativeImage$Format_1.method_4338()) {
         throw new UnsupportedOperationException("Don't know how to read format " + nativeImage$Format_1);
      } else if (MemoryUtil.memAddress(byteBuffer_1) == 0L) {
         throw new IllegalArgumentException("Invalid buffer");
      } else {
         MemoryStack memoryStack_1 = MemoryStack.stackPush();
         Throwable var3 = null;

         ReliableImage var8;
         try {
            IntBuffer intBuffer_1 = memoryStack_1.mallocInt(1);
            IntBuffer intBuffer_2 = memoryStack_1.mallocInt(1);
            IntBuffer intBuffer_3 = memoryStack_1.mallocInt(1);
            ByteBuffer byteBuffer_2 = STBImage.stbi_load_from_memory(byteBuffer_1, intBuffer_1, intBuffer_2, intBuffer_3, nativeImage$Format_1 == null ? 0 : nativeImage$Format_1.bytesPerPixel);
            if (byteBuffer_2 == null) {
               throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
            }

            var8 = new ReliableImage(nativeImage$Format_1 == null ? ReliableImage.Format.method_4336(intBuffer_3.get(0)) : nativeImage$Format_1, intBuffer_1.get(0), intBuffer_2.get(0), true, MemoryUtil.memAddress(byteBuffer_2));
         } catch (Throwable var17) {
            var3 = var17;
            throw var17;
         } finally {
            if (memoryStack_1 != null) {
               if (var3 != null) {
                  try {
                     memoryStack_1.close();
                  } catch (Throwable var16) {
                     var3.addSuppressed(var16);
                  }
               } else {
                  memoryStack_1.close();
               }
            }

         }

         return var8;
      }
   }

   private static void setTextureClamp(boolean clamp) {
      if (clamp) {
         GlStateManager.texParameter(GL11.GL_TEXTURE_2D, 10242, 10496);
         GlStateManager.texParameter(GL11.GL_TEXTURE_2D, 10243, 10496);
      } else {
         GlStateManager.texParameter(GL11.GL_TEXTURE_2D, 10242, 10497);
         GlStateManager.texParameter(GL11.GL_TEXTURE_2D, 10243, 10497);
      }

   }

   private static void setTextureFilter(boolean boolean_1, boolean boolean_2) {
      if (boolean_1) {
         GlStateManager.texParameter(3553, 10241, boolean_2 ? 9987 : 9729);
         GlStateManager.texParameter(3553, 10240, 9729);
      } else {
         GlStateManager.texParameter(3553, 10241, boolean_2 ? 9986 : 9728);
         GlStateManager.texParameter(3553, 10240, 9728);
      }

   }

   private void checkAllocated() {
      if (this.pointer == 0L) {
         throw new IllegalStateException("Image is not allocated.");
      }
   }

   @Override
public void close() {
      if (this.pointer != 0L) {
         if (this.isStbImage) {
            STBImage.nstbi_image_free(this.pointer);
         } else {
            MemoryUtil.nmemFree(this.pointer);
         }
      }

      this.pointer = 0L;
   }

   public int getWidth() {
      return this.width;
   }

   public int getHeight() {
      return this.height;
   }

   public ReliableImage.Format getFormat() {
      return this.format;
   }

   public int getPixelRGBA(int int_1, int int_2) {
      if (this.format != ReliableImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format("getPixelRGBA only works on RGBA images; have %s", this.format));
      } else if (int_1 <= this.width && int_2 <= this.height) {
         this.checkAllocated();
         return MemoryUtil.memIntBuffer(this.pointer, this.sizeBytes).get(int_1 + int_2 * this.width);
      } else {
         throw new IllegalArgumentException(String.format("(%s, %s) outside of image bounds (%s, %s)", int_1, int_2, this.width, this.height));
      }
   }

   public void setPixelRGBA(int int_1, int int_2, int int_3) {
      if (this.format != ReliableImage.Format.RGBA) {
         throw new IllegalArgumentException(String.format("getPixelRGBA only works on RGBA images; have %s", this.format));
      } else if (int_1 <= this.width && int_2 <= this.height) {
         this.checkAllocated();
         MemoryUtil.memIntBuffer(this.pointer, this.sizeBytes).put(int_1 + int_2 * this.width, int_3);
      } else {
         throw new IllegalArgumentException(String.format("(%s, %s) outside of image bounds (%s, %s)", int_1, int_2, this.width, this.height));
      }
   }
   
   public void setLuminance(int u, int v, byte value) {
       if (this.format != ReliableImage.Format.LUMINANCE) {
          throw new IllegalArgumentException(String.format("setLuminance only works on LUMINANCE images; have %s", this.format));
       } else if (u <= this.width && v <= this.height) {
          this.checkAllocated();
          MemoryUtil.memByteBuffer(this.pointer, this.sizeBytes).put(u + v * this.width, value);
       } else {
          throw new IllegalArgumentException(String.format("(%s, %s) outside of image bounds (%s, %s)", u, v, this.width, this.height));
       }
    }

   public byte getAlphaOrLuminance(int int_1, int int_2) {
      if (!this.format.hasLuminanceOrAlpha()) {
         throw new IllegalArgumentException(String.format("no luminance or alpha in %s", this.format));
      } else if (int_1 <= this.width && int_2 <= this.height) {
         return MemoryUtil.memByteBuffer(this.pointer, this.sizeBytes).get((int_1 + int_2 * this.width) * this.format.getBytesPerPixel() + this.format.method_4330() / 8);
      } else {
         throw new IllegalArgumentException(String.format("(%s, %s) outside of image bounds (%s, %s)", int_1, int_2, this.width, this.height));
      }
   }

   public void blendPixel(int int_1, int int_2, int int_3) {
      if (this.format != ReliableImage.Format.RGBA) {
         throw new UnsupportedOperationException("Can only call blendPixel with RGBA format");
      } else {
         int int_4 = this.getPixelRGBA(int_1, int_2);
         float float_1 = (float)(int_3 >> 24 & 255) / 255.0F;
         float float_2 = (float)(int_3 >> 16 & 255) / 255.0F;
         float float_3 = (float)(int_3 >> 8 & 255) / 255.0F;
         float float_4 = (float)(int_3 >> 0 & 255) / 255.0F;
         float float_5 = (float)(int_4 >> 24 & 255) / 255.0F;
         float float_6 = (float)(int_4 >> 16 & 255) / 255.0F;
         float float_7 = (float)(int_4 >> 8 & 255) / 255.0F;
         float float_8 = (float)(int_4 >> 0 & 255) / 255.0F;
         float float_10 = 1.0F - float_1;
         float float_11 = float_1 * float_1 + float_5 * float_10;
         float float_12 = float_2 * float_1 + float_6 * float_10;
         float float_13 = float_3 * float_1 + float_7 * float_10;
         float float_14 = float_4 * float_1 + float_8 * float_10;
         if (float_11 > 1.0F) {
            float_11 = 1.0F;
         }

         if (float_12 > 1.0F) {
            float_12 = 1.0F;
         }

         if (float_13 > 1.0F) {
            float_13 = 1.0F;
         }

         if (float_14 > 1.0F) {
            float_14 = 1.0F;
         }

         int int_5 = (int)(float_11 * 255.0F);
         int int_6 = (int)(float_12 * 255.0F);
         int int_7 = (int)(float_13 * 255.0F);
         int int_8 = (int)(float_14 * 255.0F);
         this.setPixelRGBA(int_1, int_2, int_5 << 24 | int_6 << 16 | int_7 << 8 | int_8 << 0);
      }
   }

   @Deprecated
   public int[] makePixelArray() {
      if (this.format != ReliableImage.Format.RGBA) {
         throw new UnsupportedOperationException("can only call makePixelArray for RGBA images.");
      } else {
         this.checkAllocated();
         int[] ints_1 = new int[this.getWidth() * this.getHeight()];

         for(int int_1 = 0; int_1 < this.getHeight(); ++int_1) {
            for(int int_2 = 0; int_2 < this.getWidth(); ++int_2) {
               int int_3 = this.getPixelRGBA(int_2, int_1);
               int int_4 = int_3 >> 24 & 255;
               int int_5 = int_3 >> 16 & 255;
               int int_6 = int_3 >> 8 & 255;
               int int_7 = int_3 >> 0 & 255;
               int int_8 = int_4 << 24 | int_7 << 16 | int_6 << 8 | int_5;
               ints_1[int_2 + int_1 * this.getWidth()] = int_8;
            }
         }

         return ints_1;
      }
   }

   public void upload(int int_1, int int_2, int int_3, boolean boolean_1) {
      this.upload(int_1, int_2, int_3, 0, 0, this.width, this.height, boolean_1);
   }

   public void upload(int int_1, int int_2, int int_3, int int_4, int int_5, int int_6, int int_7, boolean boolean_1) {
      this.upload(int_1, int_2, int_3, int_4, int_5, int_6, int_7, false, false, boolean_1);
   }

   public void upload(int int_1, int int_2, int int_3, int int_4, int int_5, int int_6, int int_7, boolean boolean_1, boolean boolean_2, boolean boolean_3) {
      this.checkAllocated();
      setTextureFilter(boolean_1, boolean_3);
      setTextureClamp(boolean_2);
      if (int_6 == this.getWidth()) {
         GlStateManager.pixelStore(GL11.GL_UNPACK_ROW_LENGTH, 0);
      } else {
         GlStateManager.pixelStore(GL11.GL_UNPACK_ROW_LENGTH, this.getWidth());
      }

      GlStateManager.pixelStore(GL11.GL_UNPACK_SKIP_PIXELS, int_4);
      GlStateManager.pixelStore(GL11.GL_UNPACK_SKIP_ROWS, int_5);
      this.format.setUnpackAlignment();
      GlStateManager.texSubImage2D(3553, int_1, int_2, int_3, int_6, int_7, this.format.getPixelDataFormat(), 5121, this.pointer);
   }

   public void loadFromTextureImage(int int_1, boolean boolean_1) {
      this.checkAllocated();
      this.format.setPackAlignment();
      GlStateManager.getTexImage(3553, int_1, this.format.getPixelDataFormat(), 5121, this.pointer);
      if (boolean_1 && this.format.method_4329()) {
         for(int int_2 = 0; int_2 < this.getHeight(); ++int_2) {
            for(int int_3 = 0; int_3 < this.getWidth(); ++int_3) {
               this.setPixelRGBA(int_3, int_2, this.getPixelRGBA(int_3, int_2) | 255 << this.format.method_4332());
            }
         }
      }

   }

   public void method_4306(boolean boolean_1) {
      this.checkAllocated();
      this.format.setPackAlignment();
      if (boolean_1) {
         GlStateManager.pixelTransfer(3357, Float.MAX_VALUE);
      }

      GlStateManager.readPixels(0, 0, this.width, this.height, this.format.getPixelDataFormat(), 5121, this.pointer);
      if (boolean_1) {
         GlStateManager.pixelTransfer(3357, 0.0F);
      }

   }

   public void writeFile(String string_1) throws IOException {
      this.writeFile(FileSystems.getDefault().getPath(string_1));
   }

   public void writeFile(File file_1) throws IOException {
      this.writeFile(file_1.toPath());
   }

   public void makeGlyphBitmapSubpixel(STBTTFontinfo sTBTTFontinfo_1, int int_1, int int_2, int int_3, float float_1, float float_2, float float_3, float float_4, int int_4, int int_5) {
      if (int_4 >= 0 && int_4 + int_2 <= this.getWidth() && int_5 >= 0 && int_5 + int_3 <= this.getHeight()) {
         if (this.format.getBytesPerPixel() != 1) {
            throw new IllegalArgumentException("Can only write fonts into 1-component images.");
         } else {
            STBTruetype.nstbtt_MakeGlyphBitmapSubpixel(sTBTTFontinfo_1.address(), this.pointer + (long)int_4 + (long)(int_5 * this.getWidth()), int_2, int_3, this.getWidth(), float_1, float_2, float_3, float_4, int_1);
         }
      } else {
         throw new IllegalArgumentException(String.format("Out of bounds: start: (%s, %s) (size: %sx%s); size: %sx%s", int_4, int_5, int_2, int_3, this.getWidth(), this.getHeight()));
      }
   }

   public void writeFile(Path path_1) throws IOException {
      if (!this.format.method_4338()) {
         throw new UnsupportedOperationException("Don't know how to write format " + this.format);
      } else {
         this.checkAllocated();
         WritableByteChannel writableByteChannel_1 = Files.newByteChannel(path_1, WRITE_TO_FILE_OPEN_OPTIONS);
         Throwable var3 = null;

         try {
            ReliableImage.WriteCallback nativeImage$WriteCallback_1 = new ReliableImage.WriteCallback(writableByteChannel_1);

            try {
               if (!STBImageWrite.stbi_write_png_to_func(nativeImage$WriteCallback_1, 0L, this.getWidth(), this.getHeight(), this.format.getBytesPerPixel(), MemoryUtil.memByteBuffer(this.pointer, this.sizeBytes), 0)) {
                  throw new IOException("Could not write image to the PNG file \"" + path_1.toAbsolutePath() + "\": " + STBImage.stbi_failure_reason());
               }
            } finally {
               nativeImage$WriteCallback_1.free();
            }

            nativeImage$WriteCallback_1.throwStoredException();
         } catch (Throwable var19) {
            var3 = var19;
            throw var19;
         } finally {
            if (writableByteChannel_1 != null) {
               if (var3 != null) {
                  try {
                     writableByteChannel_1.close();
                  } catch (Throwable var17) {
                     var3.addSuppressed(var17);
                  }
               } else {
                  writableByteChannel_1.close();
               }
            }

         }

      }
   }

   public void copyFrom(ReliableImage nativeImage_1) {
      if (nativeImage_1.getFormat() != this.format) {
         throw new UnsupportedOperationException("Image formats don't match.");
      } else {
         int int_1 = this.format.getBytesPerPixel();
         this.checkAllocated();
         nativeImage_1.checkAllocated();
         if (this.width == nativeImage_1.width) {
            MemoryUtil.memCopy(nativeImage_1.pointer, this.pointer, (long)Math.min(this.sizeBytes, nativeImage_1.sizeBytes));
         } else {
            int int_2 = Math.min(this.getWidth(), nativeImage_1.getWidth());
            int int_3 = Math.min(this.getHeight(), nativeImage_1.getHeight());

            for(int int_4 = 0; int_4 < int_3; ++int_4) {
               int int_5 = int_4 * nativeImage_1.getWidth() * int_1;
               int int_6 = int_4 * this.getWidth() * int_1;
               MemoryUtil.memCopy(nativeImage_1.pointer + (long)int_5, this.pointer + (long)int_6, (long)int_2);
            }
         }

      }
   }

   public void fillRGBA(int int_1, int int_2, int int_3, int int_4, int int_5) {
      for(int int_6 = int_2; int_6 < int_2 + int_4; ++int_6) {
         for(int int_7 = int_1; int_7 < int_1 + int_3; ++int_7) {
            this.setPixelRGBA(int_7, int_6, int_5);
         }
      }

   }

   public void method_4304(int int_1, int int_2, int int_3, int int_4, int int_5, int int_6, boolean boolean_1, boolean boolean_2) {
      for(int int_7 = 0; int_7 < int_6; ++int_7) {
         for(int int_8 = 0; int_8 < int_5; ++int_8) {
            int int_9 = boolean_1 ? int_5 - 1 - int_8 : int_8;
            int int_10 = boolean_2 ? int_6 - 1 - int_7 : int_7;
            int int_11 = this.getPixelRGBA(int_1 + int_8, int_2 + int_7);
            this.setPixelRGBA(int_1 + int_3 + int_9, int_2 + int_4 + int_10, int_11);
         }
      }

   }

   public void method_4319() {
      this.checkAllocated();
      MemoryStack memoryStack_1 = MemoryStack.stackPush();
      Throwable var2 = null;

      try {
         int int_1 = this.format.getBytesPerPixel();
         int int_2 = this.getWidth() * int_1;
         long long_1 = memoryStack_1.nmalloc(int_2);

         for(int int_3 = 0; int_3 < this.getHeight() / 2; ++int_3) {
            int int_4 = int_3 * this.getWidth() * int_1;
            int int_5 = (this.getHeight() - 1 - int_3) * this.getWidth() * int_1;
            MemoryUtil.memCopy(this.pointer + (long)int_4, long_1, (long)int_2);
            MemoryUtil.memCopy(this.pointer + (long)int_5, this.pointer + (long)int_4, (long)int_2);
            MemoryUtil.memCopy(long_1, this.pointer + (long)int_5, (long)int_2);
         }
      } catch (Throwable var17) {
         var2 = var17;
         throw var17;
      } finally {
         if (memoryStack_1 != null) {
            if (var2 != null) {
               try {
                  memoryStack_1.close();
               } catch (Throwable var16) {
                  var2.addSuppressed(var16);
               }
            } else {
               memoryStack_1.close();
            }
         }

      }

   }

   public void resizeSubRectTo(int int_1, int int_2, int int_3, int int_4, ReliableImage nativeImage_1) {
      this.checkAllocated();
      if (nativeImage_1.getFormat() != this.format) {
         throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format.");
      } else {
         int int_5 = this.format.getBytesPerPixel();
         STBImageResize.nstbir_resize_uint8(this.pointer + (long)((int_1 + int_2 * this.getWidth()) * int_5), int_3, int_4, this.getWidth() * int_5, nativeImage_1.pointer, nativeImage_1.getWidth(), nativeImage_1.getHeight(), 0, int_5);
      }
   }

   public void untrack() {
      UntrackMemoryUtil.untrack(this.pointer);
   }

   public static ReliableImage fromBase64(String string_1) throws IOException {
      MemoryStack memoryStack_1 = MemoryStack.stackPush();
      Throwable var2 = null;

      ReliableImage var6;
      try {
         ByteBuffer byteBuffer_1 = memoryStack_1.UTF8(string_1.replaceAll("\n", ""), false);
         ByteBuffer byteBuffer_2 = Base64.getDecoder().decode(byteBuffer_1);
         ByteBuffer byteBuffer_3 = memoryStack_1.malloc(byteBuffer_2.remaining());
         byteBuffer_3.put(byteBuffer_2);
         byteBuffer_3.rewind();
         var6 = fromByteBuffer(byteBuffer_3);
      } catch (Throwable var15) {
         var2 = var15;
         throw var15;
      } finally {
         if (memoryStack_1 != null) {
            if (var2 != null) {
               try {
                  memoryStack_1.close();
               } catch (Throwable var14) {
                  var2.addSuppressed(var14);
               }
            } else {
               memoryStack_1.close();
            }
         }

      }

      return var6;
   }

   static {
      WRITE_TO_FILE_OPEN_OPTIONS = EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
   }

   @Environment(EnvType.CLIENT)
   public static enum Format {
      RGBA(4, 6408, true, true, true, false, true, 0, 8, 16, 255, 24, true),
      RGB(3, 6407, true, true, true, false, false, 0, 8, 16, 255, 255, true),
      LUMINANCE_ALPHA(2, 6410, false, false, false, true, true, 255, 255, 255, 0, 8, true),
      LUMINANCE(1, 6409, false, false, false, true, false, 0, 0, 0, 0, 255, true);

      private final int bytesPerPixel;
      private final int pixelDataFormat;
      private final boolean field_5005;
      private final boolean field_5004;
      private final boolean field_5003;
      private final boolean field_5000;
      private final boolean field_4999;
      private final int field_5010;
      private final int field_5009;
      private final int field_5008;
      private final int field_5007;
      private final int field_5006;
      private final boolean field_4996;

      private Format(int int_1, int int_2, boolean boolean_1, boolean boolean_2, boolean boolean_3, boolean boolean_4, boolean boolean_5, int int_3, int int_4, int int_5, int int_6, int int_7, boolean boolean_6) {
         this.bytesPerPixel = int_1;
         this.pixelDataFormat = int_2;
         this.field_5005 = boolean_1;
         this.field_5004 = boolean_2;
         this.field_5003 = boolean_3;
         this.field_5000 = boolean_4;
         this.field_4999 = boolean_5;
         this.field_5010 = int_3;
         this.field_5009 = int_4;
         this.field_5008 = int_5;
         this.field_5007 = int_6;
         this.field_5006 = int_7;
         this.field_4996 = boolean_6;
      }

      public int getBytesPerPixel() {
         return this.bytesPerPixel;
      }

      public void setPackAlignment() {
         GlStateManager.pixelStore(3333, this.getBytesPerPixel());
      }

      public void setUnpackAlignment() {
         GlStateManager.pixelStore(3317, this.getBytesPerPixel());
      }

      public int getPixelDataFormat() {
         return this.pixelDataFormat;
      }

      public boolean method_4329() {
         return this.field_4999;
      }

      public int method_4332() {
         return this.field_5006;
      }

      public boolean hasLuminanceOrAlpha() {
         return this.field_5000 || this.field_4999;
      }

      public int method_4330() {
         return this.field_5000 ? this.field_5007 : this.field_5006;
      }

      public boolean method_4338() {
         return this.field_4996;
      }

      private static ReliableImage.Format method_4336(int int_1) {
         switch(int_1) {
         case 1:
            return LUMINANCE;
         case 2:
            return LUMINANCE_ALPHA;
         case 3:
            return RGB;
         case 4:
         default:
            return RGBA;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum class_1013 {
      RGBA(6408),
      RGB(6407),
      LUMINANCE_ALPHA(6410),
      LUMINANCE(6409),
      INTENSITY(32841);

      private final int field_5015;

      private class_1013(int int_1) {
         this.field_5015 = int_1;
      }

      public int method_4341() {
         return this.field_5015;
      }
   }

   @Environment(EnvType.CLIENT)
   static class WriteCallback extends STBIWriteCallback {
      private final WritableByteChannel channel;
      private IOException exception;

      private WriteCallback(WritableByteChannel writableByteChannel_1) {
         this.channel = writableByteChannel_1;
      }

      @Override
    public void invoke(long long_1, long long_2, int int_1) {
         ByteBuffer byteBuffer_1 = getData(long_2, int_1);

         try {
            this.channel.write(byteBuffer_1);
         } catch (IOException var8) {
            this.exception = var8;
         }

      }

      public void throwStoredException() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         }
      }

      // $FF: synthetic method
      WriteCallback(WritableByteChannel writableByteChannel_1, Object nativeImage$1_1) {
         this(writableByteChannel_1);
      }
   }
}

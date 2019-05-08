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

import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.TextureUtil;

import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.Texture;
import net.minecraft.resource.ResourceManager;

/**
 * Adapted from Minecraft NativeImage but suitable for our needs.
 * "Reliable" because won't change and I know exactly what it does.
 */
public class ReliableTexture extends AbstractTexture implements AutoCloseable, Texture {
    private ReliableImage image;

    public ReliableTexture(ReliableImage nativeImage_1) {
       this.image = nativeImage_1;
       TextureUtil.prepareImage(this.getGlId(), this.image.getWidth(), this.image.getHeight());
       this.upload();
    }

    public ReliableTexture(int int_1, int int_2, boolean boolean_1) {
       this.image = new ReliableImage(int_1, int_2, boolean_1);
       TextureUtil.prepareImage(this.getGlId(), this.image.getWidth(), this.image.getHeight());
    }

    @Override
    public void load(ResourceManager resourceManager_1) throws IOException {
    }

    public void upload() {
       this.bindTexture();
       this.image.upload(0, 0, 0, false);
    }

    @Nullable
    public ReliableImage getImage() {
       return this.image;
    }

    public void setImage(ReliableImage nativeImage_1) throws Exception {
       this.image.close();
       this.image = nativeImage_1;
    }

    @Override
    public void close() {
       this.image.close();
       this.clearGlId();
       this.image = null;
    }
}

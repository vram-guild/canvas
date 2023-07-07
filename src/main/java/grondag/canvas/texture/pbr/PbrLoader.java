/*
 * This file is part of Canvas Renderer and is licensed to the project under
 * terms that are compatible with the GNU Lesser General Public License.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership and licensing.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package grondag.canvas.texture.pbr;

import java.util.ArrayList;
import java.util.Collection;

import net.minecraft.client.resources.model.AtlasSet;
import net.minecraft.server.packs.resources.ResourceManager;

import grondag.canvas.CanvasMod;
import grondag.canvas.mixinterface.SpriteContentsExt;
import grondag.canvas.mixinterface.StitchResultExt;

public class PbrLoader {
	static final InputTextureManager inputTextureManager = new InputTextureManager();

	public static void errorOrdering() {
		CanvasMod.LOG.error("Can't load PBR atlases due to load ordering error!");
	}

	public static void reload(ResourceManager manager, Collection<AtlasSet.StitchResult> atlasPreparations) {
		var atlases = new ArrayList<PbrMapAtlas>();
		var debugProcess = new PbrProcess();

		for (var rawEntry : atlasPreparations) {
			final var entry = (StitchResultExt) rawEntry;
			final var atlas = entry.canvas_atlas();
			final var preps = entry.canvas_preparations();

			PbrMapAtlas pbrAtlas = new PbrMapAtlas(atlas.location(), preps.width(), preps.height(), preps.mipLevel());
			atlases.add(pbrAtlas);

			for (var spriteEntry : preps.regions().entrySet()) {
				var sprite = spriteEntry.getValue();
				var contents = spriteEntry.getValue().contents();
				var contentsExt = (SpriteContentsExt) contents;
				inputTextureManager.addInputImage(spriteEntry.getKey(), contentsExt.canvas_images()[0]);

				var pbrSprite = new PbrMapSprite(atlas.location(), spriteEntry.getKey(), sprite.getX(), sprite.getY(), contents.width(), contents.height());
				pbrAtlas.sprites.add(pbrSprite);

				// debug
				// pbrSprite.withProcess(PbrMapSpriteLayer.NORMAL, debugProcess, new InputTextureManager.InputTexture[]{inputTextureManager.getSpriteDefault(spriteEntry.getKey())});

				// CanvasMod.LOG.info(atlas.location() + ", " + spriteEntry.getKey() + ", " + spriteEntry.getValue().contentsExt().name());
			}
		}

		// we are in Render thread the whole time
		inputTextureManager.uploadInputs();

		var context = new PbrProcess.DrawContext();
		context.drawBuffer.bind();

		for (var atlas : atlases) {
			atlas.processAndUpload();
		}

		inputTextureManager.clear();
		context.close();
	}
}

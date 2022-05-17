# Canvas
Canvas is a shader-based Renderer for the [Fabric](https://fabricmc.net) modding toolchain.  It supports all features of the proposed [Fabric Rendering API](https://github.com/FabricMC/fabric/pull/65) plus extensions defined in [FREX](https://github.com/grondag/frex).

Discord: https://discord.gg/7NaqR2e
Curse: https://www.curseforge.com/minecraft/mc-mods/canvas-renderer

## License
Except as noted in individual source files, all code in this mod, include shaders, is [licensed under the LGPL-3.0 License](https://www.gnu.org/licenses/lgpl-3.0.en.html). This means no warranty is provided.

Some elements of code are adapted from or copied from other projects with compatible licensing.  The author has attempted to provide credit and/or appropriate notices in the code where applicable.

## Limitations
Canvas is in EARLY ALPHA.  Expect it to break.

The FREX extensions, shader library, vertex formats, attribute bindings, and lighting options are subject to change - causing your code to break.  Sorry.  When there is a stable release I will avoid breaking changes in shipping versions.  Until then, experimentation is the norm.

## Why
When people first hear about Canvas they often ask if it is a performance mod or a replacement for Optifine / shader packs.  The answer is "no, but..."

Optifine and shader packs primarily target vanilla Minecraft.  They work with modded, often well, but they aren't designed as tools for *mod authors*.

Canvas' main purpose is to give mod authors more control and options for rendering modded blocks.  It can also be used for building shader packs, but the design is entirely different than OF and does not yet support all the features needed for a full shader pack implementation.  Unlike OF shader packs, Canvas shader packs can be mixed together by adding multiple resource packs. 

## Performance
Performance-wise, Canvas tries to be be faster than Vanilla with extended features. It is optimized heavily - but the intent of these changes is to make better rendering practical, not to be a general-purpose performance mod. It isn't meant to run on low-end hardware and may or may not make your game run faster overall.

Canvas will try to fully use your hardware and will not be timid about it. It wants at least 4GB and will push both your CPU and GPU.  It will stress your cooling system.

If you're looking to max performance with Canvas, the config menu tool tips indicate which features can help.  Bloom is especially expensive at high resolutions.  But bloom is also fun to look at, so.... your call. 

More optimizations will be added after a stable release.

# Using Canvas

## Installing Canvas
Add Canvas to the `mods` folder in your minecraft folder (`%appdata%/.minecraft/mods` on Windows) and make sure you have recent versions of [Fabric](https://fabricmc.net/) Loader and API, plus at least 4GB of memory allocated to Minecraft.  An in-game config menu is available in video options, or via Mod Menu if you have it installed.

## Compatible Shader Packs

Third-party pipeline shaders:
* [Lumi Lights](https://spiralhalo.github.io/)
* [Forget-me-not](https://github.com/Poisoned-Honey/ForgetMeNot-Shaders)
* [lomo (wip)](https://github.com/fewizz/lomo/)

This list is updated infrequently.

More releases can be found in [`#canvas-3rd-party-releases` channel](https://discord.com/channels/614624415631671316/752632870257950790) on the [discord server](https://discord.gg/7NaqR2e).

# Developing With Canvas
Before using Canvas, you should first understand RenderMaterials, Meshes, RenderContexts and other features defined by the Fabric Rendering API.  For that information, consult the [rendering article on the Fabric Wiki](https://fabricmc.net/wiki/rendering). Note: Fabric wiki is still WIP as of this writing but should be more complete "soon."

You can also see [RenderBender](https://github.com/grondag/renderbender) for some (not very good) examples of usage.  Avoid duplicating those examples directly - they aren't especially performant or suitable for use at scale.  As soon as someone releases a model loader / library for Fabric Rendering API / FREX, that will almost certainly be a better approach.  

## Attaching Shaders to Materials

Shaders can be attached to materials via json, which is the preferred way. Your materials are located in `materials/` within your namespaced resource location.

### assets/example/materials/test_material.json
```json
{
	"vertexSource": "example:shaders/test.vert",
	"fragmentSource": "example:shaders/test.frag"
}
```

The paths in `vertexSource` and `fragmentSource` should point to GLSL files in your resources location.  The relative path and file extension must be included, and `shaders/` is the suggested location but not mandatory.

Afterwards, you can map a blockstate (or entity, particle, etc) to your material using a material map located in `materialmaps/`, like so:

### assets/example/materialmaps/block/test_block.json
```json
{
  "defaultMaterial": "example:test_material"
}
```

You can also load the material directly into java

```java
Renderer renderer = Renderer.get();

// obtain the loaded material, done after resource reload
RenderMaterial mat = renderer.materials().materialFromId(new ResourceLocation("example:test_material"));
```

Note that loading materials directly is only necessary for rendering custom meshes. For full blocks, entities, particles, or fluids, using material maps is the recommended way.

Alternatively, materials can also be created directly in java, like so:

```java
Renderer renderer = Renderer.get();

RenderMaterial mat = renderer.materials().materialFinder().shader(
		new ResourceLocation("example", "shaders/test.vert"),
		new ResourceLocation("example", "shaders/test.frag")
	).find();
```

This can be more reliable in case you don't want your materials to be affected by resource packs or resource reload. In this case, material maps can't be utilized.

## Writing Material Shaders

Your vertex and fragment shaders must have a `frx_materialVertex` and `frx_materialFragment` procedures respectively. To ensure compatibility, shaders are limited to `#version 330` features.

A detailed documentation of the available API can be found in the FREX source files:
* [FREX Shader API Overview](https://github.com/vram-guild/frex/blob/1.18/common/src/main/resources/assets/frex/shaders/api/FREX%20Shader%20API.md)
* [ FREX Shader API Header Files](https://github.com/vram-guild/frex/tree/1.18/common/src/main/resources/assets/frex/shaders/api)

## Adding Canvas to your project
Add these maven repos to your build if not already present

```gradle
repositories {
	// vram guild repo
	maven {url "https://maven.vram.io"}
	// cloth config
	maven {url "https://maven.shedaniel.me/"}
	// mod menu
	maven {url "https://maven.terraformersmc.com/releases/"}
}
```

And add Canvas to your dependencies

```gradle
dependencies {
	modCompileOnly "io.vram:canvas-fabric-mc118:1.0.+"

	// optional for testing in dev environment
	modRuntimeOnly "io.vram:canvas-fabric-mc118:1.0.+"
}
```

Note that versions are subject to change - look at the repo to find latest.

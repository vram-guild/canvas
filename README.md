# Canvas
Canvas is a shader-based Renderer for the [Fabric](https://fabricmc.net) modding toolchain.  It supports all features of the proposed [Fabric Rendering API](https://github.com/FabricMC/fabric/pull/65) plus extensions defined in [FREX](https://github.com/grondag/frex).

## License
Except as noted in individual source files, all code in this mod, include shaders, is [licensed under the Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0). This means no warranty is provided.

Some elements of code are adapted from or copied from other projects with compatible licensing.  The author has attempted to provide credit and/or appropriate notices in the code where applicable.

## Limitations
Canvas is in EARLY ALPHA.  Expect it to break.  Currently terrain rendering works with shaders but item rendering does not. (It will.). Rendering for blocks in movement (falling blocks, blocks being moved by pistons, etc.) is WIP.  

The FREX extensions, shader library, vertex formats, attribute bindings, and lighting options are subject to change - causing your code to break.  Sorry.  When there is a stable release I will avoid breaking changes in shipping versions.  Until then, experimentation is the norm.

## Why
When people first hear about Canvas they often ask if it is a performance mod or a replacement for Optifine / shader packs.  The answer is "no, but..."

Optifine and shader packs primarily target vanilla Minecraft.  They work with modded, often well, but they aren't designed as tools for *mod authors*.

Canvas' entire purpose is to give mod authors more control and options for rendering modded blocks.  It *could* be used for building a shader pack by replacing vanilla models with material-aware models that have custom shaders.  But that isn't its main reason for being.

Performance-wise, Canvas does try to be reasonably fast and has/will have optimizations - but the intent of these changes is to make better rendering practical, not to be a general-purpose performance mod. It isn't meant to run on low-end hardware and  may or may not make your game run faster overall.

Currently, there are two main optimizations.  1) Canvas buffers and renders SOLID and CUTOUT layers in a single pass with cutout and mip mapping variation handled in the fragment shader.  2) Canvas buffers and renders multiple chunks using the same matrix transform to reduce the number of GL state changes.

Additional optimizations will wait until after a stable release.

## Using Canvas
Before using Canvas, you should first understand RenderMaterials, Meshes, RenderContexts and other features defined by the Fabric Rendering API.  For that information, consult the [rendering article on the Fabric Wiki](https://fabricmc.net/wiki/rendering). Note: Fabric wiki is still WIP as of this writing but should be more complete "soon."

You can also see [RenderBender](https://github.com/grondag/renderbender) for some (not very good) examples of usage.  Avoid duplicating those examples directly - they aren't especially performant or suitable for use at scale.  As soon as someone releases a model loader / library for Fabric Rendering API / FREX, that will almost certainly be a better approach.  

### Overlay Sprites
Canvas supports a max sprite depth of three - meaning you can add one or two overlay sprites to each quad.  For example, to add one overlay texture, choose a material with a sprite depth of two:

```java
RenderMaterial mat = RendererAccess.INSTANCE.getRenderer().finder().spriteDepth(2)
//select other material properties...
.find();
```

Specify UV coordinates, blend mode and colors for your overlay sprites like so:
```java
  .blendMode(1, TRANSLUCENT)
  .disableColorIndex(1, true)
  .spriteBake(1, sprite, MutableQuadView.BAKE_LOCK_UV | MutableQuadView.BAKE_NORMALIZED)
  .spriteColor(1, -1, -1, -1, -1)
```

Note that is doesn't make sense to use `BlockRenderLayer.SOLID` as the blend mode for overlay textures - it would cover up the texture beneath it.  Use `TRANSLUCENT` or one of the `CUTOUT` modes instead.

It's likely Canvas will also support "decal" quads in the future, but overlay sprites will be more performant when model and texture geometry make them feasible. (Overlays can be rendered together in a single primitive and avoid the need to project the decal onto existing geometry.)

### Attaching Shaders to Materials
Shaders and their uniforms are bundled into a "pipeline" which can then be associated with a material, like so:

```java
  ExtendedRenderer er = (ExtendedRenderer) RendererAccess.INSTANCE.getRenderer();
  Pipeline p = er.pipelineBuilder()
      .vertexSource(new Identifier("renderbender", "shader/test.vert"))
      .fragmentSource(new Identifier("renderbender", "shader/test.frag"))
      .build();
  RenderMaterial mat = er.materialFinder().pipeline(p).find();
```

Note the renderer must be cast to `ExtendedRenderer` to access these features.  If you need custom uniforms, you can add them via `PipelineBuilder`.

The identifiers passed to `vertexSource` and `fragmentSource` should point to GLSL files in your resource pack.  The relative path and file extension must be included, and `shader/` is the suggested location.

Your vertex and fragment shaders must have a `main` procedure. To ensure compatibility, shaders are limited to `#version 120` features, plus `GL_EXT_gpu_shader4`.

## Vertex Shaders
Your vertex shader will automatically include all the definitions and library routines in `common_lib.glsl` and `vertex_lib.glsl`, which both live in `assets/canvas/shader`.

Your vertex shader must set `gl_Position`, `gl_ClipVertex`, and `gl_FogFragCoord` along with any `varying` variables needed in your fragment shader.  Canvas also needs to do its own prep for standard texturing and lighting here, assuming you need them. 

The easiest way to do this is to call the setupVertex() library function that Canvas provides, and then add your own logic as needed, like so:

```glsl
void main() {
    // do your custom stuff here!
    setupVertex();
    // or do it here!
}
```

## Fragment Shaders
Your fragment shader will automatically include all the definitions and library routines in `common_lib.glsl` and `fragment_lib.glsl`, which both live in `assets/canvas/shader`.

Canvas handles all lighting - diffuse and ambient occlusion - in the fragment shader.  This means your colors will always be unmodified at the start of the fragment routine.

Your fragment shader should set `gl_FragColor` or, in rare cases, call discard.  To get the lit and shaded color that would normally be output, call `diffuseColor()` and to apply the current distance fog use `fog()`.

Future versions of the fragment library will give more granular options for getting lit or unlit colors and for modifying colors before or after lighting.


## Adding Canvas to your project
Add these maven repos to your build if not already present

```gradle
repositories {
    // where grondag's mods live
    maven {
    	name = "dblsaiko"
    	url = "https://maven.dblsaiko.net/"
    }
    maven {
      name = "Cotton"
      url = "http://server.bbkr.space:8081/artifactory/libs-release/"
    }
    // REI, odds and ends
    maven {
      name = "CurseForge"
      url = "https://minecraft.curseforge.com/api/maven"
    }
}
```

And add Canvas to your dependencies

```gradle
dependencies {
	modCompile "grondag:canvas-mc116:1.0.+"
}


```

Note that versions are subject to change - look at the repo to find latest.

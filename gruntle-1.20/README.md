# Gruntle
_verb:_ The opposite of disgruntle.

## What is Gruntle
Gruntle is a set of shell scripts that maintain build configuration and dependencies across multiple branches and platforms for Minecraft mods that follow VRAM cross-platform mod conventions.

### Goals
* Minimal - simple to use and maintain
* Support multiple target platforms (Forge, Quilt, Fabric)
* Support multuple MC version branches
* Be usable by any contributor/mod author who can run shell scripts
* Ensure unambiguous dependency configuration - all dependency updates should result in a commit to the affected projects. (No wildcard dependencies.)

### Scope
Gruntle currently handles the following:
* Gradle distribution
* Gradle plug-in versioning
* Platform versions (Fabric loader, API, etc.)
* MC version
* JAR dependency versions (only VRAM deps are automatically updated)
* Update of VRAM standard gradle script and settings
* Update of VRAM standard license and headers

### Why Shell Scripts?
Shell scripts are simple and work practically everywhere.  While an approach using a gradle plug-in was briefly evaluated, it quickly became much more code and support for multiple platforms and version branches - each of which might have a different gradle version - seemed unworkable.  Gradle in general seems optimized for different use cases that can be represented as a multi-project build or for organizations that can afford to maintain extensive custom build tooling.

## Using Gruntle in a Mod

### Requirements
Gruntle is meant for mods that are maintained by VRAM or which can follow the same conventions. This means:
* Use of LGPL-3.0 with the standard VRAM license header. (The header does not mention VRAM so it should be fine if your project is OK with LGPL-3.0)
* Per-platform project structure (see next section)
* Separate branches per MC version. Support starts at 1.17 and branches should be named "1.17", "1.18", etc.
* Use of standard VRAM `build.gradle` and `settings.gradle` files.
* Mod dependencies and source sets must be defined in `project.gradle`
* `gradle.properties` has necessary tags (see below)
* Adoption of VRAM standard archive naming - `<mod>-<platform>-<mcver>-<version>`. For example: frex-fabric-mc117-6.0.101. The patch version is commit number and will be automatically determined.

Gruntle should _not_ be used to configure builds for plain java libraries that do not have dependencies on platform or minecraft version - those can be built using vanilla gradle and their build configuration tends to be relatively stable.

### VRAM Project Structure
Mod files must be organized within the project root as follows:

```
.\LICENCE                         LGPL 3.0
.\HEADER                          Standard VRAM LGPL header (not mod-specific)
.\checkstyle.xml                  Standard VRAM checkstyle config
.\gruntle.sh                      The gruntle update script
.\fabric\*                        Fabric-specific code and resources
.\fabric\gradle\                  Gradle distribution used by Fabric
.\fabric\build.gradle             Standard VRAM gradle script
.\fabric\settings.gradle          Standard VRAM
.\fabric\project.gradle           Mod-specific configuration - see below
.\fabric\gradle.properties        Mod-specific configuration - see below
.\forge\                          TBD
.\quilt\                          TBD
.\fabriquilt\*                   Code and resource common to Fabric and Quilt
.\common\*                        Code and resources common to all platforms
```

### Project Source and Dependencies: `project.gradle`
Minecraft and platforms versions will be handled by the standard configurations. All dependencies specific to the mod should be defined in `project.gradle`.

* Parenthetical syntax for dependencies is preferred.
* Use `project.loader_version` and `project.fabric_version` to declare fabric dependencies. They are automatically defined by the standard script.
* Use `modIncludeImplementation` on Fabric as shorthand to include implementation dependencies.

Examples:
```
modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
modCompileOnly(fabricApi.module("fabric-rendering-fluids-v1", project.fabric_version))
modIncludeImplementation("io.vram:bitkit:1.0.1")
```

The `project.gradle` file should also define the mod source sets. Custom source sets are often needed for cross-platform mods because source is in both common and platform-specific folder.  For example:

```
main {
		java {
			srcDirs = ['../common/src/main/java', 'src/main/java', '../fabriquilt/src/main/java']
			include '**/*'
			exclude '*.DS_Store'
		}
		resources {
			srcDirs = ['../common/src/main/resources', 'src/main/resources']
			include '**/*'
			exclude '*.DS_Store'
		}
}
```

### Project Configuration: `gradle.properties`
The gradle properties file _must_ contain the following:

```properties
# maven group for publishing
group=io.vram
# used to form archive name
mod_name=themod
# Fabric version - omit for non-Fabric or if no access widener needed
accesswidener=src/main/resources/themod.accesswidener

# Provide if needed for nested builds
# If omitted will default to what is shown here
license_file=../HEADER
checkstyle_config=../checkstyle.xml

# Build automation support
github_repository_owner=vram-guild
github_repository=frex
curseforge_id=123456
release_type=release

# Update major/minor version as needed per Semantic Versioning conventions.
# Patch version should always be omitted - it will be computed from commit number
mod_version=6.0
```

### Usage
With the correct structure in place, running `./gruntle.sh` from the project root folder will update all of the in-scope build configuration in scope. (See above.) It will _not_ commit any resulting updates.

Note that it is necessary to update `./gruntle.sh` the first time when a project is created. It will self-update afterwards.  Also note that this file is specific to Minecraft version and must be copied from the correct version subfolder of the gruntle repo.

Gruntle requires a unix-like shell. (There is no `.BAT` version of gruntle.) On Windows, use Powershell.

## Maintaining Gruntle
Gruntle is straightforward and there is little code. Most of it should be self-explanatory.  That said, some key points:

* There is no branching per Minecraft version. Each MC version gets its own folder.  This is done because many changes (like gradle or plugin updates) will span MC version and having all MC versions in the same tree makes this less tedious and error-prone.
* There are effectively three separate build configurations per MC version - one for each platform.  The gruntle script handles all three but they should be kept isolated from each other - the platforms generally don't coordinate and should be expected to have unique requirements.
* Version numbers for dependencies not located on the maven.vram.io repo must be manually updated.  This is by design - naming and stability of outside dependencies benefits from some human review before projects incorporate them.

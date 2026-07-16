Provides syntax highlighting for GlobeScript (the scripting language used by [Big Globe](https://github.com/Builderb0y/BigGlobe)).

# Usage instructions:

1. Download and install [intellij](https://www.jetbrains.com/idea/download)
2. Get the GlobeScript plugin zip file from the releases section here on Github.
3. Open intellij. Navigate to `Settings > Plugins`, click the gear button at the top, and select "Install Plugin from Disk".
4. Select the GlobeScript zip file you downloaded.
5. Create a new, empty project.
6. Create the following directory structure:
```
gs_env
src
	(data pack name)
		resources
			data
			pack.mcmeta
```
7. Populate the contents of the gs_env folder from this Github page. You can download the repository as a zip for this, or use the `git clone` command, or whatever suits your needs.
8. Optional: run the `/bigglobe:dumpRegistries full` command in-game (requires Big Globe V6.1.2 or later to be in the correct format), and copy or move the `data` folder it outputs to `gs_env/provided/data`. This will allow you to make data packs that have a dependency on Big Globe's default data files.

The src folder may contain more than one data pack. This will allow you to develop multiple data packs at once.

If all goes well, you should see syntax highlighting for some scripts, along with some very basic analysis and error checking.

# This plugin is in pre-alpha.

It has bugs. It may not have 1:1 parity with Big Globe's script parser. It definitely has missing functionality. Please report anything that doesn't work as expected either here on Github, or on my [Discord server](https://discord.gg/ucR5K6XNNP).

You may need to periodically update your gs_env folder from Github when new versions are released and bug fixes are made. This process is not automatic.
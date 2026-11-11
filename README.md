Provides syntax highlighting for GlobeScript (the scripting language used by [Big Globe](https://github.com/Builderb0y/BigGlobe)).

# Usage instructions:

1. Download and install [intellij](https://www.jetbrains.com/idea/download)
2. Get the GlobeScript plugin zip file from the releases section here on Github.
3. Open intellij. Navigate to `Settings > Plugins`, click the gear button at the top, and select "Install Plugin from Disk".
4. Select the GlobeScript zip file you downloaded.
5. Create a new, empty project.
6. Populate the gs_env folder in your project with the contents of the gs_env folder on this Github page. You can download the repository as a zip for this, or use the `git clone` command, or whatever suits your needs.
7. Edit `gs_env/packs.json5` to suit your needs. The schema for it is:
```
{
	"packs": [
		{
			"name": The name of the pack. This is used by other packs which depend on it. 
			"data_folder": The location of the "data" folder of this pack, relative to the project directory.
			"dependencies": [
				(a dependency): The name of another pack in the "packs" array which must be installed in order for this pack to function properly.
				Bi-directional dependencies (ie, when pack A depends on pack B, and pack B depends on pack A) are fully supported.
				Bi-directional dependencies imply that both packs must be installed in order for either pack to work correctly.
			]
		}
	]
}
```
The provided `packs.json5` file matches the folder structure of Big Globe, and can be used as an example. Your pack structure however will likely be different, hence why you will need to edit this file.

8. Optional: run the `/bigglobe:dumpRegistries full` command in-game (requires Big Globe V6.1.2 or later to be in the correct format), and copy or move the `data` folder it outputs to somewhere in your project, and add a dependency on it as explained above. This will allow you to reference Big Globe's built-in files in your pack.

You may have more than one data pack in your project.

If all goes well, you should see syntax highlighting for some scripts, along with some very basic analysis and error checking.

# This plugin is in pre-alpha.

It has bugs. It may not have 1:1 parity with Big Globe's script parser. It definitely has missing functionality. Please report anything that doesn't work as expected either here on Github, or on my [Discord server](https://discord.gg/ucR5K6XNNP).

You may need to periodically update your gs_env folder from Github when new versions are released and bug fixes are made. This process is not automatic.
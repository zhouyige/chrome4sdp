# Customizing Using Channels
"Channels" is an easy method to customize Snapdragon Web Engine. A channel is a folder in the build tree that enables you to:

1. Customize branding
2. Specify Web Refiner configuration
3. Enable and customize features and their parameters

Information present in the Channels folder override default settings of Snapdragon Web Engine

## Channels Directory
A Channel directory is present in:
*browser_root*/swe/channels/*channel_name*

### Channel Directory Structure
    `-- <channel_name>
        |-- branding
        |   `-- BRANDING
        `-- res
            |-- mipmap-xhdpi
            |   `-- app_icon.png
            |-- mipmap-xxhdpi
            |   `-- app_icon.png
            |-- mipmap-xxxhdpi
            |   `-- app_icon.png
            |-- mipmap-mdpi
            |   `-- app_icon.png
            |-- mipmap-hdpi
            |   `-- app_icon.png
            |-- values
            |   `-- strings.xml
            |-- values-zh-rCN
            |   `-- strings.xml
            |-- raw
            |   |-- swe_command_line
            |   |-- bookmarks_preload
            |   `-- search_engines_preload
            `-- raw-zh-rCN
                |-- swe_command_line
                |-- bookmarks_preload
                `-- search_engines_preload

All folders and files under *channel_name* are optional that causes SWE to use defaults for that setting.

Locale specific files are present in "values-_locale_" or "raw-_locale_". In our example, _locale_ is set to `zh-rCN`

### strings.xml
The file "strings.xml" contains all the resource strings and values that will be overridden. Please note that the name of this file can be anything.

## Building with Channels
To build with Channels, specify the channel names during the "runhooks" step as listed in [Sync & Build Instructions](sync_build.md).

## Updating the icons
Replace the following icons to complete the branding of the product:
	`-- <channel_name>
		|-- res
		    |-- mipmap-xhdpi
		    |   `-- app_icon.png - 144x144px
		    |-- drawable-xxhdpi
		    |   `-- fre_product_logo.png - 390x390px
		    |   `-- ic_chrome.png (general notifications) - 72x72px
		    |   `-- img_notify_update.png (update notifications if applicable) - 48x75px
		    |   `-- toolbar_shadow_normal.png - 24x12px
			Note: Add some transparent padding to the icons. Refer icons in *browser_root*/chrome/android/java/res_swe/drawable-xxhdpi

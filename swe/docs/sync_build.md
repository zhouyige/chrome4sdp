# Sync & Build Instructions
## Recommended Build System
* 64-bit machine
* 4GB or higher RAM
* 8GB or higher swap space
* 100GB or higher free hard-disk space
* 64-bit Ubuntu 12.04

## Chromium Builds
Instructions provided in this page are similar to Chromium's build
instructions at:
<https://www.chromium.org/developers/how-tos/android-build-instructions>

## One-time Environment Setup
### Update Linux packages

    $ sudo apt-get update

### Setup GIT

    $ sudo apt-get install git-core

### Setup depot_tools
1. Get depot_tools

        $ cd ~
        $ git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git

2. Add depot_tools to ~/.bashrc *(optional)*

        export PATH=$PATH:$HOME/depot_tools

### Setup GCC
Ensure gcc is set to 4.6 or higher

    $ sudo update-alternatives --config gcc

Select 'gcc-4.6' from the list

If 'gcc-4.6' is not listed, please install with:

    $ sudo apt-get -y install build-essential

### Setup JDK
Ensure that JDK is set to JDK 7

    $ sudo update-alternatives --config java

It is enough to check if 'java-7-openjdk-amd64' is listed for now since the build process sets this automatically.

If 'java-7-openjdk-amd64' is not listed then install it:

    $ sudo apt-get -y install openjdk-7-jre openjdk-7-jdk

## For China *(optional)*: Sync from APAC mirrors
To speed up sync in China, you may sync the sources from a mirror located in China at:
<https://www.codeaurora.org/xwiki/bin/Support/CodeAuroraMirrors>

## Before Sync
* Ensure you are using the bash shell

* Create a folder to sync the branch into. This folder is referred to as *browser_root* in rest of this section.

* If depot_tools was not added to ~/.bashrc during [One-time Environment Setup](#one-time-environment-setup) then update PATH now:

        PATH=$PATH:$HOME/depot_tools

## Create .gclient file
In the folder *browser_root* create a file named '.gclient' with the following content:

        solutions = [
            {   "name"        : "src",
                "url"         : "git://codeaurora.org/quic/chrome4sdp/chromium/src.git@refs/remotes/origin/$branch_name",
                "deps_file"   : "DEPS",
                "managed"     : False,
                "safesync_url": "",
            },
        ]
        target_os = ["android"]

NOTE: $branch_name will contain the branch name of the source tree e.g. master, m46 , m52 etc

## Sync Code
From *browser_root* folder:

    $ GYP_CHROMIUM_NO_ACTION=1 gclient sync -v -r refs/remotes/origin/$branch_name

GYP_CHROMIUM_NO_ACTION=1 allows gclient runhooks to run without gyp_chromium.

$branch_name in the command above should be replaced with actual branch name of the source tree e.g. master, m46 etc

## Build Code
* Install build dependencies from *browser_root* folder:

        $ cd src
        $ ./build/install-build-deps-android.sh

  The script 'install-build-depds-android.sh sets 'java-7-openjdk-amd64' as default

* Setup environment variables from *browser_root*/src

        $ . build/android/envsetup.sh

* Run hooks from *browser_root*/src

    * Default configuration

            $ GYP_CHROMIUM_NO_ACTION=1 gclient runhooks -v

* Generate GN args from *browser_root*/src:

    * Default configuration

            $ GYP_CHROMIUM_NO_ACTION=1 gn gen out/Release --args='target_os="android" is_debug=false symbol_level=0'

      OR

    * Configuration overridden using Channels

            $ GYP_CHROMIUM_NO_ACTION=1 gn gen out/Release --args='swe_channels="$channel_name" target_os="android" is_debug=false symbol_level=0'

      $channel_name in the command above should be replaced with actual channel name

  For more information about using Channels and overlay.xml, please refer to [Customizing using Channels](channels.md)

* Build APK

      $ ninja -C out/Release swe_browser_apk

  Building the APK also makes an AOSP Integration build, which includes Android.mk required to integrate the browser in AOSP.

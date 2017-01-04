# Secure Connect rulesets
This feature improves browser security by connecting to HTTPS sites. The feature relies on rules that map HTTP URLs to their HTTPS counterparts. This document describes how to compile the rulesets and provide them to the browser.

## Feature Availability
The feature is available in browser, by default. It can be removed by adding "--disable-secure-connect" to the browser command line file.

## Default User Setting
The user can enable or disable the feature, if it has not been removed (by using "--disable-secure-connect"). This setting can be accessed from the browser settings. The starting user setting can be configured by specifying "--secure-connect-default=true|false" command line flag. The default user setting is ON if the command line flag is not used.

## Rulesets
The rulesets are sourced from popular extensions for desktop browsers which achieves similar functionality. The extension and ruleset files can be located by doing an internet search. The rulesets are in the form of XML files.

We provide scripts that compiles the XML ruleset files into a data file. The browser uses this data file to generate the HTTPS mappings. The scripts are packaged in git://webtech-devx.qualcomm.com/swe/packages/web-refiner.git project. The scripts are located in GIT branch corresponding to the browser version. "generate_secure_connect_rulesets.py" generates the data file from the XML files. "validate_secure_connect_rulesets.py" can be used to validate the generated data file.

### Hosting Ruleset File
The browser downloads the ruleset data file on first launch. It also periodically checks for updated ruleset data. The ruleset files is required to be hosted on a public site. The versioning of the ruleset is controlled via a JSON file. The JSON file is also hosted on a public URL.

Following is the JSON file format:
    {
        "versioncode": <an integer value, that is incremented on every new revision of the ruleset file>,
        "download-url": <URL where the ruleset data file is hosted>,
        "interval": <minimum time (in seconds) interval for subsequent browser checks for updated ruleset file>,
        "versionstring": <string format of version. It's for display and logging only>
        "md5": <MD5 checksum of the ruleset data file hosted on download-url>
    }

The URL location of the JSON file is configured in browser via the channel configuration. "swe_secure_connect_ruleset_url" Android string resource value should contain the URL location.

## Prompt secure content for networks
This feature prompts the user to enable secure content only mode when connected to a new wifi network and stores the preference.
The feature is available in browser, by default. It can be removed by adding "--disable-secure-content-network" to the browser command line file.
Maximum count for network specific preference storage can be configured by overriding secure_content_network_max_count parameter in *browser_root*/swe/channels/*channel_name*/values/strings.xml.

For more information about using Channels and strings.xml, please refer to [Customizing using Channels](channels.md)

| Definition | Default Value | Description |
|---|---|---|
| `<integer name="secure_content_network_max_count"></integer>` | `50` |  Maximum count for network specific secure content preference storage |

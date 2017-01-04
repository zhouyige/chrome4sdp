# Custom User Agent
This feature enables partners to specify to specify a custom User Agent string. A User Agent string is used to identify the browser.

## Enable Condition
This feature is enabled when value of `swe_def_ua_override` is not empty.

## Configuration
The following configuration parameters are available. These parameters can be customized by overriding them in the following file:
*browser_root*/swe/channels/*channel_name*/values/strings.xml

For more information about using Channels and strings.xml, please refer to [Customizing using Channels](channels.md)

| Definition | Default Value | Description |
|---|---|---|
| `<string name="swe_def_ua_override" translatable="false" tools:ignore="UnusedResources"></string>` | (Empty string) | User Agent string to use |

### User Agent String
Example of `swe_def_ua_override`:

    <string name="swe_def_ua_override" formatted="false">
        <![CDATA[<%build_model>/1.0 Android <%build_version> Release/<%build_id> Browser/AppleWebKit534.30 Profile/MIDP-2.0 Configuration/CLDC-1.1]]>
    </string>

#### User Agent String Variables
The User Agent String supports the following variables. The values of these variables are substituted with system properties.

| Variable | Value |
|---|---|
| `<%build_model>` | `Build.MODEL` |
| `<%build_version>` | `Build.VERSION.RELEASE` |
| `<%build_id>` | `Build.ID` |
| `<%language>` | `Locale.getDefault().getLanguage()` |
| `<%country>` | `Locale.getDefault().getCountry()` |

# E-Store Protocol Support
Using the E-Store Protocol, websites can make SWE start an installed Android Application. If the desired Android Application is not installed, SWE will navigate to an E-Store home page and show a notification toast informing the user that the application is not installed.

E-Store URL of type `estore://appname`

## Enable Condition
This feature is enabled when the following parameter values are not empty:

* `swe_estore_homepage`
* `swe_download_estore_app`

## Configuration
The following configuration parameters are available. These parameters can be customized by overriding them in the following file:
*browser_root*/swe/channels/*channel_name*/values/strings.xml

For more information about using Channels and strings.xml, please refer to [Customizing using Channels](channels.md)

| Definition | Default Value | Description |
|---|---|---|
| `<string name="swe_estore_homepage" translatable="false" tools:ignore="UnusedResources"></string>` | (Empty) | SWE navigates to this home page if E-Store Application is not installed |
| `<string name="swe_download_estore_app" tools:ignore="UnusedResources"></string>` | Estore app does not exist, please download and install it. | SWE shows this message in a notification toast if the E-Store Application is not installed |

## Handling E-Store Links
To handle e-store links, an application capable of handling `estore://` URI scheme must be available on the target. Writing such an application is outside the scope of this document.

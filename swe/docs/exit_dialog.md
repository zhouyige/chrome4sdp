# Exit Dialog
When enabled, this feature adds an 'Exit' menu item and shows an Exit Dialog when:
1. User taps "Back" on the navigation bar and:
   a. Under all conditions where Chromium is minimized
   b. There are no tabs open
2. User taps "Exit" from the application menu

## Enable Condition
This feature is enabled when `swe_feature_exit_dialog` is set to `true`.

## Configuration
The following configuration parameters are available. These parameters can be customized by overriding them in the following file:
*browser_root*/swe/channels/*channel_name*/values/strings.xml

For more information about using Channels and strings.xml, please refer to [Customizing using Channels](channels.md)

| Definition | Default Value | Description |
|---|---|---|
| `<bool name="swe_feature_exit_dialog"></bool>` | `false` | Set to `true` to add an Exit menu item and show an exit dialog when user selects the Exit menu or closes the last remaining tab. |
| `<string name="swe_exit"></string>` | "Exit" | String used for the menu item |
| `<string name="swe_exit_dialog_title"></string>` | "Exit browser" | String used as title of the Exit Dialog |
| `<string name="swe_exit_dialog_msg"></string>` | "Do you want to exit the browser?" | String used as the dialog's body |
| `<string name="swe_minimize"></string>` | "Minimize" | String used for the Minimize button. Selecting this button makes the browser move to the background. |
| `<string name="swe_quit"></string>` | "Quit" | String used for the Quit button. Selecting this button Quits the browser. |


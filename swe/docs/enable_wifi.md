# Enable WiFi
If enabled, the browser checks the WiFi state when the first page finishes loading and starts an intent based on the WiFi state.

The browser starts a "Data Setting Intent" when:

1. WiFi is enabled but not connected
2. No WiFi networks are available

The browser starts a "WiFi Selection Intent" when:

1. WiFi is enabled but not connected
2. At least 1 WiFi network is available

Data Setting Intent and WiFi Selection Intent are configurable. Partners can use these intents to start an activity and take appropriate action.

## Enable Condition
This feature is enabled when:

* `swe_feature_network_notifier` is `true`
* `swe_def_action_wifi_selection_data_connections` is not empty
* `swe_def_intent_pick_network` is not empty

## Configuration
The following configuration parameters are available. These parameters can be customized by overriding them in the following file:
*browser_root*/swe/channels/*channel_name*/values/strings.xml

For more information about using Channels and strings.xml, please refer to [Customizing using Channels](channels.md)

| Definition | Default Value | Description |
|---|---|---|
| `<bool name="swe_feature_network_notifier"></bool>` | `false` | Set to `true` to enable the feature |
| `<string name="swe_def_action_wifi_selection_data_connections" translatable="false"></string> ` | (Empty string) | Intent string to start "Data Setting Intent" |
| `<string name="swe_def_intent_pick_network" translatable="false"></string>` | (Empty string) | Intent string to start "WiFi Selection Intent" |
| `<string name="swe_action_not_found"></string>` | "Action Not Found" | Toast message to display when an Activity with provided Intent failed to start. |
| `<string name="swe_wifi_select_message"></string>` | "Wifi Detected!\n Please Select Wifi Access Point for the Browser" | Toast message to display before "WiFi Selection Intent" is started. |

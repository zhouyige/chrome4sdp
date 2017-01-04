# Custom Homepage
This feature enables partners to set a custom home page for the browser. It also supports an option to load the NTP(New Tab Page) when there are no tabs to restore even if a homepage is set.

## Enable Condition
The Custom Homepage feature is enabled when the following parameter value is not empty:

* `default_homepage_url`

The NTP Landing Page feature is enabled when the following parameter value is set to true:

* `swe_feature_force_ntp_landing_page`

## Configuration
The following configuration parameters are available. These parameters can be customized by overriding them in the following files:
*browser_root*/swe/channels/*channel_name*/values/strings.xml
*browser_root*/swe/channels/*channel_name*/values/values.xml

For more information about using Channels and strings.xml, please refer to [Customizing using Channels](channels.md)

| Definition | File | Default Value | Description |
|---|---|---|---|
| `<string name="default_homepage_url" translatable="false">""</string>` | *browser_root*/swe/channels/*channel_name*/values/strings.xml | (Empty) | Default homepage URL |
| `<bool name="swe_feature_force_ntp_landing_page">True</bool>`          | *browser_root*/swe/channels/*channel_name*/values/values.xml  | (false) | Force load the New Tab Page for the first time |

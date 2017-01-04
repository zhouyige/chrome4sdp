# Restrict DRM File Upload
When enabled, this feature restricts users to upload DRM files.

Files with the following extensions are restricted: `.fl`, `.dm`, `.dcf`, `.dr` and `.dd`

## Enable Condition
This feature is enabled when value of parameter `swe_feature_drm_upload` is set to `true`

## Configuration
The following configuration parameters are available. These parameters can be customized by overriding them in the following file:
*browser_root*/swe/channels/*channel_name*/values/overlay.xml

For more information about using Channels and overlay.xml, please refer to [Customizing using Channels](channels.md)

| Definition | Default Value | Description |
|---|---|---|
| `<bool name="swe_feature_drm_upload" tools:ignore="UnusedResources"></bool>` | (Not in file) | Set to `true` to restrict upload and `false` otherwise |
| `<string name="swe_drm_file_unsupported" tools:ignore="UnusedResources"></string>` | Uploading DRM files is not supported! | Message to display if `swe_feature_drm_upload` is set to true and user attempts to upload a DRM file from Select File Dialog |

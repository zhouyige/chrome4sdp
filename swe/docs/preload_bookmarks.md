# Preload Bookmarks
This feature enables partners to specify a set of Bookmarks and Bookmark Folders to preload.

## Enable Condition
The feature is enabled when the value of parameter `swe_force_enable_partner_customization` is set to `true`.

## Configuration
The following configuration parameters are available. These parameters can be customized by overriding them in the following file:
*browser_root*/swe/channels/*channel_name*/values/strings.xml

For more information about using Channels and strings.xml, please refer to [Customizing using Channels](channels.md)

| Definition | Default Value | Description |
|---|---|---|
| `<bool name="swe_force_enable_partner_customization"></bool>` | true | Set to `true` to preload bookmarks |
| `<integer name="disablebookmarksediting"></integer>` | 0 | Set to `1` to permit users to edit preloaded bookmarks |

## Preload Bookmark URLs
Preload Bookmark URLs are loaded from the following file:
*browser_root*/swe/channels/*channel_name*/raw/bookmarks_preload

### Format
The file **bookmarks_preload** is a JSON file that carry Bookmarks, Bookmark Folders and other Bookmark properties.

Consider the following bookmarks to preload:

    `-- TopLevelBookmarkFolder
        |-- TopLevelBookmark1
        |-- TopLevelBookmark2
        |-- BookmarkFolder
        |   |-- Bookmark1
        |   `-- Bookmark2
        `-- TopLevelBookmark3

The file **bookmarks_preload** will look like this:

    {
        "version": "1.0"
        "title": "TopLevelBookmarkFolder"
        "bookmarks": [{
            "title": "TopLevelBookmark1",
            "url": "www.toplevelbookmark1.com",
            "favicon": "data.image/png;base64,...",
            "touchicon": "data.image/png;base64,...",
        }, {
            "title": "TopLevelBookmark2",
            "url": "www.toplevelbookmark2.com",
            "favicon": "data.image/png;base64,...",
            "touchicon": "data.image/png;base64,...",
        }, {
            "title": "BookmarkFolder",
            "bookmarks": [{
                "title": "Bookmark1",
                "url": "www.bookmark1.com",
                "favicon": "data.image/png;base64,...",
                "touchicon": "data.image/png;base64,...",
            }, {
                "title": "Bookmark2",
                "url": "www.bookmark2.com",
                "favicon": "data.image/png;base64,...",
                "touchicon": "data.image/png;base64,...",
            }]
        }, {
            "title": "TopLevelBookmark3",
            "url": "www.toplevelbookmark3.com",
            "favicon": "data.image/png;base64,...",
            "touchicon": "data.image/png;base64,...",
        }]
    }

#### Image Format
Each bookmark can have a `favicon` and a `touchicon`. 'ico' and 'png' are supported. The image data must be converted to Base64 and copied to **bookmarks_preload** in the following format:

    "favicon": "data.image/png;base64,..."

Where '...' is the Base64 image data.

For 'ico':

    "favicon": "data.image/ico;base64,..."

`touchicon` format is similar to `favicon`.

The image for both `favicon` & `touchicon` should be square (width = height) and the minimum acceptable size is 16x16.

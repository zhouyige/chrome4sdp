# Download Enhancements
When enabled:
1. Always prompt the user on file download
2. Automatically select a default folder based on the MIME Type of the download file
3. Users can select another folder from the prompt
4. Prevent download if download file's size as reported by 'Content-Length' header value is larger than storage space available in the selected folder.

## Enable Condition
The feature is enabled when the following parameter values are not empty:

* `swe_downloadpath_activity_intent`
* `swe_downloadpath_activity_result_selection`

## Description
When a user clicks on a hyperlink that is not another page, there are 2 possible actions:
1. If the browser recognizes the file as a media file, it is opened for streaming
2. If the browser does not recognize the file, it is downloaded

When this feature is enabled and when the user downloads from a hyperlink, a prompt is displayed with message specified by `swe_download_infobar_text`. The message contains the name of the download file and name of the Download To folder. Download To folder defaults to a folder based on download file's MIME Type as listed in the below table. The download to folder name is hyperlinked. On clicking the hyperlink, the browser starts an activity with intent specified by `swe_downloadpath_activity_intent`.

The activity is expected to return with the full path of the new Download To folder using the Intent class' extra string. The key name of the extra string that contains the download folder's full path is specified by `swe_downloadpath_activity_result_selection`.

### Default Download To Folders
| MIME Type | Folder                          |
|-----------|---------------------------------|
| audio     | Environment.DIRECTORY_MUSIC     |
| movies    | Environment.DIRECTORY_MOVIES    |
| image     | Environment.DIRECTORY_PICTURES  |
| (default) | Environment.DIRECTORY_DOWNLOADS |

To disable the sub-feature of Default Download To Folder on MIME type, set the following parameter to `false`:

* `swe_downloadpath_dir_on_mime`

### Configuration
The following configuration parameters are available. These parameters can be customized by overriding them in the following file:
*browser_root*/swe/channels/*channel_name*/values/strings.xml

For more information about using Channels and strings.xml, please refer to [Customizing using Channels](channels.md)

| Definition | Default Value | Description |
|---|---|---|
| `<string name="swe_downloadpath_not_enough_space"></string>` | %1$s - Not enough space for download | Prompt shown to the user when not enough space is available for download content. |
| `<string name="swe_download_infobar_text"></string>` | Do you want to download ^1 to ^2? | Prompt shown to user before downloading a file. ^1 is substituted with download file name. ^2 is replaced with the folder name and is hyperlinked. |
| `<string name="swe_downloadpath_activity_intent" translatable="false"></string>` | (Empty) | Intent string used to start an activity when user taps on download to folder hyperlink |
| `<string name="swe_downloadpath_activity_result_selection" translatable="false"></string>` | (Empty) | The key name used retrieve full path of download to folder using Intent.getExtraString() |
| `<bool name="swe_downloadpath_dir_on_mime"></bool>` | `true` | If set to `false`, disable sub-feature to set Default Download To folder on MIME type |

### Note
- Dir selection intent string looks like "com.android.fileexplorer.action.DIR_SEL"
- Dir selection result key looks like "def_file_manager_result_dir"

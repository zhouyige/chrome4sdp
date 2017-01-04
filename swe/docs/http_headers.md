# Additional HTTP Headers
This feature enables partners to specify additional HTTP Headers to send to requests.

## Enable Condition
This feature is enabled when the list of HTTP headers to send is not empty.

## Configuration
The following configuration parameters are available. These parameters can be customized by overriding them in the following file:
*browser_root*/swe/channels/*channel_name*/values/strings.xml

For more information about using Channels and strings.xml, please refer to [Customizing using Channels](channels.md)

| Definition | Default Value | Description |
|---|---|---|
| `<string name="swe_def_extra_http_headers" translatable="false" tools:ignore="UnusedResources"></string>` | (Empty string) | Key-Value pairs delimited by `\r\n` |

### Format of Key-Value Pairs
Format of `swe_def_extra_http_headers` is as follows:

    <string name="swe_def_extra_http_headers" translatable="false" tools:ignore="UnusedResources">
        <http_header_1>:<http_header_value_1>\r\n<http_header_2>:<http_header_value_2>
    </string>

For Example:

    <string name="def_extra_http_headers">
        x-wap-profile:http://www.qualcomm.com/uaprof/example.xml
    </string>
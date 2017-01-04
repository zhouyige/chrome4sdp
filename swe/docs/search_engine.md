# Custom Search Engine
This feature enables partners to specify a list of Search Engine URLs to preload.

## Enable Condition
The feature is enabled when a file named **search_engines_preload** is present in *browser_root*/swe/channels/*channel_name*/raw and is not empty.

## Search Engine URLs
Preload Search Engine URLs are loaded from the following file:
*browser_root*/swe/channels/*channel_name*/raw/search_engines_preload

### Format
The file **search_engines_preload** is a JSON file that carry a list of Search Engine URLs and index of the default search engine.

Consider the following Search Engine URLs to preload:

* www.google.com
* www.bing.com

The file **search_engines_preload** will look like this:

    {
        "version": "1.0",
        "searchEngines": [{
            "name": "Google",
            "keyword": "google.com",
            "searchUrl": "http://www.google.com/search?q={searchTerms}",
            "faviconUrl": "http://www.google.com/favicon.ico"
        }, {
            "name": "Bing",
            "keyword": "bing.com",
            "searchUrl": "http://www.bing.com/search?q={searchTerms}",
            "faviconUrl": "http://www.bing.com/s/favicon.ico"
        }],
        "default": 0
    }

`"default": 0` is a 0 based index of the Search Engine URL to select as default

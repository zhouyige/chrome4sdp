## Safe Browsing
**[Google's](https://www.google.com/) [Safe Browsing](https://developers.google.com/safe-browsing/)** service provides access to a **blacklist of websites** that have been reported as exhibiting known _malicious_ behavior. This behavior ranges from stealing user information or user credentials to phishing or even downloading malicious and otherwise unwanted software to your device by just visiting the website. This set includes both websites that have been compromised or hacked i.e. the websites original purpose is non-malicious and good to websites that have been created explicitly for malicious purposes.

TL;DR: The safe-browsing service is enabled by default. You will need to provision and provide a Safe Browsing API Key via the command line for the service to be activated for your users. You can use a command line flag to disable Safe Browsing in your build.

### Safe Browsing Enablement
Safe Browsing is automatically enabled for Snapdragon Web Engine builds. However, for the service to work for your end users, you will need to provision a *[Safe Browsing Service Key](https://developers.google.com/safe-browsing/key_signup)*.  Note that this is currently a _free service_ provided by *[Google](https://www.google.com)*. Once provisioned, you just need to provide the key to the browser via the command line flag "--google-apis-key=<your-provisioned-key>". By default, provisioned keys are allowed to make 10000 requests to the Safe Browsing service per day. Contact [Google](https://google.com) if you need to increase this limit.

  Option: --google-apis-key=<your-provisioned-key>

### Disable Safe Browsing
To disable Safe Browsing by default use the command line flag "--disable-safebrowsing". Please note that configuring the browser build with safe browsing turned off this way will make the Safe Browsing component inaccessible in the browser build. There is no way to dynamically turn safe browsing on once it has been disabled on the command line.

  Option: --disable-safebrowsing

### Disable database updates on cellular network
Safe Browsing works by converting the URL resources into a hash and then comparing this hash against a local database of malicious hashes. By default, the database updates at fixed intervals via both WiFi and Cellular Networks (whichever is available). To save cellular bandwidth, you can configure your browser build to disable database updates while on cellular and only update the database when connected to WiFi. To configure your browser build to disable safe browsing database updates while on a cellular network, use the command line flag "--disable-safebrowsing-cellular-update". Note that configuring the browser to be built with safe browsing DB updates on cellular disabled could result in sub-optimal security protection for the end user.

  Option: --disable-safebrowsing-cellular-update





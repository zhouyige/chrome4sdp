/*
 *  Copyright (c) 2015-2016, The Linux Foundation. All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 *      * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 *  ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 *  BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 *  BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 *  WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 *  OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 *  IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Portions of this file derived from Chromium code, which is BSD licensed, copyright The Chromium Authors.
 */

package org.chromium.chrome.browser;

import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;

import org.chromium.base.test.util.CallbackHelper;
import org.chromium.base.test.util.CommandLineFlags;
import org.chromium.base.test.util.Feature;
import org.chromium.chrome.browser.tab.EmptyTabObserver;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.test.ChromeTabbedActivityTestBase;
import org.chromium.content.browser.WebRefiner;
import org.chromium.content.browser.WebRefiner.RuleSet;
import org.chromium.content.browser.test.util.JavaScriptUtils;
import org.chromium.content_public.browser.LoadUrlParams;
import org.chromium.content_public.browser.WebContents;
import org.chromium.net.test.util.TestWebServer;
import org.chromium.ui.base.PageTransition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class WebRefinerTest extends ChromeTabbedActivityTestBase {
    private static final String LOGTAG = "WebRefinerTest";
    private static final int SHORT_WAIT_TIMEOUT = 1000;

    private ChromeActivity mActivity;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
    }

    @Override
    public void startMainActivity() throws InterruptedException {
        startMainActivityFromLauncher();
    }

    @MediumTest
    @Feature({"WebRefiner"})
    public void testInitialization() throws Exception {
        WebRefiner wbr = WebRefiner.getInstance();
        assertNotNull(wbr);
        assertTrue(WebRefiner.isInitialized());
    }

    private boolean writeToFile(String data, String fileName) {

        boolean result = true;
        try {
            File file = new File(mActivity.getApplicationInfo().dataDir, fileName);
            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                os.write(data.getBytes());
                os.flush();
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        } catch (Exception e) {
            Log.e(LOGTAG, e.getMessage());
            result = false;
        }

        return result;
    }

    private void loadUrlAndWaitForPageLoadCompletion(final String url) throws InterruptedException {
        loadUrlAndWaitForPageLoadCompletion(url, 0);
    }

    private void loadUrlAndWaitForPageLoadCompletion(final String url, int waitAfterLoadMs) throws InterruptedException {
        getInstrumentation().waitForIdleSync();
        Tab tab = getActivity().getActivityTab();
        final CallbackHelper loadedCallback = new CallbackHelper();
        final CallbackHelper failedCallback = new CallbackHelper();
        final CallbackHelper crashedCallback = new CallbackHelper();

        tab.addObserver(new EmptyTabObserver() {
            @Override
            public void onPageLoadFinished(Tab tab) {
                loadedCallback.notifyCalled();
            }

            @Override
            public void onPageLoadFailed(Tab tab, int errorCode) {
                failedCallback.notifyCalled();
            }

            @Override
            public void onCrash(Tab tab, boolean sadTabShown) {
                crashedCallback.notifyCalled();
            }
        });

        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Tab tab = getActivity().getActivityTab();
                int pageTransition = PageTransition.TYPED | PageTransition.FROM_ADDRESS_BAR;
                tab.loadUrl(new LoadUrlParams(url, pageTransition));
            }
        });

        boolean loaded = true;
        boolean failed = true;
        boolean crashed = true;
        try {
            loadedCallback.waitForCallback(0, 1, 20, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            loaded = false;
        }
        try {
            failedCallback.waitForCallback(0, 1, SHORT_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            failed = false;
        }
        try {
            crashedCallback.waitForCallback(0, 1, SHORT_WAIT_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            crashed = false;
        }

        assertTrue("Neither PAGE_LOAD_FINISHED nor a TAB_CRASHED event was received",
                loaded || failed || crashed);

        // Try to stop page load.
        getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                getActivity().getActivityTab().stopLoading();
            }
        });
        getInstrumentation().waitForIdleSync();

        if (waitAfterLoadMs > 0) {
            try {
                Thread.sleep(waitAfterLoadMs);
            } catch (InterruptedException e) {
            }
        }
    }

    private boolean waitForRuleSetToApply(WebRefiner wbr) {
        boolean result = true;
        try {
            Class noparams[] = {};
            Method method = wbr.getClass().getDeclaredMethod("ensurePendingRuleSetsApplied", noparams);
            method.invoke(wbr, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        }
        return result;
    }

    private String replaceHost(String srcUrlString, String host) throws MalformedURLException {
        final URL originalURL = new URL(srcUrlString);
        final URL newURL = new URL(originalURL.getProtocol(), host, originalURL.getPort(), originalURL.getFile());
        return newURL.toString();
    }

    private static final String TEST_PAGE_CONTENT = "<html>\n" +
            "<head>\n" +
            "    <script src=\"http://localhost/ad_script01.js\" type=\"text/javascript\"></script>\n" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost/ad_style01.css\">\n" +
            "</head>\n" +
            "<body>\n" +
            "    <p>WebRefiner test page.</p>\n" +
            "    <iframe src=\"http://localhost/ad_frame01.html\"></iframe>\n" +
            "    <iframe src=\"http://localhost/ad_frame02.html\"></iframe>\n" +
            "    <script src=\"http://localhost/ad_script02.js\" type=\"text/javascript\"></script>\n" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"http://localhost/ad_style02.css\">\n" +
            "    <img src=\"http://localhost/ad_img01.jpg\">\n" +
            "    <img src=\"http://localhost/ad_img02.png\">\n" +
            "</body>";

    private static final String RULE_SET_DATA = "ad_frame\n" +
            "ad_img\n" +
            "ad_style\n" +
            "ad_script\n";

    @MediumTest
    @Feature({"WebRefiner"})
    public void testRuleSet() throws Exception {

        TestWebServer webServer = TestWebServer.start();
        try {

            WebRefiner wbr = WebRefiner.getInstance();
            assertNotNull(wbr);

            String ruleSetFileName = "rule_set_01.rules";
            assertTrue(writeToFile(RULE_SET_DATA, ruleSetFileName));

            RuleSet rs = new RuleSet("TestFilters", new File(mActivity.getApplicationInfo().dataDir, ruleSetFileName).getAbsolutePath(), WebRefiner.RuleSet.CATEGORY_ADS, 1);
            wbr.addRuleSet(rs);

            assertTrue(waitForRuleSetToApply(wbr));

            final String urlString = webServer.setResponse("/webrefiner_test.html", TEST_PAGE_CONTENT, null);
            final URL url = new URL(urlString);
            final int expectedTotalURLs = 8/*subrequests*/ + 1/*main request*/;

            // Subtest 1
            {
                loadUrlAndWaitForPageLoadCompletion(urlString);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                int actualTotalURLs = wbr.getTotalURLCount(wc);
                // Sometimes favicon urls get mixed up with the page session, here we account for the
                // favicon url to be counted.
                assertTrue((actualTotalURLs >= expectedTotalURLs && actualTotalURLs <= (expectedTotalURLs + 1)));

                assertEquals(8, wbr.getBlockedURLCount(wc));

                WebRefiner.PageInfo pageInfo = wbr.getPageInfo(wc);
                assertNotNull(pageInfo);
                assertEquals(actualTotalURLs, pageInfo.mTotalUrls);
                assertEquals(8, pageInfo.mBlockedUrls);
                assertEquals(0, pageInfo.mWhiteListedUrls);
                assertEquals(pageInfo.mBlockedUrls + pageInfo.mWhiteListedUrls, pageInfo.mMatchedURLInfoList.length);

                int ads = 0;
                int trackers = 0;
                int malwares = 0;
                int images = 0;
                int scripts = 0;
                int stylesheets = 0;
                int subframes = 0;
                int whitelisted = 0;
                int blocked = 0;

                for (WebRefiner.MatchedURLInfo urlInfo : pageInfo.mMatchedURLInfoList) {
                    if (urlInfo.mActionTaken == WebRefiner.MatchedURLInfo.ACTION_BLOCKED) {
                        blocked++;
                        switch (urlInfo.mMatchedFilterCategory) {
                            case WebRefiner.RuleSet.CATEGORY_ADS:
                                ads++;
                                break;
                            case WebRefiner.RuleSet.CATEGORY_TRACKERS:
                                trackers++;
                                break;
                            case WebRefiner.RuleSet.CATEGORY_MALWARE_DOMAINS:
                                malwares++;
                                break;
                        }
                        if (0 == urlInfo.mType.compareTo("Image")) {
                            images++;
                        } else if (0 == urlInfo.mType.compareTo("Script")) {
                            scripts++;
                        } else if (0 == urlInfo.mType.compareTo("Stylesheet")) {
                            stylesheets++;
                        } else if (0 == urlInfo.mType.compareTo("SubFrame")) {
                            subframes++;
                        }
                    } else if (urlInfo.mActionTaken == WebRefiner.MatchedURLInfo.ACTION_WHITELISTED) {
                        whitelisted++;
                    }
                }

                assertEquals(8, ads);
                assertEquals(0, trackers);
                assertEquals(0, malwares);
                assertEquals(2, images);
                assertEquals(2, scripts);
                assertEquals(2, stylesheets);
                assertEquals(2, subframes);
                assertEquals(0, whitelisted);
                assertEquals(8, blocked);
            }

            // Subtest 2 - disable default
            {
                wbr.setDefaultPermission(false);

                loadUrlAndWaitForPageLoadCompletion(urlString);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                int actualTotalURLs = wbr.getTotalURLCount(wc);
                assertTrue((actualTotalURLs >= expectedTotalURLs && actualTotalURLs <= (expectedTotalURLs + 1)));

                assertEquals(0, wbr.getBlockedURLCount(wc));
                WebRefiner.PageInfo pageInfo = wbr.getPageInfo(wc);
                assertNotNull(pageInfo);
                assertEquals(actualTotalURLs, pageInfo.mTotalUrls);
                assertEquals(0, pageInfo.mBlockedUrls);
                assertEquals(0, pageInfo.mWhiteListedUrls);
                assertNotNull(pageInfo.mMatchedURLInfoList);
                assertEquals(pageInfo.mBlockedUrls + pageInfo.mWhiteListedUrls, pageInfo.mMatchedURLInfoList.length);
            }

            // Subtest 3 - Enable default
            {
                wbr.setDefaultPermission(true);

                loadUrlAndWaitForPageLoadCompletion(urlString);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                int actualTotalURLs = wbr.getTotalURLCount(wc);
                assertTrue((actualTotalURLs >= expectedTotalURLs && actualTotalURLs <= (expectedTotalURLs + 1)));

                assertEquals(8, wbr.getBlockedURLCount(wc));
                WebRefiner.PageInfo pageInfo = wbr.getPageInfo(wc);
                assertNotNull(pageInfo);
                assertEquals(actualTotalURLs, pageInfo.mTotalUrls);
                assertEquals(8, pageInfo.mBlockedUrls);
                assertEquals(0, pageInfo.mWhiteListedUrls);
                assertNotNull(pageInfo.mMatchedURLInfoList);
                assertEquals(pageInfo.mBlockedUrls + pageInfo.mWhiteListedUrls, pageInfo.mMatchedURLInfoList.length);
            }

            // Subtest 4 - enable default, but disable for origin
            {
                String origin = url.getProtocol() + "://" + url.getHost() + ":" + url.getPort() + "/";
                String[] origins = new String[1];
                origins[0] = origin;
                wbr.setPermissionForOrigins(origins, WebRefiner.PERMISSION_DISABLE, false);

                loadUrlAndWaitForPageLoadCompletion(urlString);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                int actualTotalURLs = wbr.getTotalURLCount(wc);
                assertTrue((actualTotalURLs >= expectedTotalURLs && actualTotalURLs <= (expectedTotalURLs + 1)));

                assertEquals(0, wbr.getBlockedURLCount(wc));
                WebRefiner.PageInfo pageInfo = wbr.getPageInfo(wc);
                assertNotNull(pageInfo);
                assertEquals(actualTotalURLs, pageInfo.mTotalUrls);
                assertEquals(0, pageInfo.mBlockedUrls);
                assertEquals(0, pageInfo.mWhiteListedUrls);
                assertNotNull(pageInfo.mMatchedURLInfoList);
                assertEquals(pageInfo.mBlockedUrls + pageInfo.mWhiteListedUrls, pageInfo.mMatchedURLInfoList.length);
            }

        } finally {
            webServer.shutdown();
        }
    }

    private static final String TEST_PAGE_CONTENT_IOS = "<html>\n" +
            "<head>\n" +
            "    <script src=\"/script_elemhide.js\" type=\"text/javascript\"></script>\n" +
            "    <link rel=\"stylesheet\" type=\"text/css\" href=\"/css_elemhide.css\">\n" +
            "    <script>\n" +
            "       function foobar1() {return 2;}\n" +
            "       function foobar2() {var elem = document.getElementById(\"test2\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "       function foobar3() {var elem = document.getElementById(\"test3\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "       function foobar4() {var elem = document.getElementById(\"test4\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "       function foobar5() {var elem = document.getElementById(\"test5\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "       function foobar6() {var elem = document.getElementById(\"test6\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "       function foobar7() {var elem = document.getElementById(\"myframe1\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "       function foobar8() {var elem = document.getElementById(\"test8\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "    </script>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"test2\">test2</div>\n" +
            "    <div id=\"test3\">test3</div>\n" +
            "    <div id=\"test4\">test4</div>\n" +
            "    <div id=\"test5\">test5</div>\n" +
            "    <div id=\"test6\" class=\"testclass\">test6</div>\n" +
            "    <div id=\"test8\">test8</div>\n" +
            "    <p>WebRefiner test page.</p>\n" +
            "    <iframe name=\"myframe1\" id=\"myframe1\" src=\"/ad_frame01.html\"></iframe>\n" +
            "    <img src=\"/ad_img01.jpg\">\n" +
            "</body>";

    private static final String RULE_SET_DATA_IOS = "[" +
            "{\"trigger\": {\"url-filter\": \".*\"}, \"action\": {\"type\": \"css-display-none\",\"selector\": \"#test2\"}}," +
            "{\"trigger\": {\"url-filter\": \"not_found\"}, \"action\": {\"type\": \"css-display-none\",\"selector\": \"#test3\"}}," +
            "{\"trigger\": {\"url-filter\": \".*\",\"resource-type\": \"image\"}, \"action\": {\"type\": \"css-display-none\",\"selector\": \"#test4\"}}," +
            "{\"trigger\": {\"url-filter\": \"webrefiner_test\"}, \"action\": {\"type\": \"css-display-none\",\"selector\": \"#test5\"}}," +
            "{\"trigger\": {\"url-filter\": \"ad_frame\"}, \"action\": {\"type\": \"css-display-none\",\"selector\": \"#test6\"}}," +
            "{\"trigger\": {\"url-filter\": \"ad_img\"}, \"action\": {\"type\": \"css-display-none\",\"selector\": \"#myframe1\"}}," +
            "{\"trigger\": {\"url-filter\": \"css_elemhide\"}, \"action\": {\"type\": \"css-display-none\",\"selector\": \"#test8\"}}" +
            "]";

    @MediumTest
    @Feature({"WebRefiner"})
    public void testRuleSetIOS() throws Exception {

        TestWebServer webServer = TestWebServer.start();
        try {
            WebRefiner wbr = WebRefiner.getInstance();
            assertNotNull(wbr);

            String ruleSetFileName = "rule_set_02.rules";
            assertTrue(writeToFile(RULE_SET_DATA_IOS, ruleSetFileName));

            RuleSet rs = new RuleSet("TestFilters", new File(mActivity.getApplicationInfo().dataDir, ruleSetFileName).getAbsolutePath(), WebRefiner.RuleSet.CATEGORY_ADS, 1);
            wbr.addRuleSet(rs);

            assertTrue(waitForRuleSetToApply(wbr));

            final String urlString = webServer.setResponse("/webrefiner_test.html", TEST_PAGE_CONTENT_IOS, null);
            webServer.setResponse("/ad_img01.jpg", "", null);
            webServer.setResponse("/ad_frame01.html", "<html><head><p>test frame</p></head>", null);
            webServer.setResponse("/css_elemhide.css", "table {border: 1px solid black;}", null);
            webServer.setResponse("/script_elemhide.js", "var a = 1;", null);

            {

                loadUrlAndWaitForPageLoadCompletion(urlString);
                Tab tab1 = getActivity().getActivityTab();

                // Test executing javascript from the document
                Integer jsNumber = Integer.parseInt(
                        JavaScriptUtils.executeJavaScriptAndWaitForResult(
                                tab1.getWebContents(), "foobar1()"));
                assertEquals(2, jsNumber.intValue());

                // Test element hide filter match everything
                String elemhidetest2 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "foobar2()");
                assertEquals("\"none\"", elemhidetest2);

                // Test element hide filter no match
                String elemhidetest3 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "foobar3()");
                assertEquals("\"block\"", elemhidetest3);

                // Test element hide filter match image resource
                String elemhidetest4 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "foobar4()");
                assertEquals("\"none\"", elemhidetest4);

                // Test element hide filter match document url
                String elemhidetest5 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "foobar5()");
                assertEquals("\"none\"", elemhidetest5);

                // Test match an iframe but selector is in main document
                String elemhidetest6 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "foobar6()");
                assertEquals("\"block\"", elemhidetest6);

                // Test match an image resource by url match and hide iframe element
                String elemhidetest7 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "foobar7()");
                assertEquals("\"none\"", elemhidetest7);

                // Test match a stylesheet resource by url match
                String elemhidetest8 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "foobar8()");
                assertEquals("\"none\"", elemhidetest8);

            }


        } finally {
            webServer.shutdown();
        }
    }

    private static final String TEST_PAGE_CONTENT_ELEM_HIDE = "<html>\n" +
            "<head>\n" +
            "__FAVICON_PLACEHOLDER__\n" +
            "<script>\n" +
            "var stylemap = {}\n" +
            "function getstyle1() {var elem = document.getElementById(\"e1\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "function getstyle2() {var elem = document.getElementById(\"e2\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "function getstyle3() {return stylemap[\"e3\"]};\n" +
            "function getstyle4() {return stylemap[\"e4\"]};\n" +
            "function getstyle5() {return stylemap[\"e5\"]};\n" +
            "window.onmessage = function(e) {stylemap[e.data.split(\":\")[0]] = e.data.split(\":\")[1];}\n" +
            "</script>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div id = \"e1\">\n" +
            "<p> This is element 1 </p>\n" +
            "</div>\n" +
            "<div id = \"e2\">\n" +
            "<p> This is element 2 </p>\n" +
            "</div>\n";

    private static final String TEST_FRAME1_CONTENT_ELEM_HIDE = "<html>\n" +
            "<head>\n" +
            "<script>\n" +
            "function getstyle3() {var elem = document.getElementById(\"e3\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "window.onload = function() {window.setTimeout(function(){window.top.postMessage(\"e3:\" + getstyle3(), \"*\");}, 1000)}\n" +
            "</script>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div id = \"e3\">\n" +
            "<p> This is element 3 </p>\n" +
            "</div>\n";

    private static final String TEST_FRAME2_CONTENT_ELEM_HIDE = "<html>\n" +
            "<head>\n" +
            "<script>\n" +
            "function getstyle4() {var elem = document.getElementById(\"e4\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "function getstyle5() {var elem = document.getElementById(\"e5\"); return window.getComputedStyle(elem, null).getPropertyValue(\"display\");}\n" +
            "window.onload = function() {window.setTimeout(function(){window.top.postMessage(\"e4:\" + getstyle4(), \"*\"); window.top.postMessage(\"e5:\" + getstyle5(), \"*\");}, 1000)}\n" +
            "</script>\n" +
            "</head>\n" +
            "<body>\n" +
            "<div id = \"e4\">\n" +
            "<p> This is element 4 </p>\n" +
            "</div>\n" +
            "<div id = \"e5\">\n" +
            "<p> This is element 5 </p>\n" +
            "</div>\n" +
            "</body>\n" +
            "</html>";

    @MediumTest
    @CommandLineFlags.Add("host-resolver-rules=MAP *.wrtest 127.0.0.1")
    @Feature({"WebRefiner"})
    public void testElemHide() throws Exception {
        WebRefinerTestServer server1 = WebRefinerTestServer.start();
        WebRefinerTestServer server2 = WebRefinerTestServer.start();
        WebRefinerTestServer server3 = WebRefinerTestServer.start();
        try {
            WebRefiner wbr = WebRefiner.getInstance();
            assertNotNull(wbr);


            final String RULE_SET_DATA_ELEM_HIDE = "###e1\n" + "###e3\n" + "###e4\n";
            String ruleSetFileName = "rule_set_03.rules";
            assertTrue(writeToFile(RULE_SET_DATA_ELEM_HIDE, ruleSetFileName));

            RuleSet rs = new RuleSet("TestFilters", new File(mActivity.getApplicationInfo().dataDir, ruleSetFileName).getAbsolutePath(), WebRefiner.RuleSet.CATEGORY_ADS, 1);
            wbr.addRuleSet(rs);

            assertTrue(waitForRuleSetToApply(wbr));

            final String urlInitString = server1.setResponse("/webrefiner_warm_up.html", "<html><body>Warm Up...</body></html>", null);
            loadUrlAndWaitForPageLoadCompletion(urlInitString, 2000);

            String frame2String = server3.setResponse("/frame02.html", TEST_FRAME2_CONTENT_ELEM_HIDE, null);
            frame2String = replaceHost(frame2String, "tp03.wrtest");

            String frame1String = server2.setResponse("/frame01.html", TEST_FRAME1_CONTENT_ELEM_HIDE + "<iframe name=\"myframe2\" id=\"myframe2\" src=\"" + frame2String + "\"</iframe></body></html>", null);
            frame1String = replaceHost(frame1String, "tp02.wrtest");

            final String favIconUrl = server1.setResponseWithNotFoundStatus("/favicon.ico");
            String mainFrameContent = TEST_PAGE_CONTENT_ELEM_HIDE + "<iframe name=\"myframe1\" id=\"myframe1\" src=\"" + frame1String + "\"</iframe></body></html>";
            mainFrameContent = mainFrameContent.replaceAll("__FAVICON_PLACEHOLDER__", "<link href=\"" + favIconUrl + "\" rel=\"SHORTCUT ICON\">");

            String urlString = server1.setResponse("/webrefiner_test.html", mainFrameContent, null);
            urlString = replaceHost(urlString, "tp01.wrtest");
            {
                loadUrlAndWaitForPageLoadCompletion(urlString);

                Tab tab1 = getActivity().getActivityTab();

                String elemhidetest1 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "getstyle1()");
                assertEquals("\"none\"", elemhidetest1);
                String elemhidetest2 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "getstyle2()");
                assertEquals("\"block\"", elemhidetest2);
                String elemhidetest3 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "getstyle3()");
                assertEquals("\"none\"", elemhidetest3);
                String elemhidetest4 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "getstyle4()");
                assertEquals("\"none\"", elemhidetest4);
                String elemhidetest5 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "getstyle5()");
                assertEquals("\"block\"", elemhidetest5);
            }
        } finally {
            server1.shutdown();
            server2.shutdown();
            server3.shutdown();
        }
    }

    @MediumTest
    @CommandLineFlags.Add("host-resolver-rules=MAP *.wrtest 127.0.0.1")
    @Feature({"WebRefiner"})
    public void testElemHideWhitelist() throws Exception {
        WebRefinerTestServer server1 = WebRefinerTestServer.start();
        WebRefinerTestServer server2 = WebRefinerTestServer.start();
        WebRefinerTestServer server3 = WebRefinerTestServer.start();
        try {
            WebRefiner wbr = WebRefiner.getInstance();
            assertNotNull(wbr);

            final String RULE_SET_DATA_ELEM_HIDE_WHITELIST = "###e1\n" + "###e3\n" + "###e4\n" + "@@||tp02.wrtest^$elemhide\n";
            String ruleSetFileName = "rule_set_04.rules";
            assertTrue(writeToFile(RULE_SET_DATA_ELEM_HIDE_WHITELIST, ruleSetFileName));

            RuleSet rs = new RuleSet("TestFilters", new File(mActivity.getApplicationInfo().dataDir, ruleSetFileName).getAbsolutePath(), WebRefiner.RuleSet.CATEGORY_ADS, 1);
            wbr.addRuleSet(rs);

            assertTrue(waitForRuleSetToApply(wbr));

            final String urlInitString = server1.setResponse("/webrefiner_warm_up.html", "<html><body>Warm Up...</body></html>", null);
            loadUrlAndWaitForPageLoadCompletion(urlInitString, 2000);

            String frame2String = server3.setResponse("/frame02.html", TEST_FRAME2_CONTENT_ELEM_HIDE, null);
            frame2String = replaceHost(frame2String, "tp03.wrtest");

            String frame1String = server2.setResponse("/frame01.html", TEST_FRAME1_CONTENT_ELEM_HIDE + "<iframe name=\"myframe2\" id=\"myframe2\" src=\"" + frame2String + "\"</iframe></body></html>", null);
            frame1String = replaceHost(frame1String, "tp02.wrtest");

            final String favIconUrl = server1.setResponseWithNotFoundStatus("/favicon.ico");
            String mainFrameContent = TEST_PAGE_CONTENT_ELEM_HIDE + "<iframe name=\"myframe1\" id=\"myframe1\" src=\"" + frame1String + "\"</iframe></body></html>";
            mainFrameContent = mainFrameContent.replaceAll("__FAVICON_PLACEHOLDER__", "<link href=\"" + favIconUrl + "\" rel=\"SHORTCUT ICON\">");

            String urlString = server1.setResponse("/webrefiner_test.html", mainFrameContent, null);
            urlString = replaceHost(urlString, "tp01.wrtest");
            {
                loadUrlAndWaitForPageLoadCompletion(urlString);

                Tab tab1 = getActivity().getActivityTab();

                String elemhidetest1 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "getstyle1()");
                assertEquals("\"none\"", elemhidetest1);
                String elemhidetest2 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "getstyle2()");
                assertEquals("\"block\"", elemhidetest2);
                String elemhidetest3 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "getstyle3()");
                assertEquals("\"block\"", elemhidetest3);
                String elemhidetest4 = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                        tab1.getWebContents(), "getstyle4()");
                assertEquals("\"block\"", elemhidetest4);
            }
        } finally {
            server1.shutdown();
            server2.shutdown();
            server3.shutdown();
        }
    }

    // Test for ||foobar.com^$inline-test type filters
    private static final String TEST_INLINE_SCRIPT_MAINFRAME =
            "<html>" +
                    "<head>" +
                    "__TEST_SCRIPT_PLACEHOLDER__\n" +
                    "</head>" +
                    "<body>" +
                    "<div id = \"e1\" data-test=\"unmodified\">" +
                    "    <p> This is element 1 </p>" +
                    "</div>" +
                    "<div id = \"e2\" data-test=\"unmodified\">" +
                    "    <p> This is element 2 </p>" +
                    "</div>" +
                    "<div id = \"e23\" data-test=\"unmodified\">" +
                    "    <p> This is element 23 </p>" +
                    "</div>" +
                    "<script type=\"text/javascript\">" +
                    "    var e = document.getElementById('e2');\n" +
                    "    e.dataset.test = \"modified\";\n" +
                    "    window.top.postMessage(\"e2:\" + e.dataset.test, \"*\");\n" +
                    "</script>" +
                    "<script type=\"text/javascript\">" +
                    "    var e = document.getElementById('e23');\n" +
                    "    var FooBar = e;\n" +
                    "    e.dataset.test = \"modified\";\n" +
                    "    window.top.postMessage(\"e23:\" + e.dataset.test, \"*\");\n" +
                    "</script>";

    private static final String TEST_INLINE_SCRIPT_TEST_SCRIPT =
            "    var inline_script_status = {};\n" +
                    "    function getscriptstatus(key) {\n" +
                    "        return inline_script_status[key];\n" +
                    "    }\n" +
                    "    window.onmessage = function(e) {\n" +
                    "        var key = e.data.split(\":\")[0];\n" +
                    "        var value = e.data.split(\":\")[1];\n" +
                    "        inline_script_status[key] = value;\n" +
                    "        console.log(\"window.onmessage [\" + key + \":\" + inline_script_status[key] + \"]\");\n" +
                    "    }\n";

    private static final String TEST_INLINE_SCRIPT_SCRIPT1 =
            "    var e = document.getElementById('e1');\n" +
                    "    e.dataset.test = \"modified\";\n" +
                    "    window.top.postMessage(\"e1:\" + e.dataset.test, \"*\");";

    private static final String TEST_INLINE_SCRIPT_IFRAME1 =
            "<html><body>" +
                    "<div id = \"e3\">" +
                    "    <p> This is element 3 </p>" +
                    "</div>" +
                    "<script type=\"text/javascript\">" +
                    "    var e = document.getElementById('e3');\n" +
                    "    e.dataset.test = \"modified\";\n" +
                    "    window.top.postMessage(\"e3:\" + e.dataset.test, \"*\");\n" +
                    "</script>" +
                    "</body></html>";

    private static final String TEST_INLINE_SCRIPT_IFRAME2 =
            "<html><body>" +
                    "<div id = \"e4\">" +
                    "    <p> This is element 4 </p>" +
                    "</div>" +
                    "<script type=\"text/javascript\">" +
                    "    var e = document.getElementById('e4');\n" +
                    "    e.dataset.test = \"modified\";\n" +
                    "    var BlahBlah = 42;\n" +
                    "    window.top.postMessage(\"e4:\" + e.dataset.test, \"*\");\n" +
                    "</script>" +
                    "</body></html>";

    String setupInlineScriptBlockingTestContent(WebRefinerTestServer server1, WebRefinerTestServer server2) throws Exception {
        String testScriptUrl = server1.setResponse("/test_script.js", TEST_INLINE_SCRIPT_TEST_SCRIPT, null);
        testScriptUrl = replaceHost(testScriptUrl, "mainframe.csp.wrtest");

        String script1Url = server1.setResponse("/script1.js", TEST_INLINE_SCRIPT_SCRIPT1, null);
        script1Url = replaceHost(script1Url, "mainframe.csp.wrtest");

        String frame1Url = server1.setResponse("/frame1.html", TEST_INLINE_SCRIPT_IFRAME1, null);
        frame1Url = replaceHost(frame1Url, "iframe1.csp.wrtest");

        String frame2Url = server2.setResponse("/frame2.html", TEST_INLINE_SCRIPT_IFRAME2, null);
        frame2Url = replaceHost(frame2Url, "iframe2.csp.wrtest");

        String mainFrameContent = TEST_INLINE_SCRIPT_MAINFRAME;
        mainFrameContent = mainFrameContent.replaceAll("__TEST_SCRIPT_PLACEHOLDER__", "<script src=\"" + testScriptUrl + "\"></script>");
        mainFrameContent +=
                "<iframe src=\"" + frame1Url + "\"></iframe>\n" +
                        "<script src=\"" + script1Url + "\"></script>\n" +
                        "<iframe src=\"" + frame2Url + "\"></iframe>\n" +
                        "</body></html>\n";
        String mainFrameUrl = server1.setResponse("/webrefiner_test.html", mainFrameContent, null);
        mainFrameUrl = replaceHost(mainFrameUrl, "mainframe.csp.wrtest");
        return mainFrameUrl;
    }

    @MediumTest
    @CommandLineFlags.Add("host-resolver-rules=MAP *.wrtest 127.0.0.1")
    @Feature({"WebRefiner"})
    public void testInlineScriptBlocking() throws Exception {
        WebRefinerTestServer server1 = WebRefinerTestServer.start();
        WebRefinerTestServer server2 = WebRefinerTestServer.start();
        try {
            WebRefiner wbr = WebRefiner.getInstance();
            assertNotNull(wbr);

            final String FILTER_SET_DATA = "||csp.wrtest$inline-script\n";
            final String FILTER_SET_FILE_NAME = "rule_set_04.rules";
            assertTrue(writeToFile(FILTER_SET_DATA, FILTER_SET_FILE_NAME));
            File ruleSetFile = new File(mActivity.getApplicationInfo().dataDir, FILTER_SET_FILE_NAME);
            RuleSet rs = new RuleSet("TestFilters", ruleSetFile.getAbsolutePath(), WebRefiner.RuleSet.CATEGORY_ADS, 1);
            wbr.addRuleSet(rs);
            assertTrue(waitForRuleSetToApply(wbr));

            String mainFrameUrl = setupInlineScriptBlockingTestContent(server1, server2);
            {
                loadUrlAndWaitForPageLoadCompletion(mainFrameUrl, 1000);

                Tab tab1 = getActivity().getActivityTab();
                {
                    String result = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                            tab1.getWebContents(), "getscriptstatus(\"e1\")");
                    assertEquals("\"modified\"", result);
                }
                {
                    String result = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                            tab1.getWebContents(), "getscriptstatus(\"e2\")");
                    assertEquals("null", result);
                }
                {
                    String result = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                            tab1.getWebContents(), "getscriptstatus(\"e3\")");
                    assertEquals("\"modified\"", result);
                }
                {
                    String result = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                            tab1.getWebContents(), "getscriptstatus(\"e4\")");
                    assertEquals("\"modified\"", result);
                }
            }
        } finally {
            server1.shutdown();
            server2.shutdown();
        }
    }

    @MediumTest
    @CommandLineFlags.Add("host-resolver-rules=MAP *.wrtest 127.0.0.1")
    @Feature({"WebRefiner"})
    public void testInlineScriptFilterBlocking() throws Exception {
        WebRefinerTestServer server1 = WebRefinerTestServer.start();
        WebRefinerTestServer server2 = WebRefinerTestServer.start();
        try {
            WebRefiner wbr = WebRefiner.getInstance();
            assertNotNull(wbr);

            final String FILTER_SET_DATA =
                    "csp.wrtest##script:contains(FooBar)\n" +
                            "csp.wrtest##script:contains(/.*BlahBl[a-z]h.*/)\n";

            final String FILTER_SET_FILE_NAME = "rule_set_05.rules";
            assertTrue(writeToFile(FILTER_SET_DATA, FILTER_SET_FILE_NAME));
            File ruleSetFile = new File(mActivity.getApplicationInfo().dataDir, FILTER_SET_FILE_NAME);
            RuleSet rs = new RuleSet("TestFilters", ruleSetFile.getAbsolutePath(), WebRefiner.RuleSet.CATEGORY_ADS, 1);
            wbr.addRuleSet(rs);
            assertTrue(waitForRuleSetToApply(wbr));

            String mainFrameUrl = setupInlineScriptBlockingTestContent(server1, server2);
            {
                loadUrlAndWaitForPageLoadCompletion(mainFrameUrl, 1000);

                Tab tab1 = getActivity().getActivityTab();
                {
                    String result = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                            tab1.getWebContents(), "getscriptstatus(\"e1\")");
                    assertEquals("\"modified\"", result);
                }
                {
                    String result = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                            tab1.getWebContents(), "getscriptstatus(\"e2\")");
                    assertEquals("\"modified\"", result);
                }
                {
                    String result = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                            tab1.getWebContents(), "getscriptstatus(\"e23\")");
                    assertEquals("null", result);
                }
                {
                    String result = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                            tab1.getWebContents(), "getscriptstatus(\"e3\")");
                    assertEquals("\"modified\"", result);
                }
                {
                    String result = JavaScriptUtils.executeJavaScriptAndWaitForResult(
                            tab1.getWebContents(), "getscriptstatus(\"e4\")");
                    assertEquals("null", result);
                }
            }
        } finally {
            server1.shutdown();
            server2.shutdown();
        }
    }

}

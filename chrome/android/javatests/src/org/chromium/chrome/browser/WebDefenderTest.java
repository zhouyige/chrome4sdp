/*
 *  Copyright (c) 2016, The Linux Foundation. All rights reserved.
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
import android.util.Pair;

import org.chromium.base.CommandLine;
import org.chromium.base.test.util.CallbackHelper;
import org.chromium.base.test.util.CommandLineFlags;
import org.chromium.base.test.util.Feature;
import org.chromium.chrome.browser.ChromeActivity;
import org.chromium.chrome.browser.tab.EmptyTabObserver;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.test.ChromeTabbedActivityTestBase;
import org.chromium.content.browser.WebDefender;
import org.chromium.content_public.browser.LoadUrlParams;
import org.chromium.content_public.browser.WebContents;
import org.chromium.ui.base.PageTransition;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WebDefenderTest extends ChromeTabbedActivityTestBase {
    private static final String LOGTAG = "WebDefenderTest";
    private static final int SHORT_WAIT_TIMEOUT = 1000; // In ms
    private static final int WAIT_TIME_AFTER_PAGELOAD = 1500; //In ms

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
    @CommandLineFlags.Add("host-resolver-rules=MAP *.wdtest 127.0.0.1")
    @Feature({"WebDefender"})
    public void testInitialization() throws Exception {
        CommandLine cl = CommandLine.getInstance();
        assertTrue(cl.hasSwitch("host-resolver-rules"));

        WebDefender wbdfndr = WebDefender.getInstance();
        assertNotNull(wbdfndr);
        assertTrue(WebDefender.isInitialized());
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

    public void loadUrlAndWaitForPageLoadCompletion(final String url) throws InterruptedException {
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

    private boolean waitForRuleSetToApply(WebDefender wdr) {
        boolean result = true;
        try {
            Class noparams[] = {};
            Method method = wdr.getClass().getDeclaredMethod("ensurePendingRuleSetsApplied", noparams);
            method.invoke(wdr, (Object[]) null);
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


    private static String getTestPageContent(String script01Url, String iframe01Url) {
        return new String(
                "<html><body>\n" +
                        "    <p>WebDefender test page.</p>\n" +
                        "    <p id=\"p1\">XXXXXXXXXXXXXXXXXXXXXXXXXXXX</p>\n" +
                        "    <iframe src=\"" + iframe01Url + "\"></iframe>\n" +
                        "    <script src=\"" + script01Url + "\" type=\"text/javascript\"></script>\n" +
                        "</body></html>"
        );
    }

    @MediumTest
    @CommandLineFlags.Add("host-resolver-rules=MAP *.wdtest 127.0.0.1")
    @Feature({"WebDefender"})
    public void testBasics() throws Exception {

        WebRefinerTestServer server1 = WebRefinerTestServer.start();
        WebRefinerTestServer server2 = WebRefinerTestServer.start();
        WebRefinerTestServer server3 = WebRefinerTestServer.start();
        try {
            WebDefender wdr = WebDefender.getInstance();
            assertNotNull(wdr);
            assertTrue(waitForRuleSetToApply(wdr));

            final List<Pair<String, String>> headers = new ArrayList<Pair<String, String>>();
            headers.add(new Pair<String, String>("Set-Cookie", "UserID=WdTest; Max-Age=3600; Version=1"));

            final String SCRIPT01_CONTENT = "document.getElementById(\"p1\").innerHTML = \"Script01 executed!\";";
            String script01Url = server3.setResponse("/script01.js", SCRIPT01_CONTENT, headers);
            script01Url = replaceHost(script01Url, "tp01.wdtest");

            final String IFRAME01_CONTENT = "<html><body>\n" +
                    "   <p>iframe 01</p>\n" +
                    "   <script type=\"text/javascript\">" +
                    "        document.cookie = \"favorite_food=tripe\";\n" +
                    "   </script>\n" +
                    "</body></html>";
            String iframe01Url = server2.setResponse("/iframe01.html", IFRAME01_CONTENT, null);
            iframe01Url = replaceHost(iframe01Url, "tp02.wdtest");

            String urlString = server1.setResponse("/webdefender_test.html", getTestPageContent(script01Url, iframe01Url), null);

            String[] tpDomains = {"tp01.wdtest", "tp02.wdtest"};
            Set<String> trackerDomains = new HashSet<String>(Arrays.asList(tpDomains));

            // First Load
            {
                urlString = replaceHost(urlString, "fp01.wdtest");
                loadUrlAndWaitForPageLoadCompletion(urlString, WAIT_TIME_AFTER_PAGELOAD);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                final WebDefender.ProtectionStatus ps = wdr.getProtectionStatus(wc);
                assertNotNull(ps);

                assertTrue(ps.mTrackingProtectionEnabled);
                assertEquals(2, ps.mTrackerDomains.length);
                for (WebDefender.TrackerDomain td : ps.mTrackerDomains) {
                    assertTrue(trackerDomains.contains(td.mName));
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mProtectiveAction);
                    assertEquals(WebDefender.TrackerDomain.TRACKING_METHOD_HTTP_COOKIES, td.mTrackingMethods);
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mUserDefinedProtectiveAction);
                    assertFalse(td.mUsesUserDefinedProtectiveAction);
                    assertFalse(td.mPotentialTracker);
                }
            }

            // Second Load
            {
                urlString = replaceHost(urlString, "fp02.wdtest");
                loadUrlAndWaitForPageLoadCompletion(urlString, WAIT_TIME_AFTER_PAGELOAD);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                final WebDefender.ProtectionStatus ps = wdr.getProtectionStatus(wc);
                assertNotNull(ps);

                assertTrue(ps.mTrackingProtectionEnabled);
                assertEquals(2, ps.mTrackerDomains.length);
                for (WebDefender.TrackerDomain td : ps.mTrackerDomains) {
                    assertTrue(trackerDomains.contains(td.mName));
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mProtectiveAction);
                    assertEquals(WebDefender.TrackerDomain.TRACKING_METHOD_HTTP_COOKIES, td.mTrackingMethods);
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mUserDefinedProtectiveAction);
                    assertFalse(td.mUsesUserDefinedProtectiveAction);
                    assertTrue(td.mPotentialTracker);
                }
            }

            // Third Load
            {
                urlString = replaceHost(urlString, "fp03.wdtest");
                loadUrlAndWaitForPageLoadCompletion(urlString, WAIT_TIME_AFTER_PAGELOAD);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                final WebDefender.ProtectionStatus ps = wdr.getProtectionStatus(wc);
                assertNotNull(ps);

                assertTrue(ps.mTrackingProtectionEnabled);
                assertEquals(2, ps.mTrackerDomains.length);
                for (WebDefender.TrackerDomain td : ps.mTrackerDomains) {
                    assertTrue(trackerDomains.contains(td.mName));
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_BLOCK_URL, td.mProtectiveAction);
                    assertEquals(WebDefender.TrackerDomain.TRACKING_METHOD_HTTP_COOKIES, td.mTrackingMethods);
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mUserDefinedProtectiveAction);
                    assertFalse(td.mUsesUserDefinedProtectiveAction);
                    assertTrue(td.mPotentialTracker);
                }
            }

        } finally {
            server1.shutdown();
            server2.shutdown();
            server3.shutdown();
        }
    }

    private static String canvasFpIframeContent() {
        return "<html>\n" +
                "  <body>\n" +
                "    <table border=1>\n" +
                "        <tr>\n" +
                "            <td>Fetched data URL</td>\n" +
                "            <td><textarea id=\"result\" rows=5 cols=75></textarea></td>\n" +
                "        </tr>\n" +
                "        <tr>\n" +
                "            <td>Drawn from data URL</td>\n" +
                "            <td><canvas id=\"redrawn\" width=\"578\" height=\"50\"></canvas></td>\n" +
                "        </tr>\n" +
                "    </table>\n" +
                "    <script>\n" +
                "        var res     = document.getElementById('result');\n" +
                "        var redrawn = document.getElementById('redrawn');\n" +
                "        function loadCanvas(canvas, dataURL) {\n" +
                "            var context = canvas.getContext('2d');\n" +
                "            var imageObj = new Image();\n" +
                "            imageObj.onload = function() {\n" +
                "                context.drawImage(this, 0, 0);\n" +
                "            };\n" +
                "            imageObj.src = dataURL;\n" +
                "        }\n" +
                "        function getOffScreenCanvasDataURL () {\n" +
                "            var canvas = document.createElement('canvas');\n" +
                "            var ctx = canvas.getContext('2d');\n" +
                "            var txt = 'SWE browser rocks!!!';\n" +
                "            ctx.textBaseline = \"top\";\n" +
                "            ctx.font = \"14px 'Arial'\";\n" +
                "            ctx.textBaseline = \"alphabetic\";\n" +
                "            ctx.fillStyle = \"#f60\";\n" +
                "            ctx.fillRect(125,1,62,20);\n" +
                "            ctx.fillStyle = \"#069\";\n" +
                "            ctx.fillText(txt, 2, 15);\n" +
                "            ctx.fillStyle = \"rgba(102, 204, 0, 0.7)\";\n" +
                "            ctx.fillText(txt, 4, 17);\n" +
                "            return canvas.toDataURL();\n" +
                "        }\n" +
                "        res.value = getOffScreenCanvasDataURL();\n" +
                "        loadCanvas(redrawn, res.value);\n" +
                "    </script>\n" +
                "  </body>\n" +
                "</html>\n";
    }

    @MediumTest
    @CommandLineFlags.Add("host-resolver-rules=MAP *.wdtest 127.0.0.1")
    @Feature({"WebDefender"})
    public void testCanvasFingerprint() throws Exception {

        WebRefinerTestServer server1 = WebRefinerTestServer.start();
        WebRefinerTestServer server2 = WebRefinerTestServer.start();
        WebRefinerTestServer server3 = WebRefinerTestServer.start();
        try {
            WebDefender wdr = WebDefender.getInstance();
            assertNotNull(wdr);
            assertTrue(waitForRuleSetToApply(wdr));

            final String SCRIPT01_CONTENT = "document.getElementById(\"p1\").innerHTML = \"Script01 executed!\";";
            String script01Url = server3.setResponse("/script01.js", SCRIPT01_CONTENT, null);
            script01Url = replaceHost(script01Url, "tp11.wdtest");

            final String IFRAME01_CONTENT = canvasFpIframeContent();
            String iframe01Url = server2.setResponse("/iframe01.html", IFRAME01_CONTENT, null);
            iframe01Url = replaceHost(iframe01Url, "tp12.wdtest");

            String urlString = server1.setResponse("/webdefender_test.html", getTestPageContent(script01Url, iframe01Url), null);

            String[] tpDomains = {"tp11.wdtest", "tp12.wdtest"};
            Set<String> trackerDomains = new HashSet<String>(Arrays.asList(tpDomains));

            // First Load
            {
                urlString = replaceHost(urlString, "fp11.wdtest");
                loadUrlAndWaitForPageLoadCompletion(urlString, WAIT_TIME_AFTER_PAGELOAD);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                final WebDefender.ProtectionStatus ps = wdr.getProtectionStatus(wc);
                assertNotNull(ps);

                assertTrue(ps.mTrackingProtectionEnabled);
                assertEquals(1, ps.mTrackerDomains.length);
                for (WebDefender.TrackerDomain td : ps.mTrackerDomains) {
                    assertTrue(trackerDomains.contains(td.mName));
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mProtectiveAction);
                    assertEquals(WebDefender.TrackerDomain.TRACKING_METHOD_CANVAS_FINGERPRINT, td.mTrackingMethods);
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mUserDefinedProtectiveAction);
                    assertFalse(td.mUsesUserDefinedProtectiveAction);
                    assertFalse(td.mPotentialTracker);
                }
            }

            // Second Load
            {
                urlString = replaceHost(urlString, "fp12.wdtest");
                loadUrlAndWaitForPageLoadCompletion(urlString, WAIT_TIME_AFTER_PAGELOAD);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                final WebDefender.ProtectionStatus ps = wdr.getProtectionStatus(wc);
                assertNotNull(ps);

                assertTrue(ps.mTrackingProtectionEnabled);
                assertEquals(1, ps.mTrackerDomains.length);
                for (WebDefender.TrackerDomain td : ps.mTrackerDomains) {
                    assertTrue(trackerDomains.contains(td.mName));
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mProtectiveAction);
                    assertEquals(WebDefender.TrackerDomain.TRACKING_METHOD_CANVAS_FINGERPRINT, td.mTrackingMethods);
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mUserDefinedProtectiveAction);
                    assertFalse(td.mUsesUserDefinedProtectiveAction);
                    assertTrue(td.mPotentialTracker);
                }
            }

            // Third Load
            {
                urlString = replaceHost(urlString, "fp13.wdtest");
                loadUrlAndWaitForPageLoadCompletion(urlString, WAIT_TIME_AFTER_PAGELOAD);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                final WebDefender.ProtectionStatus ps = wdr.getProtectionStatus(wc);
                assertNotNull(ps);

                assertTrue(ps.mTrackingProtectionEnabled);
                assertEquals(1, ps.mTrackerDomains.length);
                for (WebDefender.TrackerDomain td : ps.mTrackerDomains) {
                    assertTrue(trackerDomains.contains(td.mName));
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_BLOCK_URL, td.mProtectiveAction);
                    assertEquals(WebDefender.TrackerDomain.TRACKING_METHOD_CANVAS_FINGERPRINT, td.mTrackingMethods);
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mUserDefinedProtectiveAction);
                    assertFalse(td.mUsesUserDefinedProtectiveAction);
                    assertTrue(td.mPotentialTracker);
                }
            }

        } finally {
            server1.shutdown();
            server2.shutdown();
            server3.shutdown();
        }
    }

    private String localStorageIframeContent() {
        return "<html>\n" +
                "  <body>\n" +
                "    <h2> Local Storage Tracker </h2>\n" +
                "    <script type=\"text/javascript\">\n" +
                "        localStorage.track = true;\n" +
                "    </script>\n" +
                "  </body>\n" +
                "</html>\n";
    }

    @MediumTest
    @CommandLineFlags.Add("host-resolver-rules=MAP *.wdtest 127.0.0.1")
    @Feature({"WebDefender"})
    public void testLocalStorageTracking() throws Exception {

        WebRefinerTestServer server1 = WebRefinerTestServer.start();
        WebRefinerTestServer server2 = WebRefinerTestServer.start();
        WebRefinerTestServer server3 = WebRefinerTestServer.start();
        try {
            WebDefender wdr = WebDefender.getInstance();
            assertNotNull(wdr);
            assertTrue(waitForRuleSetToApply(wdr));

            final String SCRIPT01_CONTENT = "document.getElementById(\"p1\").innerHTML = \"Script01 executed!\";";
            String script01Url = server3.setResponse("/script01.js", SCRIPT01_CONTENT, null);
            script01Url = replaceHost(script01Url, "tp21.wdtest");

            final String IFRAME01_CONTENT = localStorageIframeContent();
            String iframe01Url = server2.setResponse("/iframe01.html", IFRAME01_CONTENT, null);
            iframe01Url = replaceHost(iframe01Url, "tp22.wdtest");

            String urlString = server1.setResponse("/webdefender_test.html", getTestPageContent(script01Url, iframe01Url), null);

            String[] tpDomains = {"tp21.wdtest", "tp22.wdtest"};
            Set<String> trackerDomains = new HashSet<String>(Arrays.asList(tpDomains));

            // First Load
            {
                urlString = replaceHost(urlString, "fp21.wdtest");
                loadUrlAndWaitForPageLoadCompletion(urlString, WAIT_TIME_AFTER_PAGELOAD);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                final WebDefender.ProtectionStatus ps = wdr.getProtectionStatus(wc);
                assertNotNull(ps);

                assertTrue(ps.mTrackingProtectionEnabled);
                assertEquals(1, ps.mTrackerDomains.length);
                for (WebDefender.TrackerDomain td : ps.mTrackerDomains) {
                    assertTrue(trackerDomains.contains(td.mName));
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mProtectiveAction);
                    assertEquals(WebDefender.TrackerDomain.TRACKING_METHOD_HTML5_LOCAL_STORAGE, td.mTrackingMethods);
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mUserDefinedProtectiveAction);
                    assertFalse(td.mUsesUserDefinedProtectiveAction);
                    assertFalse(td.mPotentialTracker);
                }
            }

            // Second Load
            {
                urlString = replaceHost(urlString, "fp22.wdtest");
                loadUrlAndWaitForPageLoadCompletion(urlString, WAIT_TIME_AFTER_PAGELOAD);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                final WebDefender.ProtectionStatus ps = wdr.getProtectionStatus(wc);
                assertNotNull(ps);

                assertTrue(ps.mTrackingProtectionEnabled);
                assertEquals(1, ps.mTrackerDomains.length);
                for (WebDefender.TrackerDomain td : ps.mTrackerDomains) {
                    assertTrue(trackerDomains.contains(td.mName));
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mProtectiveAction);
                    assertEquals(WebDefender.TrackerDomain.TRACKING_METHOD_HTML5_LOCAL_STORAGE, td.mTrackingMethods);
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mUserDefinedProtectiveAction);
                    assertFalse(td.mUsesUserDefinedProtectiveAction);
                    assertTrue(td.mPotentialTracker);
                }
            }

            // Third Load
            {
                urlString = replaceHost(urlString, "fp23.wdtest");
                loadUrlAndWaitForPageLoadCompletion(urlString, WAIT_TIME_AFTER_PAGELOAD);

                WebContents wc = getActivity().getActivityTab().getWebContents();
                final WebDefender.ProtectionStatus ps = wdr.getProtectionStatus(wc);
                assertNotNull(ps);

                assertTrue(ps.mTrackingProtectionEnabled);
                assertEquals(1, ps.mTrackerDomains.length);
                for (WebDefender.TrackerDomain td : ps.mTrackerDomains) {
                    assertTrue(trackerDomains.contains(td.mName));
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_BLOCK_URL, td.mProtectiveAction);
                    assertEquals(WebDefender.TrackerDomain.TRACKING_METHOD_HTML5_LOCAL_STORAGE, td.mTrackingMethods);
                    assertEquals(WebDefender.TrackerDomain.PROTECTIVE_ACTION_UNBLOCK, td.mUserDefinedProtectiveAction);
                    assertFalse(td.mUsesUserDefinedProtectiveAction);
                    assertTrue(td.mPotentialTracker);
                }
            }

        } finally {
            server1.shutdown();
            server2.shutdown();
            server3.shutdown();
        }
    }

}

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

package org.codeaurora.swe.test;

import android.app.Activity;
import android.content.Context;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;
import android.util.Pair;

import org.chromium.base.CommandLine;
import org.chromium.base.test.util.CommandLineFlags;
import org.chromium.base.test.util.Feature;
import org.chromium.base.test.util.CallbackHelper;
import org.chromium.chrome.browser.ChromeActivity;
import org.chromium.chrome.browser.ChromeApplication;
import org.chromium.chrome.browser.ChromeSwitches;
import org.chromium.chrome.browser.preferences.XSSDefenderHandler;
import org.chromium.chrome.browser.WebshieldBridge;
import org.chromium.chrome.browser.tab.EmptyTabObserver;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tab.TabObserver;
import org.chromium.chrome.browser.tabmodel.TabModel;
import org.chromium.chrome.browser.tabmodel.TabModelUtils;
import org.chromium.chrome.test.ChromeTabbedActivityTestBase;
import org.chromium.chrome.test.util.ChromeTabUtils;
import org.chromium.chrome.test.util.browser.TabLoadObserver;
import org.chromium.content.browser.test.util.JavaScriptUtils;
import org.chromium.content.browser.ContentViewCore;
import org.chromium.content.browser.test.util.DOMUtils;
import org.chromium.content.browser.WebRefiner;
import org.chromium.content.browser.WebRefiner.RuleSet;
import org.chromium.net.test.util.TestWebServer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;


public class XSSDefenderTest extends ChromeTabbedActivityTestBase {
    private static final String LOGTAG = "XSSDefenderTest";
    private WebshieldBridge bridge;

    private ChromeActivity mActivity;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        bridge = new WebshieldBridge();
    }

    @Override
    public void startMainActivity() throws InterruptedException {
        startMainActivityFromLauncher();
    }

    private boolean isDisabled() {
        boolean isDisabled = CommandLine.getInstance().hasSwitch(ChromeSwitches.DISABLE_XSS_DEFENDER);
        if (isDisabled)
            Log.w(LOGTAG, "XSSDefender is currently disabled through the command line.");
        return isDisabled;
    }

    @MediumTest
    @Feature({"XSSDefender"})
    public void testDoesLibraryExist() throws Exception {
        if (isDisabled()) return;

        // Get library path from XSSDefenderHandler.java
        String xssdef_path = XSSDefenderHandler.getLibraryFullPath();
        Log.i(LOGTAG, "Checking path for XSS Defender: " + xssdef_path);

        // Test XSS Defender library exist
        File file = new File(xssdef_path);
        assertTrue(file.exists());
    }

    @MediumTest
    @Feature({"XSSDefender"})
    public void testIsLibraryLoaded() throws Exception {
        if (isDisabled()) return;

        boolean is_loaded = bridge.loadLibrary();

        // Test if library is loaded successfully
        assertTrue(is_loaded);
    }

    @MediumTest
    @Feature({"XSSDefender"})
    public void testGetVersionCode() throws Exception {
        if (isDisabled()) return;

        String benign_website = "http://www.test.com/?search=aaa";
        int website_type = 0; // Main frame

        Boolean is_loaded = bridge.loadLibrary();
        int version_num = bridge.getXSSDefVersion();
        int error_type = bridge.getErrorType();

        // Test if library is loaded
        assertTrue(is_loaded);
        // Test if the GetVersionCode API is missing from lib
        assertTrue(error_type == 0);
        assertTrue(version_num > 0);
    }

    @MediumTest
    @Feature({"XSSDefender"})
    public void testCheckURL() throws Exception {
        if (isDisabled()) return;

        String benign_website = "http://www.test.com/?search=aaa";
        int website_type = 0; // Main frame
        Boolean is_loaded = bridge.loadLibrary();
        boolean is_malicious = bridge.checkURL(benign_website, website_type);
        int error_type = bridge.getErrorType();

        // Test if library is loaded
        assertTrue(is_loaded);
        // Test if the CheckURL API is missing from lib
        assertTrue(error_type == 0);
    }

    @MediumTest
    @Feature({"XSSDefender"})
    public void testFilterURL() throws Exception {
        if (isDisabled()) return;

        String benign_website = "http://www.test.com/?search=aaa";
        boolean is_loaded = bridge.loadLibrary();
        String filteredURL = bridge.filterURL(benign_website);
        int error_type = bridge.getErrorType();

        // Test if library is loaded
        assertTrue(is_loaded);
        // Test if the CheckURL API is missing from lib
        assertTrue(error_type == 0);
    }

    @MediumTest
    @Feature({"XSSDefender"})
    public void testSetUserResponse() throws Exception {
        if (isDisabled()) return;

        String malicous_website = "http://www.test.com/search=<script>alert(\"test the model\")";
        boolean is_loaded = bridge.loadLibrary();
        bridge.sendUserResponse(malicous_website, true);
        int error_type = bridge.getErrorType();

        // Test if library is loaded
        assertTrue(is_loaded);
        // Test if the SetUserResponse API is missing from lib
        assertTrue(error_type == 0);
    }

    @MediumTest
    @Feature({"XSSDefender"})
    public void testModel() throws Exception {
        if (isDisabled()) return;

        String benign_website = "http://www.test.com/search=(\"test the model\")";
        int website_type = 0; // Main frame
        boolean is_loaded = bridge.loadLibrary();
        boolean is_malicious = bridge.checkURL(benign_website, website_type);
        int error_type = bridge.getErrorType();

        // Test if library is loaded
        assertTrue(is_loaded);
        // Test if the CheckURL API is missing from lib
        assertTrue(error_type == 0);
        //Test if the library is miss predicting benign links
        assertTrue(!is_malicious);

        String malicous_website = "http://www.test.com/search=<script>alert(\"test the model\")";
        is_malicious = bridge.checkURL(malicous_website, website_type);

        // Test if the library is not detecting malicious links
        assertTrue(is_malicious);
    }
}

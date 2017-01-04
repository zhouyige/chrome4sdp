/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *     * Neither the name of The Linux Foundation nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.chromium.content.browser;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.chromium.base.ThreadUtils;
import org.chromium.content_public.browser.WebContents;

import java.io.File;
import java.lang.reflect.Constructor;

public class SecureConnect {
    public static final String LOGTAG = "SecureConnect";

    public static final int PERMISSION_USE_DEFAULT      = 0;
    public static final int PERMISSION_ENABLE           = 1;
    public static final int PERMISSION_DISABLE          = 2;

    public static class URLInfo {
        public final String mURL;
        public final boolean mUpgraded;
        public final String mRulesetName;
        public final boolean mRulesetEnabled;
        public final String mDisableReason;

        public URLInfo(String url, boolean upgraded, String name, boolean enabled, String reason) {
            mURL = url;
            mUpgraded = upgraded;
            mRulesetName = name;
            mRulesetEnabled = enabled;
            mDisableReason = reason;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof URLInfo))
                return false;
            if (this == o)
                return true;
            URLInfo compareTo = (URLInfo) o;
            return mRulesetName.equals(compareTo.mRulesetName);
        }

        @Override
        public int hashCode() {
            return 31 * mRulesetName.hashCode() + 11;
        }
    }

    public static class Info {
        public final URLInfo mMainFrameURL;
        public final URLInfo[] mSubFrameURLs;
        public final int mNumURLsNotUpgraded;

        public Info(URLInfo mainURL, URLInfo[] subURLs, int numNotUpgraded) {
            mMainFrameURL = mainURL;
            mSubFrameURLs = subURLs;
            mNumURLsNotUpgraded = numNotUpgraded;
        }

        public boolean hasMainFrameUrl() {
            return mMainFrameURL != null && !TextUtils.isEmpty(mMainFrameURL.mURL);
        }

        public boolean wasMainFrameUpgraded() {
            return hasMainFrameUrl() && mMainFrameURL.mUpgraded;
        }

        public int getSubFrameUrlUpgradeCount() {
            int count = 0;
            if (mSubFrameURLs == null) return count;

            for (URLInfo info : mSubFrameURLs) {
                if (info.mUpgraded) count++;
            }
            return count;
        }

        public boolean hasMixedContent() {
            return wasMainFrameUpgraded() && mNumURLsNotUpgraded > 0;
        }
    }

    private static SecureConnect sInstance = null;
    /**
     * Should be called only once after the engine initialization is completed.
     * @return Initialization success/failure.
     */
    public static boolean Initialize(Context ctx) {
        try {
            Constructor<?> constructor =
                    Class.forName("com.qualcomm.qti.webrefiner.SecureConnectImpl")
                            .getConstructor(Context.class);
            sInstance = (SecureConnect)constructor.newInstance(ctx);
        } catch (Exception e) {
            Log.e(LOGTAG, "Initialization failed: " + e.toString());
        }
        return (sInstance != null);
    }

    /**
     * A convenient method to check whether the SecureConnect is initialized and set successfully
     * from its internal implementation.
     * @return Whether the SecureConnect has been initialized.
     */
    public static boolean isInitialized() {
        ThreadUtils.assertOnUiThread();
        return sInstance != null;
    }

    /**
     * @return The singleton SecureConnect object. Could be null if WebRefiner initialization is failed.
     */
    public static SecureConnect getInstance() {
        ThreadUtils.assertOnUiThread();
        return sInstance;
    }

    public void setDefaultPermission(boolean allow) {}

    public boolean validateRules(File file) {return false;}

    public void loadRules(File fileOrDir) {}

    public void commitRules() {}

    public void setPermissionForOrigins(String[] origins, int permission,
                                        boolean forIncognitoOnly) {}

    public void resetAllIncognitoPermissions() {}

    public void unloadAllRules() {}

    public interface Listener {

        /**
         * Called whenever a page is upgraded
         * @param url URL of the page.
         * @param mainFrame Is the page main frame
         */
        void onPageUpgrade(String url, Boolean mainFrame);

        void onUpgradeFailed(String origin, String url, String redirect, int error);
    }

    public void setListener(Listener listener) {}

    public Info getInfo(WebContents wc) { return null; }

    /* Supports only getting a list of disabled rulesets */
    public String[] getRulesets(boolean disabledRulesetsOnly) { return null;}

    public void changeRulesetState(String rulesetName, boolean enable) {}

    public void setSecureContentOnlyMode(boolean enable) { }

    public boolean getSecureContentOnlyMode() { return false; }
}

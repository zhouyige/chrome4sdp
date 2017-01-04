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

package org.codeaurora.swe;

import java.util.Locale;

import android.content.Context;
import android.text.TextUtils;
import android.os.Build;

import org.chromium.base.CommandLine;


public final class SWECommandLine {

    private static SWECommandLine sSWECommandLine;
    private Context mContext;
    private boolean mEstoreEnabled;
    private String mEstoreHomepage;
    private String mEstoreDownloadAppMessage;

    public static final String kSWEDownloadPathActivityIntent =
            "swe_downloadpath_activity_intent";
    public static final String kSWEDownloadPathActivityResultSelection =
            "swe_downloadpath_activity_result_selection";

    // Before the sub-feature to control Download Dir on MIME Type was introduced,
    // enabling the Custom Download Feature automatically enabled Download Dir
    // on MIME Type. To keep the pre-implementation behavior consistent:
    // 1. swe_downloadpath_dir_on_mime value take effect only if Custom Download
    //    feature ie enabled.
    // 2. Default value of swe_downloadpath_dir_on_mime is set to 'true'. This has
    //    the effect of enabling Download Folder on MIME Type when Custom Download
    //    is enabled unless explicitly defined and set to 'false'.
    public static final String kSWEDownloadPathDirOnMime =
            "swe_downloadpath_dir_on_mime";
    public static final Boolean kSWEDownloadPathDirOnMimeDefVal = true;

    private SWECommandLine(Context context) {
        mContext = context;
    }

    public static SWECommandLine getInstance(Context context) {
        if (sSWECommandLine == null)
            sSWECommandLine = new SWECommandLine(context);
        return sSWECommandLine;
    }

    public void initSWECommandLine() {
        overrideUserAgent();
        setExtraHTTPRequestHeaders();
        setEstoreConfig();
        setDrmUpload();
        setDownloadPathSelection();
    }

    private void overrideUserAgent() {
        // Check if the UA is already present using command line file
        CommandLine cl = CommandLine.getInstance();
        if (cl.hasSwitch(SWEBrowserSwitches.OVERRIDE_USER_AGENT)) {
            return;
        }

        final int id = mContext.getResources().getIdentifier("swe_def_ua_override", "string",
                mContext.getPackageName());
        String ua = mContext.getResources().getString(id);

        if (TextUtils.isEmpty(ua))
            return;

        ua = constructUserAgent(ua);

        if (!TextUtils.isEmpty(ua)){
            cl.appendSwitchWithValue(SWEBrowserSwitches.OVERRIDE_USER_AGENT, ua);
        }
    }

    private String constructUserAgent(String userAgent) {
        try {
            userAgent = userAgent.replaceAll("<%build_model>", Build.MODEL);
            userAgent = userAgent.replaceAll("<%build_version>", Build.VERSION.RELEASE);
            userAgent = userAgent.replaceAll("<%build_id>", Build.ID);
            userAgent = userAgent.replaceAll("<%language>", Locale.getDefault().getLanguage());
            userAgent = userAgent.replaceAll("<%country>", Locale.getDefault().getCountry());
            return userAgent;
        } catch (Exception ex) {
            return null;
        }
    }

    private void setExtraHTTPRequestHeaders() {
        CommandLine cl = CommandLine.getInstance();
        final int id = mContext.getResources().getIdentifier("swe_def_extra_http_headers", "string",
                mContext.getPackageName());
        String headers = mContext.getResources().getString(id);
        if (!TextUtils.isEmpty(headers))
            cl.appendSwitchWithValue(SWEBrowserSwitches.HTTP_HEADERS, headers);
    }

    private void setEstoreConfig() {
        final int estoreHomepageId = mContext.getResources().getIdentifier(
                                       "swe_estore_homepage",
                                       "string",
                                       mContext.getPackageName());
        final int estoreDownloadEstoreAppId = mContext.getResources().getIdentifier(
                                       "swe_download_estore_app",
                                       "string",
                                       mContext.getPackageName());

        if (estoreHomepageId == 0 || estoreDownloadEstoreAppId == 0) return;

        mEstoreHomepage = mContext.getResources().getString(estoreHomepageId);
        mEstoreDownloadAppMessage = mContext.getResources().getString(estoreDownloadEstoreAppId);
        if (!TextUtils.isEmpty(mEstoreHomepage)) {
            mEstoreEnabled = true;
        } else {
            mEstoreEnabled = false;
        }
    }

    public boolean getEstoreEnabled() {
        return mEstoreEnabled;
    }

    public String getEstoreHomepage() {
        return mEstoreHomepage;
    }

    public String getEstoreDownloadAppMessage() {
        return mEstoreDownloadAppMessage;
    }

    private void setDrmUpload() {
        final int id = mContext.getResources().getIdentifier("swe_feature_drm_upload",
                "bool",
                mContext.getPackageName());

        if (id == 0) return;

        if (mContext.getResources().getBoolean(id)) {
            CommandLine cl = CommandLine.getInstance();
            cl.appendSwitch(SWEBrowserSwitches.DRM_UPLOAD);
        }
    }

    public static String getResourceString(Context ctx, String resId) {
        final int id = ctx.getResources().getIdentifier(
                resId, "string", ctx.getPackageName());
        if (0 == id)
            return null;

        return ctx.getResources().getString(id);
    }

    public static boolean getResourceBoolean(Context ctx, String resId,
            boolean defVal) {
        final int id = ctx.getResources().getIdentifier(
                resId, "bool", ctx.getPackageName());
        if (0 == id)
            return defVal;

        return ctx.getResources().getBoolean(id);
    }

    private void setDownloadPathSelection() {
        String downloadpathSelectionActivityIntent = getResourceString(
                mContext, kSWEDownloadPathActivityIntent);
        String downloadpathSelectionActivityResult = getResourceString(
                mContext, kSWEDownloadPathActivityResultSelection);
        if (!TextUtils.isEmpty(downloadpathSelectionActivityIntent)
                && !TextUtils.isEmpty(downloadpathSelectionActivityResult)) {
            CommandLine cl = CommandLine.getInstance();
            cl.appendSwitch(SWEBrowserSwitches.DOWNLOAD_PATH_SELECTION);

            boolean downloadpathDirOnMime = getResourceBoolean(
                    mContext, kSWEDownloadPathDirOnMime,
                    kSWEDownloadPathDirOnMimeDefVal);
            if (downloadpathDirOnMime) {
                cl.appendSwitch(
                        SWEBrowserSwitches.DOWNLOAD_PATH_DIR_ON_MIME);
            }
        }
    }
}

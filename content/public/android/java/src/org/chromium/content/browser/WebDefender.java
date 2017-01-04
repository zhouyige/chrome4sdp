/*
 * Copyright (c) 2015-2016, The Linux Foundation. All rights reserved.
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
import android.util.Log;

import org.chromium.base.ThreadUtils;
import org.chromium.content_public.browser.WebContents;

import java.lang.reflect.Constructor;

public class WebDefender {
    public static final String LOGTAG = "WebDefender";

    public static class TrackerDomain {
        /**
         * Protective action on this domain.
         */
        public static final int PROTECTIVE_ACTION_UNBLOCK                 = 0; // GREEN
        public static final int PROTECTIVE_ACTION_BLOCK_COOKIES           = 1; // YELLOW
        public static final int PROTECTIVE_ACTION_BLOCK_URL               = 2; // RED

        /**
         * Tracking methods employed by this domain.
         */
        public static final int TRACKING_METHOD_NONE                        = 0;
        public static final int TRACKING_METHOD_HTTP_COOKIES                = 1;
        public static final int TRACKING_METHOD_HTML5_LOCAL_STORAGE         = 1 << 1;
        public static final int TRACKING_METHOD_CANVAS_FINGERPRINT          = 1 << 2;
        /* Note: More methods to follow */


        public final String mName;
        public final int mProtectiveAction;
        public final int mTrackingMethods;
        public final int mUserDefinedProtectiveAction;
        public final boolean mUsesUserDefinedProtectiveAction;
        public final boolean mPotentialTracker;

        /**
         * @param name Tracker domain name.
         * @param action Protective action identified automatically on this tracker domain.
         * @param userDefinedAction User-defined protective action that overrides the auto-identified action.
         * @param usesUserDefinedAction Whether the effective protective action is user-defined or auto-identified.
         * @param trackingMethods One or more tracking methods employed by this domain.
         * @param potentialTracker Whether this domain is a potential tracker.
         */
        public TrackerDomain(String name, int action, int userDefinedAction, boolean usesUserDefinedAction, int trackingMethods, boolean potentialTracker) {
            mName = name;
            mProtectiveAction = action;
            mTrackingMethods = trackingMethods;
            mUserDefinedProtectiveAction = userDefinedAction;
            mUsesUserDefinedProtectiveAction = usesUserDefinedAction;
            mPotentialTracker = potentialTracker;
        }

        public TrackerDomain(String name, int action) {
            this(name, action, PROTECTIVE_ACTION_UNBLOCK, false, TRACKING_METHOD_NONE, false);
        }

        public TrackerDomain(String name) {
            this(name, PROTECTIVE_ACTION_UNBLOCK);
        }
    }

    public static class ProtectionStatus {
        /**
         * List of tracker domains identified on a given web page.
         */
        public final TrackerDomain[] mTrackerDomains;
        /**
         * Flag to indicate whether the tracking protection is enabled on a given web page.
         */
        public final boolean mTrackingProtectionEnabled;

        public ProtectionStatus(TrackerDomain[] list, boolean flag) {
            mTrackerDomains = list;
            mTrackingProtectionEnabled = flag;
        }
    }

    public static class TrackerDomainStats {
        /**
         * Total number of potential tracker domains, but WebDefender didn't do anything about them.
         */
        public final int mPotentialTrackerDomains;
        /**
         * Total number of tracker domains that are allowed to connect, but their tracking vectors are blocked.
         */
        public final int mYellowTrackerDomains;
        /**
         * Total number of tracker domains that are not allowed to connect (effectively blocked all the contents).
         */
        public final int mRedTrackerDomains;
        /**
         * Total number of tracker domains that used tracking vectors to store/retrieve private information.
         * mTotalTrackerDomains = mPotentialTrackerDomains + mYellowTrackerDomains + mRedTrackerDomains + 'unclassified/unrestricted tracker domains';
         */
        public final int mTotalTrackerDomains;
        /**
         * Flag to indicate whether the tracking protection is enabled on a given web page.
         */
        public final boolean mTrackingProtectionEnabled;

        public TrackerDomainStats(int potentialTrackerDomains, int yellowTrackerDomains, int redTrackerDomains, int totalTrackerDomains, boolean trackingProtectionEnabled) {
            mPotentialTrackerDomains = potentialTrackerDomains;
            mYellowTrackerDomains = yellowTrackerDomains;
            mRedTrackerDomains = redTrackerDomains;
            mTotalTrackerDomains = totalTrackerDomains;
            mTrackingProtectionEnabled = trackingProtectionEnabled;
        }
    }

    private static WebDefender sInstance = null;
    /**
     * Should be called only once after the engine initialization is completed.
     * @return Initialization success/failure.
     */
    public static boolean Initialize(Context ctx) {
        try {
            Constructor<?> constructor =
                    Class.forName("com.qualcomm.qti.webrefiner.WebDefenderImpl").getConstructor(Context.class);
            sInstance = (WebDefender)constructor.newInstance(ctx);
        } catch (Exception e) {
            Log.e(LOGTAG, "Initialization failed: " + e.toString());
        }
        return (sInstance != null);
    }

    /**
     * A convenient method to check whether the WebDefender is initialized and set successfully
     * from its internal implementation.
     * @return Whether the WebDefender has been initialized.
     */
    public static boolean isInitialized() {
        ThreadUtils.assertOnUiThread();
        return sInstance != null;
    }

    /**
     * @return The singleton WebDefender object. Could be null if WebRefiner initialization is failed.
     */
    public static WebDefender getInstance() {
        ThreadUtils.assertOnUiThread();
        return sInstance;
    }

    /**
     * Generates ProtectionStatus from the statistics collected by WebDefender for the given WebContents session.
     * @param wc WebContents instance.
     * @return Protection status info collected by WebDefender on the WebContents's current page session.
     */
    public ProtectionStatus getProtectionStatus(WebContents wc) { return null; }

    /**
     * Returns TrackerDomainStats object that contains various tracker domain counts.
     * You could alternatively calculate all the information in TrackerDomainStats from
     * {@link #getProtectionStatus(WebContents)}, but this API is convenient if you
     * don't really want the list of {@link TrackerDomain}s and calculate the numbers.
     * @param wc WebContents instance of the given page.
     * @return TrackerDomainStats object or null for the given WebContents session.
     */
    public TrackerDomainStats getTrackerDomainStats(WebContents wc) { return null; }

    /**
     * Enables or disables tracking protection by default on all the websites.
     * Can be overridden by {@link #setPermissionForOrigins(String[], int, boolean)}.
     * @param allow True to enable and False to disable tracking protection on all the websites.
     */
    public void setDefaultPermission(boolean allow) {}

    /**
     * Enables or disables tracking protection for the given origins.
     * Overrides {@link #setDefaultPermission(boolean)}.
     * @param origins List of origins .
     * @param permission one of WebRefiner.PERMISSION_USE_DEFAULT, WebRefiner.PERMISSION_ENABLE and WebRefiner.PERMISSION_DISABLE.
     * @param incognitoOnly True to apply these permissions only on Incognito sessions
     *                      and False to apply these permissions on all the sessions.
     */
    public void setPermissionForOrigins(String[] origins, int permission, boolean incognitoOnly) {}

    /**
     * Resets all the permissions set by {@link #setPermissionForOrigins(String[], int, boolean)}
     * when 'boolean incognitoOnly' was set to True .
     */
    public void resetAllIncognitoPermissions() {}

    /**
     * Overrides WebDefenders default protective action on the given tracker domains.
     * @param trackerDomains List of TrackerDomains with their corresponding protective actions .
     */
    public void overrideProtectiveActionsForTrackerDomains(TrackerDomain[] trackerDomains) {}

    /**
     * Resets overridden protective actions back to default on the given tracker domains.
     * @param trackerDomains List of tracker domains to be reset.
     */
    public void resetProtectiveActionsForTrackerDomains(TrackerDomain[] trackerDomains) {}

    /**
     * Returns the unified and local name (en-us strings) for WebDefender.
     * @return A valid FeatureName object or null.
     */
    public WebRefiner.FeatureName getFeatureName() { return null; }

}

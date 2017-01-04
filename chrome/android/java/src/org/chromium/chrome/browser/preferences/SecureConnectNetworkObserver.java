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

package org.chromium.chrome.browser.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.LruCache;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.BrowserChromeActivity;
import org.chromium.chrome.browser.preferences.privacy.SecureContentDialog;
import org.chromium.content.browser.NetworkChangeNotifier;

import java.util.Map;

/**
 * A NetworkObserver which listens to a new network connection for Secure Connect feature.
 * Performs actions related to secure content only mode on a new wifi network connection.
 */
public class SecureConnectNetworkObserver implements NetworkChangeNotifier.NetworkObserver {
    private static final String NETWORK_PREFERENCE = "network_preference";

    private BrowserChromeActivity mActivity;
    private SharedPreferences mPrefs;
    private boolean mShowSecureContentDialog;
    private static LruCache<String, Boolean> mNetworkPrefCache;
    private static String mWifiSSID;

    public SecureConnectNetworkObserver(BrowserChromeActivity activity) {
        mActivity = activity;
        mPrefs = activity.getApplicationContext()
                .getSharedPreferences(NETWORK_PREFERENCE, Context.MODE_PRIVATE);
        int secureContentNetworkMaxCount = activity.getResources()
                .getInteger(R.integer.secure_content_network_max_count);
        if (secureContentNetworkMaxCount > 0) {
            mNetworkPrefCache = new LruCache(secureContentNetworkMaxCount);
            Map<String, ?> map = mPrefs.getAll();
            for (String key : map.keySet()) {
                mNetworkPrefCache.put(key, (Boolean) map.get(key));
            }
        }
    }

    @Override
    public void onNetworkConnect(long netId,
                                 NetworkChangeNotifier.NetworkConnectionType connectionType) {
        // Ignore non wifi networks. Go back to global preference.
        if(connectionType != NetworkChangeNotifier.NetworkConnectionType.CONNECTION_WIFI) {
            boolean globalPref = PrefServiceBridge.getInstance().getSecureContentOnlyEnabled();
            SecureConnectPreferenceHandler.setSecureContentOnlyMode(globalPref);
            return;
        }

        WifiManager wifiMgr = (WifiManager) mActivity.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        String newWifiSSID = String.valueOf(wifiInfo.getSSID().hashCode());
        if(newWifiSSID.equals(mWifiSSID)) return;

        mWifiSSID = newWifiSSID;
        Boolean pref = mNetworkPrefCache.get(mWifiSSID);
        if (pref != null) {
            // Use the stored preference, if applicable
            UseNetworkSpecificPrefIfNecessary(pref);
            mShowSecureContentDialog = false;
        } else {
            // New network - show dialog, if needed
            mShowSecureContentDialog = true;
            mActivity.showSecureContentDialogIfNecessary();
        }
    }

    private void UseNetworkSpecificPrefIfNecessary(boolean pref) {
        if (isSecureContentOnlyForNetworkApplicable()) {
            SecureConnectPreferenceHandler.setSecureContentOnlyMode(pref);
        }
    }

    /**
     * Display the secure content for network dialog if needed
     * @return whether the dialog was displayed or not
     */
    public boolean showSecureContentDialogIfNecessary() {
        if (showSecureContentDialog()) {
            SecureContentDialog.show(mActivity, this);
            mShowSecureContentDialog = false;
            return true;
        }
        return false;
    }

    private boolean showSecureContentDialog() {
        return mActivity.getIsAppInForeground() && mShowSecureContentDialog
                && isSecureContentOnlyForNetworkApplicable();
    }

    private boolean isSecureContentOnlyForNetworkApplicable() {
        return (!PrefServiceBridge.getInstance().getSecureContentOnlyEnabled() &&
                PrefServiceBridge.getInstance().getSecureContentOnlyForNetworkEnabled());
    }

    /**
     * @return Disable the secure content preference for current network.
     */
    public void disableSecureContentForNetworkIfExists() {
        Boolean pref = mNetworkPrefCache.get(mWifiSSID);
        if (pref != null) {
            mNetworkPrefCache.put(mWifiSSID, false);
        }
    }

    /**
     * @return Sets secure content preference for current network and enables/disables the secure
     * content feature accordingly.
     */
    public void setSecureContentForNetwork(boolean pref) {
        mNetworkPrefCache.put(mWifiSSID, pref);
        SecureConnectPreferenceHandler.setSecureContentOnlyMode(pref);
    }

    /**
     * @return Secure content preference for current network.
     * Returns null if the preference is not available.
     */
    public static Boolean getSecureContentForNetwork() {
        return mNetworkPrefCache.get(mWifiSSID);
    }

    public void writeCacheToSharedPref() {
        mPrefs.edit().clear().apply();
        Map<String , Boolean> map = mNetworkPrefCache.snapshot();
        for (String key : map.keySet()) {
            mPrefs.edit().putBoolean(key, map.get(key)).apply();
        }
    }
}

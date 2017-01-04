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

import org.chromium.base.ContextUtils;
import org.chromium.base.ObserverList;
import org.chromium.base.ThreadUtils;
import org.chromium.net.ConnectionType;
import org.chromium.net.NetworkChangeNotifierAutoDetect;
import org.chromium.net.RegistrationPolicyAlwaysRegister;

/**
 * Notifies the network connectivity events to it's observers. Uses chromium's
 * NetworkChangeNotifierAutoDetect which listens to platform changes in connectivity.
 */
public class NetworkChangeNotifier {
    public enum NetworkConnectionType {
        CONNECTION_UNKNOWN,
        CONNECTION_ETHERNET,
        CONNECTION_WIFI,
        CONNECTION_4G,
        CONNECTION_3G,
        CONNECTION_2G,
        CONNECTION_UNKNOWN_CELLULAR,
        CONNECTION_BLUETOOTH,
        CONNECTION_NONE
    }

    public interface NetworkObserver {
        void onNetworkConnect(long netId, NetworkConnectionType connectionType);
    }

    private NetworkChangeNotifierAutoDetect mNotifier;
    private ObserverList<NetworkObserver> mNetworkObservers = new ObserverList<>();

    private static NetworkChangeNotifier sInstance;

    /**
     * Returns the singleton instance.
     */
    public static NetworkChangeNotifier getInstance() {
        if (sInstance == null) {
            sInstance = new NetworkChangeNotifier();
        }
        return sInstance;
    }

    private NetworkConnectionType getConnectionType(int connectionType) {
        switch (connectionType) {
            case ConnectionType.CONNECTION_ETHERNET:
                return NetworkConnectionType.CONNECTION_ETHERNET;
            case ConnectionType.CONNECTION_WIFI:
                return NetworkConnectionType.CONNECTION_WIFI;
            case ConnectionType.CONNECTION_BLUETOOTH:
                return NetworkConnectionType.CONNECTION_BLUETOOTH;
            case ConnectionType.CONNECTION_2G:
                return NetworkConnectionType.CONNECTION_2G;
            case ConnectionType.CONNECTION_3G:
                return NetworkConnectionType.CONNECTION_3G;
            case ConnectionType.CONNECTION_4G:
                return NetworkConnectionType.CONNECTION_4G;
            default:
                return NetworkConnectionType.CONNECTION_UNKNOWN;
        }
    }

    public static void registerObserver(NetworkObserver observer) {
        getInstance().registerObserverInternal(observer);
    }

    private void registerObserverInternal(NetworkObserver observer) {
        ThreadUtils.assertOnUiThread();

        // Create the NetworkChangeNotifierAutoDetect if it does not exist already.
        if (mNotifier == null) {
            mNotifier = new NetworkChangeNotifierAutoDetect(
                    new NetworkChangeNotifierAutoDetect.Observer() {
                @Override
                public void onConnectionTypeChanged(int newConnectionType) {
                }
                @Override
                public void onMaxBandwidthChanged(double maxBandwidthMbps) {}
                @Override
                public void onNetworkConnect(long netId, int connectionType) {
                    ThreadUtils.assertOnUiThread();
                    for (NetworkObserver observer : mNetworkObservers) {
                        observer.onNetworkConnect(netId, getConnectionType(connectionType));
                    }
                }
                @Override
                public void onNetworkSoonToDisconnect(long netId) {}
                @Override
                public void onNetworkDisconnect(long netId) {}
                @Override
                public void purgeActiveNetworkList(long[] activeNetIds) {}
            }, ContextUtils.getApplicationContext(), new RegistrationPolicyAlwaysRegister());
        }
        mNetworkObservers.addObserver(observer);
    }

    public static void removeObserver(NetworkObserver observer) {
        getInstance().removeObserverInternal(observer);
    }

    private void removeObserverInternal(NetworkObserver observer) {
        mNetworkObservers.removeObserver(observer);
        if (mNetworkObservers.isEmpty() && mNotifier != null) {
            mNotifier.destroy();
            mNotifier = null;
        }
    }
}

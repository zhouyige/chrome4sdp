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
import android.util.Log;
import org.chromium.base.ThreadUtils;
import org.chromium.net.NetworkServices;


public class Libswenet {
    public static final String LOGTAG = "Libswenet";

    // private constructor used by singleton
    // may throw exception in the future
    private Libswenet() {}

    private static Libswenet sInstance = null;
    /**
     * Should be called only once after the engine initialization is completed.
     * @return Initialization success/failure.
     */
    public static boolean Initialize(Context ctx) {
        try {
            sInstance = new Libswenet();
        } catch (Exception e) {
            Log.e(LOGTAG, "Initialization failed: " + e.toString());
        }
        return (sInstance != null);
    }

    /**
     * A convenient method to check whether the Libswenet is initialized and set successfully
     * from its internal implementation.
     * @return Whether the Libswenet has been initialized.
     */
    public static boolean isInitialized() {
        ThreadUtils.assertOnUiThread();
        return sInstance != null;
    }

    /**
     * @return The singleton Libswenet object. Could be null if WebRefiner initialization is failed.
     */
    public static Libswenet getInstance() {
        ThreadUtils.assertOnUiThread();
        return sInstance;
    }

    public static boolean propLibLoaded() {
        return NetworkServices.IsPropLibLoaded();
    }
}

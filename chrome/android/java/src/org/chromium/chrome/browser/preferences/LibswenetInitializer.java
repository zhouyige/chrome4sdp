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
import android.os.AsyncTask;
import android.os.StrictMode;

import org.chromium.base.CommandLine;
import org.chromium.base.Log;
import org.chromium.chrome.browser.ChromeSwitches;
import org.chromium.chrome.browser.UpdateCheckService;
import org.chromium.content.browser.Libswenet;
import org.chromium.net.NetworkServices;

/**
 * Handler for libswenet that deals with initializing and handling
 * its settings
 */
public class LibswenetInitializer {
    private static boolean mLibswenetInitComplete = false;

    private static final String LIBSWENET_SERVICE_PREF = "libswenet_update_service";

    public static void initializeGlobalInstance(Context ctx) {
        Log.v("LIBNEXT", "initializeGlobalInstance");
        if (!mLibswenetInitComplete && Libswenet.propLibLoaded()) {
            final StrictMode.ThreadPolicy oldPolicy = StrictMode.getThreadPolicy();
            StrictMode.ThreadPolicy tmpPolicy = new StrictMode.ThreadPolicy.Builder(oldPolicy)
                    .permitDiskReads()
                    .permitDiskWrites()
                    .build();
            StrictMode.setThreadPolicy(tmpPolicy);

            mLibswenetInitComplete = true;
            Libswenet.Initialize(ctx);

            final String fileName = ctx.getApplicationInfo().dataDir + "/srp_wl.dat";
            final String url = CommandLine.getInstance().getSwitchValue(ChromeSwitches.LIBSWENET_SRP_WHITELIST);
            new UpdateCheckService(ctx, LIBSWENET_SERVICE_PREF, url , fileName,
                    new UpdateCheckService.UpdateServiceEventListener() {
                        @Override
                        public void updateComplete(boolean success) {
                            if (!success)
                                return;
                            NetworkServices.NotifyResourceFetcherDone(fileName, "libswenet");
                        }
                        @Override
                        public void updateProgress(int bytesRead) {}
                        @Override
                        public boolean overrideInterval() {
                            return false;
                        }
                    }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            StrictMode.setThreadPolicy(oldPolicy);
        }
    }
}

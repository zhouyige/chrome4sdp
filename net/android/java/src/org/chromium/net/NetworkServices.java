/*
 * Copyright (c) 2014, 2016 The Linux Foundation. All rights reserved.
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

package org.chromium.net;

import android.content.Context;
import android.util.Log;

import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.UsedByReflection;


@JNINamespace("net")
public class NetworkServices {
    static public void NotifyResourceFetcherDone(String path, String module) {
        try {
            nativeNotifyResourceFetcherDone(path, module);
        } catch (UnsatisfiedLinkError e) {
            HandleNotifyException();
        } catch (Exception e) {
            HandleNotifyException();
        }
    }
    static public boolean IsPropLibLoaded() {
        try {
            return nativeIsPropLibLoaded();
        } catch (UnsatisfiedLinkError e) {
            HandleNotifyException();
            return false;
        } catch (Exception e) {
            HandleNotifyException();
            return false;
        }
    }
    static private void HandleNotifyException() {
        Log.d("libswenet","ResourceFetcher - Resource Fetcher done message ignored");
    }

    private static native void nativeNotifyResourceFetcherDone(String path, String module);
    private static native boolean nativeIsPropLibLoaded();
}

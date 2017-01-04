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

#include "chrome/browser/android/webshield_bridge.h"

#include "base/android/jni_android.h"
#include "base/android/jni_string.h"
#include "base/android/scoped_java_ref.h"
#include "chrome/browser/xss_defender/xss_defender.h"

#include "jni/WebshieldBridge_jni.h"

namespace chrome {
namespace android {

WebshieldBridge::WebshieldBridge() {
}

WebshieldBridge::~WebshieldBridge() {
}

bool WebshieldBridge::Register(JNIEnv* env) {
  return RegisterNativesImpl(env);
}

jboolean CheckURL(JNIEnv* env, const base::android::JavaParamRef<jobject>& caller, const base::android::JavaParamRef<jstring>& url, jint type) {
  xss_defender::XSSDefender::URLResource url_resource;
  url_resource.url = base::android::ConvertJavaStringToUTF8(env, url);
  url_resource.request_type = type;
  url_resource.is_incognito = false;
  return xss_defender::XSSDefender::CheckURL(&url_resource);
}

base::android::ScopedJavaLocalRef<jstring> FilterURL(JNIEnv* env, const base::android::JavaParamRef<jobject>& caller, const base::android::JavaParamRef<jstring>& url) {
  return base::android::ConvertUTF8ToJavaString(env, xss_defender::XSSDefender::FilterURL(GURL(base::android::ConvertJavaStringToUTF8(env, url))));
}

jboolean IsMalicious(JNIEnv* env, const base::android::JavaParamRef<jobject>& caller, const base::android::JavaParamRef<jstring>& url, jint type) {
  return xss_defender::XSSDefender::IsMalicious(GURL(base::android::ConvertJavaStringToUTF8(env, url)), content::ResourceType::RESOURCE_TYPE_MAIN_FRAME, false);
}

jint GetXSSDefVersion(JNIEnv* env, const base::android::JavaParamRef<jobject>& caller) {
  return xss_defender::XSSDefender::GetVersionCode();
}

jint GetErrorType(JNIEnv* env, const base::android::JavaParamRef<jobject>& caller) {
  return xss_defender::XSSDefender::GetErrorType();
}

jboolean LoadLibrary(JNIEnv* env, const base::android::JavaParamRef<jobject>& caller) {
  return static_cast<bool>(xss_defender::XSSDefender::LoadLibrary());
}

void SendUserResponse(JNIEnv* env, const base::android::JavaParamRef<jobject>& caller, const base::android::JavaParamRef<jstring>& url, jboolean result) {
  xss_defender::XSSDefender::SendUserResponse(GURL(base::android::ConvertJavaStringToUTF8(env, url)), result);
}

} //namespace android
} //namespace chrome

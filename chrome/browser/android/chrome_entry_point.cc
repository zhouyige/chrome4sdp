// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "base/android/jni_android.h"
#include "base/bind.h"
#include "chrome/app/android/chrome_jni_onload.h"

#ifdef CALL_SWE_CORE
#include "swe/swe_core/swe_core.h"
#include "components/version_info/version_info_values.h"
#endif // CALL_SWE_CORE

namespace {

bool RegisterJNI(JNIEnv* env) {
  return true;
}

bool Init() {
  return true;
}

}  // namespace

// This is called by the VM when the shared library is first loaded.
JNI_EXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
  if (!android::OnJNIOnLoadRegisterJNI(vm, base::Bind(&RegisterJNI)) ||
      !android::OnJNIOnLoadInit(base::Bind(&Init))) {
    return -1;
  }

#ifdef CALL_SWE_CORE
  // calling into swe-core
#define STR_EXPAND(tok) #tok
#define STR(tok) STR_EXPAND(tok)
  int ret;
  if (!swe_core::check("{\"caller\":\"browser\", "
                        "\"version\":\"" SWE_PRODUCT_VERSION "\", "
                        "\"sha\":\"" SWE_LAST_CHANGE "\", "
                        "\"official\":\"" STR(IS_OFFICIAL_BUILD) "\""
                        "}", ret)) {
    exit(-1);
  }
#endif // CALL_SWE_CORE
  return JNI_VERSION_1_4;
}

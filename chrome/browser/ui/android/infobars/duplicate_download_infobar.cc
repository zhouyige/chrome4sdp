// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "chrome/browser/ui/android/infobars/duplicate_download_infobar.h"

#include <utility>

#include "base/android/jni_android.h"
#include "base/android/jni_string.h"
#include "base/memory/ptr_util.h"
#include "chrome/browser/android/download/duplicate_download_infobar_delegate.h"
#include "jni/DuplicateDownloadInfoBar_jni.h"

using chrome::android::DuplicateDownloadInfoBarDelegate;

// static
std::unique_ptr<infobars::InfoBar> DuplicateDownloadInfoBar::CreateInfoBar(
    std::unique_ptr<DuplicateDownloadInfoBarDelegate> delegate) {
  return base::WrapUnique(new DuplicateDownloadInfoBar(std::move(delegate)));
}

DuplicateDownloadInfoBar::~DuplicateDownloadInfoBar() {
}

DuplicateDownloadInfoBar::DuplicateDownloadInfoBar(
    std::unique_ptr<DuplicateDownloadInfoBarDelegate> delegate)
    : ConfirmInfoBar(std::move(delegate)) {}

base::android::ScopedJavaLocalRef<jobject>
DuplicateDownloadInfoBar::CreateRenderInfoBar(JNIEnv* env) {
  DuplicateDownloadInfoBarDelegate* delegate = GetDelegate();

  base::android::ScopedJavaLocalRef<jstring> j_file_path =
      base::android::ConvertUTF8ToJavaString(env, delegate->GetFilePath());
  jlong j_total_bytes = delegate->GetTotalBytes();
  base::android::ScopedJavaLocalRef<jstring> j_mime_type =
      base::android::ConvertUTF8ToJavaString(env, delegate->GetMimeType());
  base::android::ScopedJavaLocalRef<jstring> j_page_url =
      base::android::ConvertUTF8ToJavaString(env, delegate->GetPageURL());
  base::android::ScopedJavaLocalRef<jobject> java_infobar(
      Java_DuplicateDownloadInfoBar_createInfoBar(
          env, j_file_path, j_total_bytes, j_mime_type,
          delegate->IsOfflinePage(), j_page_url,
          delegate->IsOffTheRecord()));
  return java_infobar;
}

void DuplicateDownloadInfoBar::SetDirFullPath(
    JNIEnv* env,
    const base::android::JavaParamRef<jobject>& obj,
    const base::android::JavaParamRef<jstring>& jdir_full_path) {
  DuplicateDownloadInfoBarDelegate* delegate = GetDelegate();

  std::string dir_full_path =
      base::android::ConvertJavaStringToUTF8(env, jdir_full_path);
  delegate->SetDirFullPath(dir_full_path);
}

DuplicateDownloadInfoBarDelegate* DuplicateDownloadInfoBar::GetDelegate() {
  return static_cast<DuplicateDownloadInfoBarDelegate*>(delegate());
}

// Native JNI methods ---------------------------------------------------------

bool RegisterDuplicateDownloadInfoBarDelegate(JNIEnv* env) {
  return RegisterNativesImpl(env);
}

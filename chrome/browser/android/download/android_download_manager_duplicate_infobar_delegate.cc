// Copyright 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "chrome/browser/android/download/android_download_manager_duplicate_infobar_delegate.h"

#include <memory>

#include "base/memory/ptr_util.h"
#include "chrome/browser/android/download/chrome_download_delegate.h"
#include "chrome/browser/infobars/infobar_service.h"
#include "chrome/browser/ui/android/infobars/duplicate_download_infobar.h"
#include "components/infobars/core/infobar.h"

using base::android::ScopedJavaLocalRef;

namespace chrome {
namespace android {

AndroidDownloadManagerDuplicateInfoBarDelegate::
    ~AndroidDownloadManagerDuplicateInfoBarDelegate() {
}

// static
void AndroidDownloadManagerDuplicateInfoBarDelegate::Create(
    InfoBarService* infobar_service,
    const std::string& file_path,
    int64_t total_bytes,
    const std::string& mime_type,
    jobject chrome_download_delegate,
    jobject download_info,
    jboolean is_off_the_record) {
  infobar_service->AddInfoBar(DuplicateDownloadInfoBar::CreateInfoBar(
      base::WrapUnique(new AndroidDownloadManagerDuplicateInfoBarDelegate(
          file_path, total_bytes, mime_type, chrome_download_delegate, download_info,
          is_off_the_record))));
}

AndroidDownloadManagerDuplicateInfoBarDelegate::
    AndroidDownloadManagerDuplicateInfoBarDelegate(
        const std::string& file_path,
        int64_t total_bytes,
        const std::string& mime_type,
        jobject chrome_download_delegate,
        jobject download_info,
        jboolean is_off_the_record)
    : file_path_(file_path),
      total_bytes_(total_bytes),
      mime_type_(mime_type),
      is_off_the_record_(is_off_the_record) {
  JNIEnv* env = base::android::AttachCurrentThread();
  chrome_download_delegate_.Reset(env, chrome_download_delegate);
  download_info_.Reset(env, download_info);
}

infobars::InfoBarDelegate::InfoBarIdentifier
AndroidDownloadManagerDuplicateInfoBarDelegate::GetIdentifier() const {
  return ANDROID_DOWNLOAD_MANAGER_DUPLICATE_INFOBAR_DELEGATE;
}

bool AndroidDownloadManagerDuplicateInfoBarDelegate::Accept() {
  bool tab_closed = ChromeDownloadDelegate::EnqueueDownloadManagerRequest(
      chrome_download_delegate_.obj(), true, download_info_.obj(),
      file_path_);
  return !tab_closed;
}

bool AndroidDownloadManagerDuplicateInfoBarDelegate::Cancel() {
  return true;
}

std::string AndroidDownloadManagerDuplicateInfoBarDelegate::GetFilePath()
    const {
  return file_path_;
}

int64_t AndroidDownloadManagerDuplicateInfoBarDelegate::GetTotalBytes() const {
  return total_bytes_;
}

std::string AndroidDownloadManagerDuplicateInfoBarDelegate::GetMimeType() const {
  return mime_type_;
}

bool AndroidDownloadManagerDuplicateInfoBarDelegate::SetDirFullPath(
    const std::string& dir_full_path) {
  base::FilePath old_path(file_path_);
  std::vector<base::FilePath::StringType> components;
  old_path.GetComponents(&components);
  if (components.empty())
      return false;

  base::FilePath new_path(dir_full_path);
  new_path.Append(components.back());
  file_path_ = new_path.value();
  return true;
}

bool AndroidDownloadManagerDuplicateInfoBarDelegate::IsOffTheRecord() const {
  return is_off_the_record_;
}

}  // namespace android
}  // namespace chrome

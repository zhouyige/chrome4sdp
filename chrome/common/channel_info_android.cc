// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "chrome/common/channel_info.h"

#include "base/android/build_info.h"
#include "base/logging.h"
#include "base/strings/string_util.h"
#include "components/version_info/version_info.h"

namespace chrome {

std::string GetChannelString() {
  switch (GetChannel()) {
    case version_info::Channel::UNKNOWN: return "unknown";
    case version_info::Channel::CANARY: return "canary";
    case version_info::Channel::DEV: return "dev";
    case version_info::Channel::BETA: return "beta";
    case version_info::Channel::STABLE: return std::string();
  }
  NOTREACHED() << "Unknown channel " << static_cast<int>(GetChannel());
  return std::string();
}

version_info::Channel GetChannel() {
  const base::android::BuildInfo* bi = base::android::BuildInfo::GetInstance();
  DCHECK(bi && bi->package_name());
  const char* parsed_package_name;

  parsed_package_name = strrchr(bi->package_name(), '.');

  if (!parsed_package_name)
    return version_info::Channel::STABLE;

  if (!strcmp(parsed_package_name+1, "beta"))
    return version_info::Channel::BETA;
  if (!strcmp(parsed_package_name+1, "dev"))
    return version_info::Channel::DEV;

  return version_info::Channel::STABLE;
}

}  // namespace chrome

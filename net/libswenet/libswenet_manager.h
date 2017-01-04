// Copyright (c) 2016 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
#ifndef NET_LIBSWENET_LIBSWENET_MANAGER_H_
#define NET_LIBSWENET_LIBSWENET_MANAGER_H_

#include "libswenet_export.h"

#include <string>

namespace base {
  class MessageLoopForIO;
}

namespace net {
  class HttpCache;
}

namespace libswenet {

class LibswenetManager {
public:
    static LibswenetManager* Get();

    LIBSWENET_EXPORT static void Register(LibswenetManager*);
    LIBSWENET_EXPORT static bool SchedulePreconnect(const char* url, unsigned int count);
    LIBSWENET_EXPORT static bool ScheduleDnsResolve(const char* url);

    virtual ~LibswenetManager() { }
    virtual void ResourceFetcherDone(std::string path) { }
    virtual void PageLoadStarted(std::string url, bool isDesktopUserAgent) { }
    virtual bool IsPropLibLoaded() = 0;

    virtual void SetIoMessageLoop(base::MessageLoopForIO* message_loop) { }
    virtual base::MessageLoopForIO* GetIoMessageLoop();
    virtual void SetHttpCache(net::HttpCache* http_cache) { }
    virtual net::HttpCache* GetHttpCache();
};

} // namespace libswenet

#endif  // NET_LIBSWENET_LIBSWENET_MANAGER_H_

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

#include "libswenet_manager.h"

#include "base/logging.h"
#include "base/message_loop/message_loop.h"
#include "content/public/browser/browser_thread.h"
#include "net/base/address_list.h"
#include "net/base/host_port_pair.h"
#include "net/dns/host_resolver.h"
#include "net/http/http_cache_transaction.h"
#include "net/http/preconnect.h"
#include "net/log/net_log_with_source.h"
#include "../../base/lazy_instance.h"

namespace libswenet {

class DummyLibswenetManager :  public LibswenetManager {

public:
    DummyLibswenetManager() { }
    bool IsPropLibLoaded() override { return 0; }
    ~DummyLibswenetManager() override { }
    base::MessageLoopForIO* GetIoMessageLoop() override { return 0; }
    net::HttpCache* GetHttpCache() override { return 0; }

};

static base::LazyInstance<DummyLibswenetManager> g_dummy_libswenet_manager = LAZY_INSTANCE_INITIALIZER;
static LibswenetManager* g_registered_libswenet_manager = 0;

LibswenetManager* LibswenetManager::Get() {
    if (!g_registered_libswenet_manager)
        return g_dummy_libswenet_manager.Pointer();

    return g_registered_libswenet_manager;
}

void LibswenetManager::Register(LibswenetManager* instance) {
    g_registered_libswenet_manager = instance;
}

static void DoPreconnect(std::string* url, unsigned int count) {
  auto libswenet_manager = libswenet::LibswenetManager::Get();
  if (libswenet_manager) {
    auto http_cache = libswenet_manager->GetHttpCache();
    if (http_cache) {
      net::HttpNetworkSession* session = http_cache->GetSession();
      if (session) {
          net::Preconnect::DoPreconnect(session, GURL(*url), count);
      }
    }
  }

  delete url;
}

static void onResolveDone(int result) {
}

static void DoDnsResolve(std::string* url) {
  auto libswenet_manager = libswenet::LibswenetManager::Get();
  if (libswenet_manager) {
    auto cache = libswenet_manager->GetHttpCache();
    if (cache) {
      net::HttpNetworkSession* session = cache->GetSession();
      if (session) {
        //we allocate this only once and hence do need to delete. This might cause false memory leak alert.
        static net::AddressList* adList = new net::AddressList;

        net::HostPortPair hpPair;
        hpPair.set_host(url[0]);
        hpPair.set_port(80);
        net::HostResolver::RequestInfo reqInfo(hpPair);

        net::HostResolver* hr = session->params().host_resolver;
        std::unique_ptr<net::HostResolver::Request> request(nullptr);

        if(hr) {
          hr->Resolve(reqInfo, net::RequestPriority::DEFAULT_PRIORITY,
            adList, base::Bind(&onResolveDone), &request, net::NetLogWithSource());
        }
      }
    }
  }
  delete url;
}

bool LibswenetManager::SchedulePreconnect(const char* url, unsigned int count) {
  auto libswenet_manager = LibswenetManager::Get();
  if (libswenet_manager && url) {
    auto message_loop = libswenet_manager->GetIoMessageLoop();
    if (message_loop) {
      message_loop->task_runner()->PostTask(FROM_HERE, base::Bind(&DoPreconnect, new std::string(url), count));
      return true;
    }
  }
  return false;
}

bool LibswenetManager::ScheduleDnsResolve(const char* url) {
  auto libswenet_manager = LibswenetManager::Get();
  if (libswenet_manager && url) {
    auto message_loop = libswenet_manager->GetIoMessageLoop();
    if (message_loop) {
      message_loop->task_runner()->PostTask(FROM_HERE, base::Bind(&DoDnsResolve, new std::string(url)));
      return true;
    }
  }
  return false;
}
}

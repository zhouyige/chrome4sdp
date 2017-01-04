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

#ifndef CHROME_BROWSER_RENDERER_HOST_XSS_RESOURCE_THROTTLE_H_
#define CHROME_BROWSER_RENDERER_HOST_XSS_RESOURCE_THROTTLE_H_

#include <string>
#include <vector>

#include "base/memory/ref_counted.h"
#include "base/time/time.h"
#include "base/timer/timer.h"
#include "components/safe_browsing_db/database_manager.h"
#include "components/safe_browsing_db/util.h"
#include "chrome/browser/safe_browsing/safe_browsing_service.h"
#include "chrome/browser/safe_browsing/ui_manager.h"
#include "content/public/browser/resource_throttle.h"
#include "content/public/browser/web_contents.h"
#include "content/public/common/resource_type.h"
#include "net/log/net_log.h"

class ResourceDispatcherHost;

namespace net {
class URLRequest;
}
// Note: The ResourceThrottle interface is called in this order:
// WillStartRequest once, WillRedirectRequest zero or more times, and then
// WillProcessReponse once.
class XSSResourceThrottle
    : public content::ResourceThrottle,
      public safe_browsing::SafeBrowsingDatabaseManager::Client,
      public base::SupportsWeakPtr<XSSResourceThrottle> {
 public:
  // Will construct a XSSResourceThrottle, or return NULL
  // if on Android and not in the field trial.
  static XSSResourceThrottle* MaybeCreate(
      net::URLRequest* request,
      content::ResourceType resource_type,
      bool is_incognito);

  void ShowPage(content::WebContents* web_contents, const GURL& url);
  // content::ResourceThrottle implementation (called on IO thread):
  void WillStartRequest(bool* defer) override;
  void WillRedirectRequest(const net::RedirectInfo& redirect_info,
                           bool* defer) override;
  const char* GetNameForLogging() const override;

  typedef base::Callback<void(GURL, bool /*proceed*/)> UrlCheckCallback;

  struct UnsafeResource {
    UnsafeResource();
    UnsafeResource(const UnsafeResource& other);
    ~UnsafeResource();

    GURL url;
    GURL original_url;
    std::vector<GURL> redirect_urls;
    bool is_subresource;
    bool is_subframe;
    safe_browsing::SBThreatType threat_type;
    safe_browsing::ThreatMetadata threat_metadata;
    UrlCheckCallback callback;  // This is called back on |callback_thread|.
    scoped_refptr<base::SingleThreadTaskRunner> callback_thread;
    int render_process_host_id;
    int render_frame_id;
    safe_browsing::ThreatSource threat_source;
  };

  static void AddToWhitelist(const XSSResourceThrottle::UnsafeResource& resource);
  static bool IsWhitelisted(const XSSResourceThrottle::UnsafeResource& resource);

 protected:
  XSSResourceThrottle(const net::URLRequest* request,
                      content::ResourceType resource_type,
                      bool is_incognito);

 private:
  // Describes what phase of the check a throttle is in.
  enum State {
    // Haven't started checking or checking is complete. Not deferred.
    STATE_NONE,
    // We're displaying a blocking page. Could be deferred.
    STATE_DISPLAYING_BLOCKING_PAGE,
  };

  // Describes what stage of the request got paused by the check.
  enum DeferState {
    DEFERRED_NONE,
    DEFERRED_START,
    DEFERRED_REDIRECT,
  };

  ~XSSResourceThrottle() override;

  void OnUserResponse(GURL url, bool proceed);

  void ProcessMaliciousLink(const GURL& url);

  // Starts displaying the safe browsing interstitial page if it's not
  // prerendering. Called on the UI thread.
  static void StartDisplayingBlockingPage(
      const base::WeakPtr<XSSResourceThrottle>& throttle,
      const XSSResourceThrottle::UnsafeResource& resource);

  // Called on the IO thread if the request turned out to be for a prerendered
  // page.
  void Cancel();

  State state_;
  DeferState defer_state_;
  bool is_incognito_;

  // The redirect chain for this resource
  std::vector<GURL> redirect_urls_;

  const net::URLRequest* request_;

  DISALLOW_COPY_AND_ASSIGN(XSSResourceThrottle);
};
#endif  // CHROME_BROWSER_RENDERER_HOST_XSS_RESOURCE_THROTTLE_H_

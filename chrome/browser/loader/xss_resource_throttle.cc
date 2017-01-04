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

#include "chrome/browser/loader/xss_resource_throttle.h"

#include "base/logging.h"
#include "base/metrics/histogram_macros.h"
#include "base/values.h"
#include "chrome/browser/interstitials/xss_interstitial_page.h"
#include "chrome/browser/browser_process.h"
#include "chrome/browser/prerender/prerender_contents.h"
#include "chrome/browser/safe_browsing/safe_browsing_service.h"
#include "chrome/browser/tab_contents/tab_util.h"
#include "content/public/browser/browser_thread.h"
#include "content/public/browser/render_frame_host.h"
#include "content/public/browser/resource_controller.h"
#include "content/public/browser/resource_request_info.h"
#include "content/public/browser/web_contents.h"
#include "chrome/browser/xss_defender/xss_defender.h"
#include "net/base/load_flags.h"
#include "net/log/net_log.h"
#include "net/url_request/redirect_info.h"
#include "net/url_request/url_request.h"


namespace {

const void* const kWhitelistKey = &kWhitelistKey;

class WhitelistUrlSet : public base::SupportsUserData::Data {
 public:
  WhitelistUrlSet() {}

  bool Contains(const GURL url) {
    auto iter = set_.find(url.GetWithEmptyPath());
    return iter != set_.end();
  }

  void Insert(const GURL url) { set_.insert(url.GetWithEmptyPath()); }

 private:
  std::set<GURL> set_;

  DISALLOW_COPY_AND_ASSIGN(WhitelistUrlSet);
};

}  // namespace

XSSResourceThrottle::UnsafeResource::UnsafeResource() {}
XSSResourceThrottle::UnsafeResource::UnsafeResource(
    const UnsafeResource& other) = default;
XSSResourceThrottle::UnsafeResource::~UnsafeResource() {}

// static
XSSResourceThrottle* XSSResourceThrottle::MaybeCreate(
    net::URLRequest* request,
    content::ResourceType resource_type,
    bool is_incognito) {

 // Only check main frame URLs
  if (resource_type == content::RESOURCE_TYPE_MAIN_FRAME) {
    if (xss_defender::XSSDefender::IsMalicious(request->url(), resource_type, is_incognito))
    {
      return new XSSResourceThrottle(request, resource_type, is_incognito);
    }
  }
  return nullptr;
}

XSSResourceThrottle::XSSResourceThrottle(
    const net::URLRequest* request,
    content::ResourceType resource_type,
    bool is_incognito)
    : state_(STATE_NONE),
      defer_state_(DEFERRED_NONE),
      is_incognito_(is_incognito),
      request_(request){}

XSSResourceThrottle::~XSSResourceThrottle() {
}

void XSSResourceThrottle::ProcessMaliciousLink(const GURL& url){

  const content::ResourceRequestInfo* info =
     content::ResourceRequestInfo::ForRequest(request_);
  XSSResourceThrottle::UnsafeResource resource;
  resource.url = url;
  resource.callback = base::Bind(
      &XSSResourceThrottle::OnUserResponse, AsWeakPtr());
  resource.render_process_host_id = info->GetChildID();
  resource.render_frame_id = info->GetRenderFrameID();

  // State = Show blocking page
  state_ = STATE_DISPLAYING_BLOCKING_PAGE;
  content::BrowserThread::PostTask(
      content::BrowserThread::UI,
      FROM_HERE,
      base::Bind(&XSSResourceThrottle::StartDisplayingBlockingPage,
                 AsWeakPtr(), resource));

}

void XSSResourceThrottle::WillStartRequest(bool* defer) {
  defer_state_ = DEFERRED_START;
  if (defer)
    *defer = true;
  ProcessMaliciousLink(request_->url());
}

void XSSResourceThrottle::WillRedirectRequest(
    const net::RedirectInfo& redirect_info,
    bool* defer) {
  CHECK_EQ(defer_state_, DEFERRED_NONE);
  if (xss_defender::XSSDefender::IsMalicious(redirect_info.new_url,
                                             content::RESOURCE_TYPE_MAIN_FRAME,
                                             is_incognito_)) {
    defer_state_ = DEFERRED_REDIRECT;
    if (defer)
      *defer = true;
    ProcessMaliciousLink(redirect_info.new_url);
  }
  else {
    if (defer)
      *defer = false;
  }
}


const char* XSSResourceThrottle::GetNameForLogging() const {
  return "XSSResourceThrottle";
}

void XSSResourceThrottle::StartDisplayingBlockingPage(
    const base::WeakPtr<XSSResourceThrottle>& throttle,
    const XSSResourceThrottle::UnsafeResource& resource) {

  if (IsWhitelisted(resource)) {
     if (!resource.callback.is_null()) {
         content::BrowserThread::PostTask(content::BrowserThread::IO, FROM_HERE,
             base::Bind(resource.callback, resource.url, true));
      }
      return;
  }

  content::RenderFrameHost* rvh = content::RenderFrameHost::FromID(
      resource.render_process_host_id, resource.render_frame_id);
  if (rvh) {
    content::WebContents* web_contents =
        content::WebContents::FromRenderFrameHost(rvh);
    prerender::PrerenderContents* prerender_contents =
        prerender::PrerenderContents::FromWebContents(web_contents);

    if (prerender_contents) {
      prerender_contents->Destroy(prerender::FINAL_STATUS_SAFE_BROWSING);
    } else {
      XSSInterstitialPage* xss_page = new XSSInterstitialPage(web_contents, resource);
      xss_page->Show();
      return;
    }
  }
  // Tab is gone or it's being prerendered.
  content::BrowserThread::PostTask(
      content::BrowserThread::IO,
      FROM_HERE,
      base::Bind(&XSSResourceThrottle::Cancel, throttle));
}

void XSSResourceThrottle::Cancel() {
  controller()->Cancel();
}

// called on the IO thread when the user has decided to
// proceed with the current request, or go back.
void XSSResourceThrottle::OnUserResponse(GURL url, bool proceed) {
  CHECK_EQ(state_, STATE_DISPLAYING_BLOCKING_PAGE);
  state_ = STATE_NONE;
  // Send URL and User response if it is not incognito tab
  if (!is_incognito_)
    xss_defender::XSSDefender::SendUserResponse(url, proceed);

  if (proceed) {
    if (defer_state_ != DEFERRED_NONE) {
      controller()->Resume();
      defer_state_ = DEFERRED_NONE;
    }
  } else {
    controller()->Cancel();
  }
}

// Whitelist this domain in the current WebContents. Either add the
// domain to an existing WhitelistUrlSet, or create a new WhitelistUrlSet.
void XSSResourceThrottle::AddToWhitelist(
    const XSSResourceThrottle::UnsafeResource& resource) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);

  content::WebContents* web_contents = tab_util::GetWebContentsByFrameID(
      resource.render_process_host_id, resource.render_frame_id);

  WhitelistUrlSet* site_list =
      static_cast<WhitelistUrlSet*>(web_contents->GetUserData(kWhitelistKey));
  if (!site_list) {
    site_list = new WhitelistUrlSet;
    web_contents->SetUserData(kWhitelistKey, site_list);
  }

  GURL whitelisted_url = resource.url;
  site_list->Insert(whitelisted_url);
}

// Check if the user has already ignored a SB warning for this WebContents and
// top-level domain.
bool XSSResourceThrottle::IsWhitelisted(
    const XSSResourceThrottle::UnsafeResource& resource) {
  DCHECK_CURRENTLY_ON(content::BrowserThread::UI);

  content::WebContents* web_contents = tab_util::GetWebContentsByFrameID(
      resource.render_process_host_id, resource.render_frame_id);

  GURL maybe_whitelisted_url = resource.url;

  WhitelistUrlSet* site_list =
      static_cast<WhitelistUrlSet*>(web_contents->GetUserData(kWhitelistKey));
  if (!site_list)
    return false;
  return site_list->Contains(maybe_whitelisted_url);
}


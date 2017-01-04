/*
 * Copyright (c) 2015, The Linux Foundation. All rights reserved.
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

#ifndef WEB_REFINER_H_
#define WEB_REFINER_H_

#include <vector>

#include "base/memory/ref_counted.h"
#include "content/public/common/web_refiner_export.h"
#include "url/gurl.h"

namespace net {
class URLRequest;
class HttpResponseHeaders;
class HttpRequestHeaders;
class CanonicalCookie;
class CookieOptions;
typedef std::vector<CanonicalCookie> CookieList;
}

namespace content {

class RenderProcessHost;
class RenderFrameHost;
class RenderThread;
class RenderView;
class WebContents;

class WebRefiner {
public:
    WEB_REFINER_EXPORT static WebRefiner* Get();
    WEB_REFINER_EXPORT static void Register(WebRefiner*);

    virtual ~WebRefiner() { }

    virtual void RegisterRenderViewImpl(RenderView* render_view) = 0;

    virtual void RegisterRenderThreadImpl(content::RenderThread* render_thread) = 0;

    virtual void RegisterRenderProcessHostImpl(RenderProcessHost* render_process_host) = 0;

    virtual void RegisterRenderFrameHostImpl(RenderFrameHost* render_frame_host) = 0;

    virtual void UnRegisterRenderFrameHostImpl(RenderFrameHost* render_frame_host) = 0;

    virtual void RegisterWebContentsImpl(content::WebContents* web_contents_impl) = 0;

    virtual void OnBeforeURLRequest(net::URLRequest* request, GURL* new_url, int* out_response_code) = 0;

    virtual void OnBeforeSendHeaders(net::URLRequest* request, net::HttpRequestHeaders* headers) = 0;

    virtual void OnHeadersReceived(net::URLRequest* request, const net::HttpResponseHeaders*, scoped_refptr<net::HttpResponseHeaders>*) = 0;

    virtual void OnCompleted(net::URLRequest* request, bool started) = 0;

    virtual bool OnCanGetCookies(const net::URLRequest* request, const net::CookieList& cookie_list) = 0;

    virtual bool AllowGetCookies(int process_id, int routing_id, int render_frame_id, const GURL& cookie_url,
                                    const GURL& first_party_url, const net::CookieList& cookie_list) = 0;

    virtual bool OnCanSetCookie(const net::URLRequest* request, const std::string& cookie_line, net::CookieOptions* options) = 0;

    virtual bool AllowSetCookie(int process_id, int routing_id, int render_frame_id, const GURL& cookie_url,
                                    const GURL& first_party_url, const std::string& cookie_line, net::CookieOptions* options) = 0;
    virtual void OnWebActivity(int process_id, int routing_id, int render_frame_id, const GURL& url,
                                    const GURL& fp_url, int type) = 0;

};

} // namespace content

#endif // WEB_REFINER_H_
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

#include "public/web/WebActivitiesForward.h"

#include "public/platform/WebActivities.h"

#include "core/dom/Document.h"
#include "core/html/HTMLCanvasElement.h"
#include "core/loader/FrameLoaderClient.h"

namespace blink {

class WebActivitiesForwardImpl : public WebActivitiesForward {
  void notifyCanvasActivity(v8::Isolate* isolate,
                            Document* doc,
                            const HTMLCanvasElement* canvas,
                            int activityType) override {
    Document* docPtr(doc);
    WebDocument webDoc(docPtr);
    WebActivitiesClient* client =
        doc->frame()->loader().client()->webActivitiesClient();
    WebActivities::get()->onCanvasActivity(isolate, webDoc, doc->activityData(),
                                           canvas->activityData(), client,
                                           activityType);
  }

  void notifyLocalStorageActivity(LocalFrame* sourceFrame,
                                  blink::WebStorageArea* storageArea) override {
    Document* docPtr(sourceFrame->document());
    WebDocument webDoc(docPtr);
    WebActivitiesClient* client =
        sourceFrame->loader().client()->webActivitiesClient();
    WebActivities::get()->onLocalStorageActivity(webDoc, storageArea, client);
  }
};

static WebActivitiesForward* g_web_activities_forward_impl = 0;
WebActivitiesForward* WebActivitiesForward::get() {
  if (!g_web_activities_forward_impl)
    g_web_activities_forward_impl = new WebActivitiesForwardImpl;
  return g_web_activities_forward_impl;
}

}  //namespace blink {

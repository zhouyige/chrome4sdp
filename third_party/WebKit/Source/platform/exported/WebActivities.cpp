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

#define BLINK_IMPLEMENTATION 1
#define INSIDE_BLINK 1

#include "public/platform/WebActivities.h"

#include "core/dom/Document.h"
#include "core/html/HTMLCanvasElement.h"
#include "core/loader/FrameLoaderClient.h"

#include "public/web/WebActivitiesForward.h"

namespace blink {

class MockWebActivities : public WebActivities {
 public:
  MockWebActivities() {}
  void createActivityData(WebActivityData* data,
                          WebActivityDataType type) override {}
  void resetActivityData(WebActivityData* data,
                         WebActivityDataType type) override {}
  void destroyActivityData(WebActivityData* data,
                           WebActivityDataType type) override {}
  void onCanvasActivity(v8::Isolate* isolate,
                        WebDocument& doc,
                        WebActivityData docActivityData,
                        WebActivityData canvasActivityData,
                        WebActivitiesClient* client,
                        int drawOp) override {}
  void onLocalStorageActivity(WebDocument& doc,
                              blink::WebStorageArea* storageArea,
                              WebActivitiesClient* client) override {}

 protected:
  ~MockWebActivities() override {}
};

static WebActivities* g_mock_web_activities = 0;
static WebActivities* g_registered_web_activities = 0;

void WebActivities::set(WebActivities* instance) {
  g_registered_web_activities = instance;
}

WebActivities* WebActivities::get() {
  if (g_registered_web_activities) {
    return g_registered_web_activities;
  } else if (!g_mock_web_activities) {
    g_mock_web_activities = new MockWebActivities;
  }
  return g_mock_web_activities;
}

static WebActivitiesForward* g_web_activities_forward = 0;
void WebActivities::setForward(WebActivitiesForward* instance) {
  g_web_activities_forward = instance;
}

void WebActivities::notifyCanvasActivity(v8::Isolate* isolate,
                                         Document* doc,
                                         const HTMLCanvasElement* canvas,
                                         int activityType) {
  if (g_web_activities_forward)
    g_web_activities_forward->notifyCanvasActivity(isolate, doc, canvas,
                                                   activityType);
}

void WebActivities::notifyLocalStorageActivity(
    LocalFrame* sourceFrame,
    blink::WebStorageArea* storageArea) {
  if (g_web_activities_forward)
    g_web_activities_forward->notifyLocalStorageActivity(sourceFrame,
                                                         storageArea);
}

}  //namespace blink {

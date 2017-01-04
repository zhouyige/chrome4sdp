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

#ifndef WebActivities_h
#define WebActivities_h

#include "public/platform/WebCommon.h"
#include "public/web/WebDocument.h"

namespace v8 {
class Isolate;
}

namespace blink {

typedef void* WebActivityData;
class WebActivitiesForward;
class WebStorageArea;
class Document;
class LocalFrame;
class HTMLCanvasElement;

enum WebActivityDataType { DOMDocumentType, CanvasElement };

class WebActivitiesClient {
 public:
  virtual void onWebActivity(const WebURL& url, const WebURL& fpUrl, int type) {
  }

 protected:
  ~WebActivitiesClient() {}
};

class WebActivities {
 public:
  BLINK_PLATFORM_EXPORT static void set(WebActivities* instance);
  BLINK_PLATFORM_EXPORT static void setForward(WebActivitiesForward* instance);

  virtual void createActivityData(WebActivityData* data,
                                  WebActivityDataType type) = 0;
  virtual void resetActivityData(WebActivityData* data,
                                 WebActivityDataType type) = 0;
  virtual void destroyActivityData(WebActivityData* data,
                                   WebActivityDataType type) = 0;
  virtual void onCanvasActivity(v8::Isolate* isolate,
                                WebDocument& doc,
                                WebActivityData docActivityData,
                                WebActivityData canvasActivityData,
                                WebActivitiesClient* client,
                                int drawOp) = 0;
  virtual void onLocalStorageActivity(WebDocument& doc,
                                      WebStorageArea* storageArea,
                                      WebActivitiesClient* client) = 0;

#if INSIDE_BLINK
  BLINK_COMMON_EXPORT static WebActivities* get();
  BLINK_COMMON_EXPORT static void notifyCanvasActivity(
      v8::Isolate* isolate,
      Document* doc,
      const HTMLCanvasElement* canvas,
      int drawOp);
  BLINK_COMMON_EXPORT static void notifyLocalStorageActivity(
      LocalFrame* sourceFrame,
      WebStorageArea* storageArea);
#endif

 protected:
  virtual ~WebActivities() {}
};

}  // namespace blink

#endif  // WebActivities_h

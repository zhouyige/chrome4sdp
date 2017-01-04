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

#ifndef CONTENT_XSS_DEFENDER_H_
#define CONTENT_XSS_DEFENDER_H_

#include <string>
#include "content/public/common/resource_type.h"
#include "url/gurl.h"

namespace xss_defender {

class XSSDefender {
public:
  XSSDefender();
  ~XSSDefender();
  enum ErrorType {
    NONE,
    LOAD_ERR,
    CheckURL_ERR,
    FilterURL_ERR,
    GetVersionCode_ERR,
    SetUserResponse_ERR,
  };
  struct URLResource {
    std::string url;
    short request_type;
    bool is_incognito;
    bool allow;
  };

  static bool CheckURL(const URLResource* url_resource);
  static std::string FilterURL(const GURL& URL);
  static ErrorType GetErrorType();
  static int GetVersionCode();
  static bool IsEnabled();
  static bool IsMalicious(const GURL& URL, const content::ResourceType& type, const bool is_incognito);
  static void* LoadLibrary();
  static void SendUserResponse(const GURL& URL, const bool result);

private:
  static std::string DecodeURL(const std::string& URL);
};

}
#endif

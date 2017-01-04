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

#include "xss_defender.h"
#include "xss_defender_log.h"
#include <dlfcn.h>
#include <stdlib.h>

#if defined(OS_ANDROID)
#include "base/command_line.h"
#include "chrome/common/chrome_switches.h"
// Decode URL headers
#include "url/url_canon.h"
#include "url/url_util.h"
#include "base/strings/utf_string_conversions.h"
// Java JNI
#include <jni.h>
#include "base/android/jni_android.h"
#include "base/android/jni_array.h"
#include "base/android/jni_string.h"
#include "base/android/jni_weak_ref.h"
#include "base/strings/string_util.h"

#include "chrome/browser/jni_headers/chrome/jni/XSSDefenderHandler_jni.h"

using base::android::ScopedJavaLocalRef;

namespace xss_defender {

XSSDefender::XSSDefender() {}
XSSDefender::~XSSDefender() {}

// Update library min version if API changes
const int MIN_SUPPORTED_VERSION = 20;
const int XSSUrlMinPathLengthCheck = 20;
static void* library_handle_ = NULL;
// Library API
typedef int (*CheckURL)(const XSSDefender::URLResource*);
typedef std::string (*FilterURL)(const XSSDefender::URLResource*);
typedef int (*GetVersionCode)();
typedef void (*SendUserResponse)(const XSSDefender::URLResource*);
static CheckURL CheckURL_f = NULL;
static FilterURL FilterURL_f = NULL;
static GetVersionCode GetVersionCode_f = NULL;
static SendUserResponse SendUserResponse_f = NULL;
static XSSDefender::ErrorType error_type = XSSDefender::ErrorType::NONE;

// Load Model Library
void* XSSDefender::LoadLibrary() {
  // Load once and don't try loading again if last load failed
  if (!library_handle_ && (error_type != ErrorType::LOAD_ERR)) {
    XSSLOGT("Load Library");
    // Get dynamic path from Java context
    JNIEnv* env = base::android::AttachCurrentThread();
    base::android::ScopedJavaLocalRef<jstring> jpath =
        xss_defender::Java_XSSDefenderHandler_getLibraryFullPath(env);
    std::string path = ConvertJavaStringToUTF8(env, jpath);
    // Library is loaded once and pointer is cached
    if (!path.empty())
      library_handle_ = dlopen(path.c_str(), RTLD_NOW);

    const char* err = dlerror();
    if (err != NULL && strlen(err) > 0) {
      XSSLOGT("Error Could not load library from (%s) Error msg: %s", path.c_str(), err);
      error_type = ErrorType::LOAD_ERR;
      // Load old library if the updated one fails
      jpath = xss_defender::Java_XSSDefenderHandler_getLibraryFullPathByName(
          env, Java_XSSDefenderHandler_getOldLibraryName(env).obj());
      path = ConvertJavaStringToUTF8(env, jpath);
      library_handle_ = dlopen(path.c_str(), RTLD_NOW);
    }

    if (library_handle_) {
      int version = XSSDefender::GetVersionCode();
      xss_defender::Java_XSSDefenderHandler_setLibraryVersion(env, version);
      XSSLOGD("Library version:%i successfully loaded from: %s", version, path.c_str());
      if (version <  MIN_SUPPORTED_VERSION) {
        dlclose(library_handle_);
        library_handle_ = NULL;
        error_type = ErrorType::LOAD_ERR;
        XSSLOGD("Error: Library Closed. Version:%i is not supported, Min required version:%i",
                version, MIN_SUPPORTED_VERSION);
      }
    }
  }
  return library_handle_;
}

// Call XSS Defender Library to Check URL
bool XSSDefender::CheckURL(const XSSDefender::URLResource* url_resource) {
  if (library_handle_) {
    // Lookup the function address and save it the first time only
    if (!CheckURL_f) {
      dlerror(); //clear errors
      CheckURL_f = (int (*)(const XSSDefender::URLResource*)) dlsym(library_handle_, "CheckURL");
      const char* err = dlerror();
      if (err != NULL && strlen(err) > 0) {
        XSSLOGT("Error CheckURL is not found in library! Error: %s", err);
        error_type = ErrorType::CheckURL_ERR;
      }
    }
    if (CheckURL_f)
      return (*CheckURL_f)(url_resource);
  }
  else {
    XSSLOGT("Error: Tring to call CheckURL while library is not loaded!");
  }
  return false;
}

// Used to filter the query in frames and Iframes
// Called from chrome_network_delegate.cc
std::string XSSDefender::FilterURL(const GURL& URL) {
  // Assumes registered library has been called before this
  if (library_handle_) {
    // Lookup the function address and save it the first time only
    if (!FilterURL_f) {
      dlerror(); //clear errors
      FilterURL_f = (std::string (*)(const XSSDefender::URLResource*))
          dlsym(library_handle_, "FilterURL");
      const char* err = dlerror();
      if (err != NULL && strlen(err) > 0) {
        XSSLOGT("Error FilterURL is not found in library! Error: %s", err);
        error_type = ErrorType::FilterURL_ERR;
      }
    }
    if (FilterURL_f && URL.has_query()) {
      XSSDefender::URLResource url_resource;
      url_resource.url = URL.spec();
      std::string filtered_url = (*FilterURL_f)(&url_resource);
      if (!filtered_url.empty()) {
        // Make sure the domain name has not changed after filtering
        if (filtered_url.compare(0, URL.GetOrigin().spec().length(),
               URL.GetOrigin().spec()) == 0) {
          return filtered_url;
        }
      }
      else {
        return URL.spec();
      }
    }
  }
  else {
    XSSLOGT("Error: Tring to call FilterURL while library is not loaded!");
  }
  return URL.spec();
}

// Call XSS Defender Library to Check URL
int XSSDefender::GetVersionCode() {
  if (library_handle_) {
    // Lookup the function address and save it the first time only
    if (!GetVersionCode_f) {
      dlerror(); //clear errors
      GetVersionCode_f = (int (*)()) dlsym(library_handle_, "GetVersionCode");
      const char* err = dlerror();
      if (err != NULL && strlen(err) > 0) {
        XSSLOGT("Error GetVersionCode is not found in library! Error: %s", err);
        error_type = ErrorType::GetVersionCode_ERR;
      }
    }
    if (GetVersionCode_f)
      return (*GetVersionCode_f)();
  }
  else {
    XSSLOGT("Error: Tring to call GetVersionCode while library is not loaded!");
  }
  return 0;
}

// Called after the user selects proceed or return from the interstitial page
// Mainframe links only, not called in incognito tabs
void XSSDefender::SendUserResponse(const GURL& URL, const bool user_response) {
    std::string decoded_url = DecodeURL(URL.spec());
    XSSDefender::URLResource url_resource;
    url_resource.url = decoded_url;
    url_resource.allow = user_response;

  if (library_handle_) {
    // Lookup the function address and save it the first time only
    if (!SendUserResponse_f) {
      dlerror(); //clear errors
      SendUserResponse_f = (void (*)(const XSSDefender::URLResource*))
          dlsym(library_handle_, "SendUserResponse");
      const char* err = dlerror();
      if (err != NULL && strlen(err) > 0) {
        XSSLOGT("Error SendUserResponse is not found in library! Error: %s", err);
        error_type = ErrorType::SetUserResponse_ERR;
      }
    }
    if (SendUserResponse_f)
      return (*SendUserResponse_f)(&url_resource);
  }
  else {
    XSSLOGT("Error: Tring to call SendUserResponse while library is not loaded!");
  }
}

// Decode URL
std::string XSSDefender::DecodeURL(const std::string& URL) {
  url::RawCanonOutputW<1024> decoded_url;
  url::DecodeURLEscapeSequences(URL.c_str(), URL.length(), &decoded_url);
  return base::UTF16ToUTF8(base::string16(decoded_url.data(),decoded_url.length()));
}

// Main API called from outside class to load library and check URL
bool XSSDefender::IsMalicious(const GURL& URL, const content::ResourceType& type, bool is_incognito) {
  LoadLibrary();

  if (library_handle_ && URL.is_valid() && URL.SchemeIsHTTPOrHTTPS() &&
      (URL.has_query() || (URL.has_path() && URL.path().length() > XSSUrlMinPathLengthCheck))) {
    std::string decoded_url = DecodeURL(URL.spec());
    XSSDefender::URLResource url_resource;
    url_resource.url = decoded_url;
    url_resource.request_type = type;
    url_resource.is_incognito = is_incognito;
    return XSSDefender::CheckURL(&url_resource);
  }
  return false;
}

// Check if XSS-Defender is disabled from commandline
// GUI settings should be also checkd using the context
bool XSSDefender::IsEnabled() {
  if (!base::CommandLine::ForCurrentProcess()->HasSwitch(
      switches::kDisableXssDefender)) {
    return true;
  }
  return false;
}

XSSDefender::ErrorType XSSDefender::GetErrorType() {
  return error_type;
}

}//namespace

#else // !Android OS
namespace xss_defender {
XSSDefender::XSSDefender() {}
XSSDefender::~XSSDefender() {}
bool XSSDefender::CheckURL(const URLResource* url_resource) { return false; }
std::string XSSDefender::FilterURL(const GURL& URL) { return URL.spec(); }
XSSDefender::ErrorType GetErrorType() { return XSSDefender::ErrorType::NONE; }
int XSSDefender::GetVersionCode() { return 0; }
bool XSSDefender::IsEnabled() { return false; }
bool XSSDefender::IsMalicious(const GURL& URL, const content::ResourceType& type, const bool is_incognito) { return false; }
void* XSSDefender::LoadLibrary() { return 0; }
void XSSDefender::SendUserResponse(const GURL& URL, const bool result) { }
}//namespace
#endif

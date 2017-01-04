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

#ifndef XSS_DEFENDER_LOG_H_
#define XSS_DEFENDER_LOG_H_

#include "build/build_config.h"

#if defined(OS_ANDROID)

#include <android/log.h>
#define XSSD_LOG_TAG        "XSSDefender"
#define XSS_DEFENDER_LOG_TAG   "XSSDefenderStat"


#define ENABLE_DEFENDER_LOGS
//#define LOCAL_TRACE

#ifdef ENABLE_DEFENDER_LOGS
#define XSSLOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, XSSD_LOG_TAG, __VA_ARGS__);
#define XSSLOGI(...)  __android_log_print(ANDROID_LOG_INFO, XSSD_LOG_TAG, __VA_ARGS__);
#define XSSLOGT(...)  __android_log_print(ANDROID_LOG_ERROR, XSSD_LOG_TAG, __VA_ARGS__);
#else
#define XSSLOGI(...)
#define XSSLOGD(...)
#define XSSLOGT(...)
#endif // ENABLE_DEFENDER_LOGS

// Always on
#define XSSLOGSTAT(...)  __android_log_print(ANDROID_LOG_DEBUG, XSS_DEFENDER_LOG_TAG, __VA_ARGS__);
#define XSSLOG_E(...)  __android_log_print(ANDROID_LOG_ERROR, XSSD_LOG_TAG, __VA_ARGS__);
#define XSSLOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE, XSSD_LOG_TAG, __VA_ARGS__);

#ifdef LOCAL_TRACE
    class LTrace {
        public:
            LTrace(char * name, void * obj): name_(name), obj_(obj) {
                char * fmt = (char *) "+++ BEGIN %s (%x)";
                __android_log_print(ANDROID_LOG_DEBUG, XSSD_LOG_TAG, fmt, name_, obj_);
            }
            ~LTrace() {
                char * fmt = (char*) "+++ END %s (%x)";
                 __android_log_print(ANDROID_LOG_DEBUG, XSSD_LOG_TAG, fmt, name_, obj_);
            }
        private:
            char * name_;
            void * obj_;
    };
    #define LTRACE(a) LTrace _ltrace_((char *)(a), (void *) (this))
    #define LTRACEF(a) LTrace _ltrace_((char *)(a), NULL)
#else
    #define LTRACE(a)
    #define LTRACEF(a)
#endif

#else //#if defined(OS_ANDROID)

#define XSSLOGD(...)
#define XSSLOGT(...)
#define XSSLOGI(...)
#define XSSLOGSTAT(...)

#endif //defined(OS_ANDROID)

#endif //XSS_DEFENDER_LOG_H_

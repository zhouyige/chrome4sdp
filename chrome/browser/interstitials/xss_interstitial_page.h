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

#ifndef CHROME_BROWSER_INTERSTITIALS_XSS_INTERSTITIAL_PAGE_H_
#define CHROME_BROWSER_INTERSTITIALS_XSS_INTERSTITIAL_PAGE_H_

#include "base/strings/string16.h"
#include "chrome/browser/safe_browsing/safe_browsing_service.h"
#include "chrome/browser/loader/xss_resource_throttle.h"
#include "content/public/browser/interstitial_page_delegate.h"
#include "content/public/browser/resource_throttle.h"
#include "url/gurl.h"

namespace base {
class DictionaryValue;
}

namespace content {
class InterstitialPage;
class WebContents;
}

namespace security_interstitials {
class MetricsHelper;
}

namespace interstitials {
const char kBoxChecked[] = "boxchecked";
const char kDisplayCheckBox[] = "displaycheckbox";
const char kOptInLink[] = "optInLink";
const char kPrivacyLinkHtml[] =
    "<a id=\"privacy-link\" href=\"\" onclick=\"sendCommand(%d); "
    "return false;\" onmousedown=\"return false;\">%s</a>";
}

class XSSInterstitialPage :
  public content::InterstitialPageDelegate {

 public:
  // These represent the commands sent from the interstitial JavaScript.
  // DO NOT reorder or change these without also changing the JavaScript!
  // See chrome/browser/resources/security_warnings/interstitial_v2.js
  enum SecurityInterstitialCommands {
    // Used by tests
    CMD_ERROR = -3,
    CMD_TEXT_FOUND = -2,
    CMD_TEXT_NOT_FOUND = -1,
    // Decisions
    CMD_DONT_PROCEED = 0,
    CMD_PROCEED = 1,
    // Ways for user to get more information
    CMD_SHOW_MORE_SECTION = 2,
    CMD_OPEN_HELP_CENTER = 3,
    CMD_OPEN_DIAGNOSTIC = 4,
    // Primary button actions
    CMD_RELOAD = 5,
    CMD_OPEN_DATE_SETTINGS = 6,
    CMD_OPEN_LOGIN = 7,
    // Safe Browsing Extended Reporting
    CMD_DO_REPORT = 8,
    CMD_DONT_REPORT = 9,
    CMD_OPEN_REPORTING_PRIVACY = 10,
    // Report a phishing error
    CMD_REPORT_PHISHING_ERROR = 11,
  };

  XSSInterstitialPage(content::WebContents* web_contents,
                XSSResourceThrottle::UnsafeResource resource);
  ~XSSInterstitialPage() override;

  // Creates an interstitial and shows it.
  void Show();

 protected:
  // Populates the strings used to generate the HTML from the template.
  void PopulateInterstitialStrings(
      base::DictionaryValue* load_time_data);

  // InterstitialPageDelegate method:
  std::string GetHTMLContents() override;

  content::InterstitialPage* interstitial_page() const;
  content::WebContents* web_contents() const;
  GURL request_url() const;

  void CommandReceived(const std::string& command) override;
  void OnProceed() override;
  void OnDontProceed() override;

 private:
  // The WebContents with which this interstitial page is
  // associated. Not available in ~XSSInterstitialPage, since it
  // can be destroyed before this class is destroyed.
  content::WebContents* web_contents_;
  XSSResourceThrottle::UnsafeResource resource_;
  const GURL request_url_;
  // Once shown, |interstitial_page| takes ownership of this
  content::InterstitialPage* interstitial_page_;

  DISALLOW_COPY_AND_ASSIGN(XSSInterstitialPage);
};

#endif  // CHROME_BROWSER_INTERSTITIALS_XSS_INTERSTITIAL_PAGE_H_

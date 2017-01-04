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

#include "base/memory/weak_ptr.h"

#include "chrome/browser/interstitials/xss_interstitial_page.h"

#include "base/i18n/rtl.h"

#include "base/command_line.h"
#include "base/metrics/histogram.h"
#include "base/strings/utf_string_conversions.h"
#include "base/values.h"
#include "chrome/browser/browser_process.h"
#include "chrome/browser/net/referrer.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/common/chrome_switches.h"
#include "chrome/common/pref_names.h"
#include "chrome/common/url_constants.h"
#include "chrome/grit/browser_resources.h"
#include "chrome/grit/swe_strings.h"
#include "chrome/grit/generated_resources.h"
#include "components/google/core/browser/google_util.h"
#include "components/grit/components_resources.h"
#include "components/security_interstitials/core/metrics_helper.h"
#include "components/url_formatter/url_formatter.h"
#include "content/public/browser/interstitial_page.h"
#include "content/public/browser/page_navigator.h"
#include "content/public/browser/web_contents.h"
#include "ui/base/l10n/l10n_util.h"
#include "ui/base/resource/resource_bundle.h"
#include "ui/base/webui/jstemplate_builder.h"
#include "ui/base/webui/web_ui_util.h"

#include "content/public/browser/browser_thread.h"
#include "base/strings/string_number_conversions.h"

using content::OpenURLParams;
using content::Referrer;

const char kXssDefenderInfoUrlDefault[] = "https://webtech-devx.qualcomm.com/swe/docs/articles/en/xss-defender.html";

XSSInterstitialPage::XSSInterstitialPage(
    content::WebContents* web_contents,
    XSSResourceThrottle::UnsafeResource resource)
    : web_contents_(web_contents),
      resource_(resource),
      request_url_(resource.url),
      interstitial_page_(NULL) {
  // Creating interstitial_page_ without showing it leaks memory, so don't
  // create it here.
}

XSSInterstitialPage::~XSSInterstitialPage() {
}

content::InterstitialPage* XSSInterstitialPage::interstitial_page() const {
  return interstitial_page_;
}

content::WebContents* XSSInterstitialPage::web_contents() const {
  return web_contents_;
}

GURL XSSInterstitialPage::request_url() const {
  return request_url_;
}

void XSSInterstitialPage::Show() {
  DCHECK(!interstitial_page_);
  interstitial_page_ = content::InterstitialPage::Create(
      web_contents_, true, request_url_, this);
  interstitial_page_->Show();
}


void XSSInterstitialPage::OnDontProceed() {
  if (!resource_.callback.is_null()) {
    content::BrowserThread::PostTask(
        content::BrowserThread::IO, FROM_HERE, base::Bind(resource_.callback,
        request_url_, false));
  }
}
void XSSInterstitialPage::OnProceed() {
  if (!resource_.callback.is_null()) {
    content::BrowserThread::PostTask(
        content::BrowserThread::IO, FROM_HERE, base::Bind(resource_.callback,
        request_url_, true));
  }
  XSSResourceThrottle::AddToWhitelist(resource_);
}

void XSSInterstitialPage::CommandReceived(const std::string& page_cmd) {
  if (page_cmd == "\"pageLoadComplete\"") {
    // content::WaitForRenderFrameReady sends this message when the page
    // load completes. Ignore it.
    return;
  }

  int command = 0;
  base::StringToInt(page_cmd, &command);
  switch (command) {
    case CMD_PROCEED: {
        // User pressed on the button to proceed.
        interstitial_page()->Proceed();
        // |this| has been deleted after Proceed() returns.
        break;
    }
      // If the user can't proceed, fall through to CMD_DONT_PROCEED.
    case CMD_DONT_PROCEED: {
      // User pressed on the button to return to safety.
      interstitial_page()->DontProceed();
      // |this| has been deleted after DontProceed() returns.
      break;
    }
    case CMD_SHOW_MORE_SECTION: {
      // User asked for info on XSS attacks
      GURL xss_attack_info_url(kXssDefenderInfoUrlDefault);
      // Use help URL from command line if provided
      const base::CommandLine* command_line = base::CommandLine::ForCurrentProcess();
      if (command_line->HasSwitch(switches::kXssDefenderInfoUrl)) {
        std::string xss_attack_info_str = command_line->GetSwitchValueASCII(switches::kXssDefenderInfoUrl);
        GURL xss_attack_info_url2(xss_attack_info_str.data());
        xss_attack_info_url.Swap(&xss_attack_info_url2);
      }
      xss_attack_info_url = google_util::AppendGoogleLocaleParam(
        xss_attack_info_url, g_browser_process->GetApplicationLocale());
      OpenURLParams params(xss_attack_info_url,
                           Referrer(),
                           WindowOpenDisposition::CURRENT_TAB,
                           ui::PAGE_TRANSITION_LINK,
                           false);
      web_contents()->OpenURL(params);
      break;
    }
    default:{
      break;
    }
  } // Switch
}

std::string XSSInterstitialPage::GetHTMLContents() {
  base::DictionaryValue load_time_data;
  PopulateInterstitialStrings(&load_time_data);
  const std::string& app_locale = g_browser_process->GetApplicationLocale();
  webui::SetLoadTimeDataDefaults(app_locale, &load_time_data);
  std::string html = ResourceBundle::GetSharedInstance()
                         .GetRawDataResource(IDR_XSS_INTERSTITIAL_HTML)
                         .as_string();

  webui::AppendWebUiCssTextDefaults(&html);
  return webui::GetI18nTemplateHtml(html, &load_time_data);
}

void XSSInterstitialPage::PopulateInterstitialStrings(base::DictionaryValue* load_time_data) {
  CHECK(load_time_data);

  load_time_data->SetString("type", "XSSDEFENDER");
  load_time_data->SetString(
      "tabTitle", l10n_util::GetStringUTF16(IDS_SAFEBROWSING_V3_TITLE));

  load_time_data->SetString(
      "primaryButtonText",
      l10n_util::GetStringUTF16(IDS_SAFEBROWSING_OVERRIDABLE_SAFETY_BUTTON));

  load_time_data->SetString("heading",
      l10n_util::GetStringUTF16(IDS_XSS_V1_HEADING));

  load_time_data->SetString(
      "primaryParagraph",
      l10n_util::GetStringUTF16(IDS_XSS_V1_PRIMARY_PARAGRAPH));

  load_time_data->SetString(
      "explanationParagraph",
      l10n_util::GetStringUTF16(IDS_XSS_V1_EXPLANATION_PARAGRAPH_LINK) +
      l10n_util::GetStringUTF16(IDS_XSS_V1_EXPLANATION_PARAGRAPH));

  load_time_data->SetString(
      "finalParagraph",
      l10n_util::GetStringUTF16(IDS_XSS_V1_PROCEED_PARAGRAPH) +
      l10n_util::GetStringUTF16(IDS_XSS_V1_PROCEED_PARAGRAPH_LINK));
}

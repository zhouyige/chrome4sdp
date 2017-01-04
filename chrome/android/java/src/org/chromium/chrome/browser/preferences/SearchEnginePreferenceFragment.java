/*
 * Copyright (c) 2016 The Linux Foundation. All rights reserved.
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

package org.chromium.chrome.browser.preferences;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.incognito.IncognitoOnlyModeUtil;
import org.chromium.chrome.browser.search_engines.TemplateUrlService;

/**
 * A preference to choose search engine for regular and incognito tabs.
 */
public class SearchEnginePreferenceFragment extends PreferenceFragment implements
        Preference.OnPreferenceClickListener {

    public static final String PREF_SEARCH_ENGINE_FOR_REGULAR = "search_engine_regular";
    public static final String PREF_SEARCH_ENGINE_FOR_INCOGNITO = "search_engine_incognito";
    public static final String IS_INCOGNITO = "is_incognito";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.search_engine_preferences);
        getActivity().setTitle(R.string.prefs_search_engine);

        PreferenceScreen preferenceScreen = getPreferenceScreen();
        boolean isIncognitoOnlyBrowser = IncognitoOnlyModeUtil.getInstance()
                .isIncognitoOnlyModeEnabled();
        ChromeBasePreference searchEngineRegularPref =
                (ChromeBasePreference) findPreference(PREF_SEARCH_ENGINE_FOR_REGULAR);
        if (isIncognitoOnlyBrowser) {
            preferenceScreen.removePreference(searchEngineRegularPref);
        }
        ChromeBasePreference searchEngineIncognitoPref =
                (ChromeBasePreference) findPreference(PREF_SEARCH_ENGINE_FOR_INCOGNITO);
        searchEngineIncognitoPref.setOnPreferenceClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences();
    }

    private void updatePreferences() {
        updatePreferenceSearchEngine(false);
        updatePreferenceSearchEngine(true);
    }

    private void updatePreferenceSearchEngine(final boolean isIncognito) {
        final TemplateUrlService templateUrlService = TemplateUrlService.getInstance(isIncognito);
        if (templateUrlService.isLoaded()) {
            updateSummary(isIncognito);
        } else {
            TemplateUrlService.LoadListener loadListener =
                    new TemplateUrlService.LoadListener() {
                        @Override
                        public void onTemplateUrlServiceLoaded() {
                            templateUrlService.unregisterLoadListener(this);
                            // Set the injected default search engine for incognito tabs,
                            // if using partner search engines.
                            String keyword = templateUrlService.getDefaultSearchEngineInjected();
                            if (keyword != null) {
                                templateUrlService.setDefaultSearchEngine(keyword);
                            }
                            updateSummary(isIncognito);
                        }
                    };
            templateUrlService.registerLoadListener(loadListener);
            templateUrlService.load();
            ChromeBasePreference searchEnginePref;
            if (!isIncognito) {
                searchEnginePref =
                        (ChromeBasePreference) findPreference(PREF_SEARCH_ENGINE_FOR_REGULAR);
            } else {
                searchEnginePref =
                        (ChromeBasePreference) findPreference(PREF_SEARCH_ENGINE_FOR_INCOGNITO);
            }
            // Search engine for regular tabs option is not displayed for incognito only browser
            if (searchEnginePref == null) return;
            searchEnginePref.setEnabled(false);
        }
    }

    private void updateSummary(boolean isIncognito) {
        TemplateUrlService templateUrlService = TemplateUrlService.getInstance(isIncognito);
        ChromeBasePreference searchEnginePref;
        if (!isIncognito) {
            searchEnginePref =
                    (ChromeBasePreference) findPreference(PREF_SEARCH_ENGINE_FOR_REGULAR);
        } else {
            searchEnginePref =
                    (ChromeBasePreference) findPreference(PREF_SEARCH_ENGINE_FOR_INCOGNITO);
        }
        // Search engine for regular tabs option is not displayed for incognito only browser
        if (searchEnginePref == null) return;
        searchEnginePref.setEnabled(true);
        searchEnginePref.setSummary(templateUrlService.getDefaultSearchEngineTemplateUrl()
                .getShortName());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String preferenceKey = preference.getKey();
        if (preferenceKey.equals(PREF_SEARCH_ENGINE_FOR_INCOGNITO)) {
            if (preference.getFragment() != null &&
                    getActivity() instanceof OnPreferenceStartFragmentCallback) {
                Bundle args = new Bundle();
                args.putBoolean(IS_INCOGNITO, true);
                Bundle extra = preference.getExtras();
                extra.putAll(args);

                return ((OnPreferenceStartFragmentCallback)getActivity()).onPreferenceStartFragment(
                        this, preference);
            }
        }
        return false;
    }
}

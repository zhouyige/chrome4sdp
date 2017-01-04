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

package org.chromium.chrome.browser.preferences.privacy;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import org.chromium.chrome.R;
import org.chromium.chrome.browser.preferences.SecureConnectNetworkObserver;
import org.chromium.chrome.browser.preferences.PrefServiceBridge;

/**
 * Dialog to prompt the user to enable secure content only mode for a network.
 */
public class SecureContentDialog extends Dialog implements View.OnClickListener {

    private SecureConnectNetworkObserver mObserver;
    private static boolean sIsVisible;

    private static View getContentView(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.secure_content_dialog, null);
    }

    @Override
    public void onClick(View v) {
        dismiss();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        sIsVisible = false;
    }

    public static void show(Activity parentActivity, SecureConnectNetworkObserver observer) {
        if (sIsVisible) return;
        SecureContentDialog promoScreen = new SecureContentDialog(parentActivity, observer);
        promoScreen.show();
        sIsVisible = true;

    }


    public SecureContentDialog(Context context, SecureConnectNetworkObserver observer) {
        super(context, R.style.DataReductionPromoScreenDialog);
        mObserver = observer;
        setContentView(getContentView(context), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        // Remove the shadow from the buttons.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Button enableButton = (Button) findViewById(R.id.enable_button);
            Button disableButton = (Button) findViewById(R.id.disable_button);
            enableButton.setStateListAnimator(null);
            disableButton.setStateListAnimator(null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Keep the window full screen otherwise the flip animation will frame-skip.
        getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        addListeners();
    }

    private void addListeners() {
        findViewById(R.id.close_button).setOnClickListener(this);

        findViewById(R.id.enable_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mObserver.setSecureContentForNetwork(true);
                dismiss();
            }
        });

        findViewById(R.id.disable_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mObserver.setSecureContentForNetwork(false);
                dismiss();
            }
        });

        CheckBox doNotShow = (CheckBox) findViewById(R.id.do_not_show);
        doNotShow.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        PrefServiceBridge.getInstance()
                                .setSecureContentOnlyForNetworkEnabled(!isChecked);
                    }
                });
    }
}
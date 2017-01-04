// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium.content.browser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Uses an existing Activity to handle displaying video in full screen.
 */
public class ActivityContentVideoViewEmbedder implements ContentVideoViewEmbedder {
    public static final float INVALID_BRIGHTNESS = -2.f;

    private final Activity mActivity;
    private View mView;

    private float mOriginalBrightness = INVALID_BRIGHTNESS;
    private int mOriginalRequestedOrientation;

    public ActivityContentVideoViewEmbedder(Activity activity)  {
        this.mActivity = activity;

        mOriginalRequestedOrientation = mActivity.getRequestedOrientation();
    }

    @Override
    public void enterFullscreenVideo(View view, boolean isVideoLoaded) {
        FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
        decor.addView(view, 0,
                new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER));
        setSystemUiVisibility(true);
        mView = view;
    }

    @Override
    public void fullscreenVideoLoaded() {}

    @Override
    public void exitFullscreenVideo() {
        FrameLayout decor = (FrameLayout) mActivity.getWindow().getDecorView();
        decor.removeView(mView);
        setSystemUiVisibility(false);
        mView = null;
    }

    @Override
    @SuppressLint("InlinedApi")
    public void setSystemUiVisibility(boolean enterFullscreen) {
        View decor = mActivity.getWindow().getDecorView();
        if (enterFullscreen) {
            mActivity.getWindow().setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            mActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }

        int systemUiVisibility = decor.getSystemUiVisibility();
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if (enterFullscreen) {
            systemUiVisibility |= flags;
        } else {
            systemUiVisibility &= ~flags;
        }
        decor.setSystemUiVisibility(systemUiVisibility);
    }

    @Override
    public float adjustBrightness(float delta) {
        WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();

        if (mOriginalBrightness == INVALID_BRIGHTNESS)
            mOriginalBrightness = lp.screenBrightness;

        float brightness;
        if (lp.screenBrightness == -1) {
            try {
                brightness = Settings.System.getInt(
                    ((Context) mActivity).getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS) / 255.f;
            } catch (Settings.SettingNotFoundException e) {
                return INVALID_BRIGHTNESS;
            }
        } else {
            brightness = lp.screenBrightness;
        }
        brightness += delta;
        brightness = Math.min(1.f, Math.max(0.f, brightness));

        lp.screenBrightness = brightness;
        mActivity.getWindow().setAttributes(lp);

        return brightness;
    }

    @Override
    @SuppressLint("WrongConstant")
    public void setRotateLock(boolean lock) {
        if (lock) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {

                // Only support rotate lock after API level 18
                mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            }
        } else {
            mActivity.setRequestedOrientation(mOriginalRequestedOrientation);
        }
    }

    @Override
    @SuppressLint("WrongConstant")
    public float reset() {
        if (mOriginalBrightness != INVALID_BRIGHTNESS) {
            WindowManager.LayoutParams lp = mActivity.getWindow().getAttributes();

            lp.screenBrightness = mOriginalBrightness;
            mActivity.getWindow().setAttributes(lp);
        }

        mActivity.setRequestedOrientation(mOriginalRequestedOrientation);

        return mOriginalBrightness;
    }
}

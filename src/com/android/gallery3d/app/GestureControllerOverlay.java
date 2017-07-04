/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.

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

package com.android.gallery3d.app;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import android.widget.TextView;
import org.codeaurora.gallery.R;
import com.android.gallery3d.util.GalleryUtils;

public class GestureControllerOverlay extends FrameLayout implements
        ControllerOverlay, GestureController.GestureControlListener {
    private GestureController mGestureController;

    private static final int MAX_VIDEO_STEP_TIME = 60 * 1000;
    private static final int MAX_BRIGHTNESS = 100;

    private float mStartBrightness = -1.0f;
    private double mStartVolumePercent = -1.0f;
    private int mStartVideoTime = -1;

    private TextView mCurrentIndicator;
    private Drawable mBrightnessDrawable;
    private Drawable mVolumeDrawable;
    private Drawable mRewindDrawable;
    private Drawable mForwardDrawable;

    private final int mIndicatorHeight;
    private final int mIndicatorWidth;
    private final int mIndicatorIconSize;
    private final int mIndicatorTextSize;

    private Listener mListener;
    private TimeBar mTimeBar;

    private CommonControllerOverlay mControllerOverlay;

    public GestureControllerOverlay(Context context) {
        super(context);
        Resources res = getResources();
        mIndicatorHeight = res.getDimensionPixelSize(R.dimen.controller_indicator_height);
        mIndicatorWidth = res.getDimensionPixelSize(R.dimen.controller_indicator_width);
        mIndicatorIconSize = res.getDimensionPixelSize(R.dimen.controller_indicator_icon_size);
        mIndicatorTextSize = res.getDimensionPixelSize(R.dimen.controller_indicator_text_size);
        init(context);
    }

    public GestureControllerOverlay(Context context,
                                    MovieControllerOverlay movieControllerOverlay) {
        this(context);
        mControllerOverlay = movieControllerOverlay;
        if (movieControllerOverlay instanceof MovieControllerOverlayNew) {
            mTimeBar = ((MovieControllerOverlayNew) movieControllerOverlay).getTimeBar();
        }
    }

    private void init(Context context) {
        mGestureController = new GestureController(context, this);

        mBrightnessDrawable = getResources().getDrawable(R.drawable.ic_controller_brightness, null);
        if (mBrightnessDrawable != null) {
            mBrightnessDrawable.setBounds(0, 0, mIndicatorIconSize, mIndicatorIconSize);
        }

        mVolumeDrawable = getResources().getDrawable(R.drawable.ic_controller_volume, null);
        if (mVolumeDrawable != null) {
            mVolumeDrawable.setBounds(0, 0, mIndicatorIconSize, mIndicatorIconSize);
        }

        mRewindDrawable = getResources().getDrawable(R.drawable.ic_menu_rewind, null);
        if (mRewindDrawable != null) {
            mRewindDrawable.setBounds(0, 0, mIndicatorIconSize, mIndicatorIconSize);
        }

        mForwardDrawable = getResources().getDrawable(R.drawable.ic_menu_forward, null);
        if (mForwardDrawable != null) {
            mForwardDrawable.setBounds(0, 0, mIndicatorIconSize, mIndicatorIconSize);
        }

        LayoutParams matchParent =
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        View background = new View(context);
        background.setBackgroundColor(Color.TRANSPARENT);
        addView(background, matchParent);

        LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, mIndicatorHeight);
        int paddingH = getResources().getDimensionPixelSize(R.dimen.controller_indicator_padding_h);
        int paddingV = getResources().getDimensionPixelSize(R.dimen.controller_indicator_padding_v);
        mCurrentIndicator = new TextView(context);
        mCurrentIndicator.setBackgroundResource(R.drawable.bg_controller_indicator);
        mCurrentIndicator.setPadding(paddingH, paddingV, paddingH, paddingV);
        mCurrentIndicator.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        mCurrentIndicator.setTextSize(TypedValue.COMPLEX_UNIT_PX, mIndicatorTextSize);
        mCurrentIndicator.setTextColor(Color.WHITE);
        mCurrentIndicator.setMinWidth(mIndicatorWidth);
        addView(mCurrentIndicator, layoutParams);
        mCurrentIndicator.setVisibility(INVISIBLE);
    }

    private void showIndicator() {
        if (mCurrentIndicator != null) {
            mCurrentIndicator.setAlpha(1.0f);
            mCurrentIndicator.setVisibility(VISIBLE);
        }
    }

    private void hideIndicator() {
        if (mCurrentIndicator != null) {
            mCurrentIndicator.setText(null);
            mCurrentIndicator.setVisibility(INVISIBLE);
        }
    }

    public void doOnPause() {
        hideIndicator();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureController != null) {
            mGestureController.onTouchEvent(event);
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed && mGestureController != null) {
            mGestureController.setRect(left, top, right, bottom);
        }

        int h = bottom - top;
        int w = right - left;

        if (mCurrentIndicator != null) {
            layoutCenteredView(mCurrentIndicator, 0, 0, w, h);
        }
    }

    protected void layoutCenteredView(View view, int l, int t, int r, int b) {
        int cw = view.getMeasuredWidth();
        int ch = view.getMeasuredHeight();
        int cl = (r - l - cw) / 2;
        int ct = (b - t - ch) / 2;
        view.layout(cl, ct, cl + cw, ct + ch);
    }

    @Override
    public void onGestureDone(boolean notStart) {
        mStartBrightness = -1.0f;
        mStartVolumePercent = -1.0f;
        mStartVideoTime = -1;
        hideIndicator();

        if (notStart) {
            if (mControllerOverlay != null) {
                if (mControllerOverlay.isShown())
                    mControllerOverlay.hide();
                else
                    mControllerOverlay.show();
            }

        }
    }

    @Override
    public void adjustBrightness(double adjustPercent) {
        if (adjustPercent < -1.0f) {
            adjustPercent = -1.0f;
        } else if (adjustPercent > 1.0f) {
            adjustPercent = 1.0f;
        }

        WindowManager.LayoutParams lp = ((MovieActivity) getContext()).getWindow().getAttributes();
        if (mStartBrightness < 0) {
            mStartBrightness = lp.screenBrightness;
        }
        float targetBrightness = (float) (mStartBrightness + adjustPercent * 1.0f);
        if (targetBrightness <= 0.0f) {
            targetBrightness = 0.0f;
        } else if (targetBrightness >= 1.0f) {
            targetBrightness = 1.0f;
        }
        lp.screenBrightness = targetBrightness;
        ((MovieActivity) getContext()).getWindow().setAttributes(lp);

        if (mCurrentIndicator != null) {
            mCurrentIndicator.setCompoundDrawables(null, mBrightnessDrawable, null, null);
            mCurrentIndicator.setText((int) (targetBrightness * MAX_BRIGHTNESS) + "%");
        }
        showIndicator();
    }

    @Override
    public void adjustVolumeLevel(double adjustPercent) {
        if (adjustPercent < -1.0f) {
            adjustPercent = -1.0f;
        } else if (adjustPercent > 1.0f) {
            adjustPercent = 1.0f;
        }

        AudioManager audioManager = (AudioManager) getContext()
                .getSystemService(Context.AUDIO_SERVICE);
        final int STREAM = AudioManager.STREAM_MUSIC;
        int maxVolume = audioManager.getStreamMaxVolume(STREAM);

        if (maxVolume == 0) return;

        if (mStartVolumePercent < 0) {
            int curVolume = audioManager.getStreamVolume(STREAM);
            mStartVolumePercent = curVolume * 1.0f / maxVolume;
        }
        double targetPercent = mStartVolumePercent + adjustPercent;
        if (targetPercent > 1.0f) {
            targetPercent = 1.0f;
        } else if (targetPercent < 0) {
            targetPercent = 0;
        }

        int index = (int) (maxVolume * targetPercent);
        if (index > maxVolume) {
            index = maxVolume;
        } else if (index < 0) {
            index = 0;
        }
        audioManager.setStreamVolume(STREAM, index, 0);

        if (mCurrentIndicator != null) {
            mCurrentIndicator.setCompoundDrawables(null, mVolumeDrawable, null, null);
            mCurrentIndicator.setText(index * 100 / maxVolume + "%");
        }
        showIndicator();
    }

    @Override
    public void adjustVideoPosition(double adjustPercent, boolean forwardDirection) {
        if (mTimeBar == null || !(mTimeBar instanceof TimeBarNew)) {
            return;
        }

        if (!((TimeBarNew) mTimeBar).seekable() || !mTimeBar.getScrubbing()
                || !mTimeBar.isClickable()) {
            return;
        }

        if (adjustPercent < -1.0f) {
            adjustPercent = -1.0f;
        } else if (adjustPercent > 1.0f) {
            adjustPercent = 1.0f;
        }

        int totalTime = ((TimeBarNew) mTimeBar).getTotalTime();

        if (mStartVideoTime < 0) {
            mStartVideoTime = ((TimeBarNew) mTimeBar).getCurrentTime();
        }

        int targetTime = mStartVideoTime + (int) (MAX_VIDEO_STEP_TIME * adjustPercent);
        if (targetTime > totalTime) {
            targetTime = totalTime;
        }
        if (targetTime < 0) {
            targetTime = 0;
        }

        String targetTimeString = GalleryUtils.formatDuration(getContext(), targetTime / 1000);

        if (forwardDirection) {
            if (mCurrentIndicator != null) {
                mCurrentIndicator.setCompoundDrawables(null, mForwardDrawable, null, null);
                mCurrentIndicator.setText(targetTimeString);
            }
        } else {
            if (mCurrentIndicator != null) {
                mCurrentIndicator.setCompoundDrawables(null, mRewindDrawable, null, null);
                mCurrentIndicator.setText(targetTimeString);
            }
        }

        if (mListener != null) {
            mListener.onSeekEnd(targetTime, 0, 0);
        }
        if (mTimeBar != null) {
            mTimeBar.setTime(targetTime, totalTime, 0, 0);
        }

        showIndicator();
    }

    @Override
    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    @Override
    public void setCanReplay(boolean canReplay) {

    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void show() {

    }

    @Override
    public void showPlaying() {

    }

    @Override
    public void showPaused() {

    }

    @Override
    public void showEnded() {

    }

    @Override
    public void showLoading() {

    }

    @Override
    public void showErrorMessage(String message) {

    }

    @Override
    public void setTimes(int currentTime, int totalTime, int trimStartTime, int trimEndTime) {

    }

    @Override
    public void setViewEnabled(boolean isEnabled) {

    }

    @Override
    public void setPlayPauseReplayResume() {

    }
}

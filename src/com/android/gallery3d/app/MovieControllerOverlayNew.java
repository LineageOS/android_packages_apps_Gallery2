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
import android.graphics.Color;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import org.codeaurora.gallery3d.video.IVideoSnapshotListener;
import org.codeaurora.gallery3d.video.IControllerRewindAndForward;

import org.codeaurora.gallery.R;

public class MovieControllerOverlayNew extends MovieControllerOverlay {
    private VideoSnapshotLayout mVideoSnapshotLayout = new VideoSnapshotLayout();
    private IVideoSnapshotListener mVideoSnapshotListener;

    private boolean mIsLive = false;
    private ImageView mLiveImage;
    private ImageView mStopBtn;

    private int mLiveMargin;

    public MovieControllerOverlayNew(Context context) {
        super(context);

        mVideoSnapshotLayout.init(context);
        mLiveImage = ((MovieActivity) mContext).getLiveImage();
        mStopBtn = mControllerRewindAndForwardExt.getStopBtn();
        mLiveMargin = context.getResources().getDimensionPixelSize(
                R.dimen.livestream_icon_padding);
        addView(mStopBtn);
    }

    @Override
    protected void createTimeBar(Context context) {
        mTimeBar = new TimeBarNew(context, this);
    }

    public TimeBar getTimeBar() {
        return mTimeBar;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = ((MovieActivity) mContext).getWindowManager().getDefaultDisplay().getWidth();
        Rect insets = mWindowInsets;
        int pr = insets.right;
        int pb = insets.bottom;
        int pt = insets.top;
        int h = bottom - top;
        int w = right - left;

        int y = h - pb;
        int barHeight = mTimeBar.getPreferredHeight();

        mBackground.layout(0, y - barHeight, w, y);
        mScreenModeExt.onLayout(w, pr, y);
        if (mIsLive && mState != State.ENDED) {
            if (mLiveImage != null) {
                mLiveImage.setPadding(mLiveImage.getPaddingLeft(), mLiveMargin + pt,
                        mLiveMargin + pr, mLiveImage.getPaddingBottom());
                mLiveImage.setVisibility(View.VISIBLE);
            }
            if (isPrepared()) {
                mPlayPauseReplayView.setVisibility(View.GONE);

                if (mStopBtn != null) {
                    mStopBtn.setVisibility(View.VISIBLE);
                    mStopBtn.layout(0, y - barHeight, barHeight, y);
                }
            }
        } else {
            mPlayPauseReplayView.setVisibility(View.VISIBLE);
            if (mLiveImage != null) {
                mLiveImage.setVisibility(View.GONE);
            }
            if (mStopBtn != null) {
                mStopBtn.setVisibility(View.GONE);
            }
            mPlayPauseReplayView.layout(insets.left, y - barHeight, insets.left + barHeight, y);
        }
        mTimeBar.layout(insets.left + barHeight, y - barHeight,
                width - mScreenModeExt.getAddedRightPadding(), y);

        mVideoSnapshotLayout.layoutButton(
                w - pr - mVideoSnapshotLayout.getButtonWidth(),
                y - barHeight - mVideoSnapshotLayout.getButtonHeight(),
                w - pr,
                y - barHeight);
        mVideoSnapshotLayout.layoutAnim(left, top, right, bottom);
    }

    public void setLive(boolean live) {
        mIsLive = live;
        mTimeBar.setClickable(!mIsLive);
    }

    @Override
    public IControllerRewindAndForward getControllerRewindAndForwardExt() {
        return mControllerRewindAndForwardExt;
    }

    private boolean isPrepared() {
        if ((mState == State.PLAYING || mState == State.BUFFERING || mState == State.LOADING)) {
            return true;
        }
        return false;
    }

    public void setVideoSnapshotListener(IVideoSnapshotListener listener) {
        mVideoSnapshotListener = listener;
    }

    public void showVideoSnapshotButton(boolean show) {
        if (mVideoSnapshotLayout != null) {
            mVideoSnapshotLayout.showVideoSnapshotButton(show);
        }
    }

    class VideoSnapshotLayout implements View.OnClickListener {
        private ImageView mVideoSnapshotButton;
        private ImageView mVideoSnapshotAnimView;
        private Animation mVideoSnapshotAnimation;

        public void init(Context context) {
            LayoutParams wrapContent =
                    new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            mVideoSnapshotButton = new ImageView(context);
            mVideoSnapshotButton.setImageResource(R.drawable.ic_video_snapshot_selector);
            mVideoSnapshotButton.setScaleType(ImageView.ScaleType.CENTER);
            mVideoSnapshotButton.setFocusable(true);
            mVideoSnapshotButton.setClickable(true);
            mVideoSnapshotButton.setOnClickListener(this);
            addView(mVideoSnapshotButton, wrapContent);

            LayoutParams matchParent =
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            mVideoSnapshotAnimView = new ImageView(context);
            addView(mVideoSnapshotAnimView, matchParent);

            mVideoSnapshotAnimation = AnimationUtils.loadAnimation(context, R.anim.player_out);
            mVideoSnapshotAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    mVideoSnapshotAnimView.setBackgroundColor(Color.WHITE);
                    mVideoSnapshotButton.setEnabled(false);
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mVideoSnapshotAnimView.setBackgroundColor(Color.TRANSPARENT);
                    mVideoSnapshotButton.setEnabled(true);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }

        public int getButtonWidth() {
            return mVideoSnapshotButton == null ? 0 : mVideoSnapshotButton.getMeasuredWidth();
        }

        public int getButtonHeight() {
            return mVideoSnapshotButton == null ? 0 : mVideoSnapshotButton.getMeasuredHeight();
        }

        public void layoutButton(int l, int t, int r, int b) {
            if (mVideoSnapshotButton != null) {
                mVideoSnapshotButton.layout(l, t, r, b);
            }
        }

        public void layoutAnim(int l, int t, int r, int b) {
            if (mVideoSnapshotAnimView != null) {
                mVideoSnapshotAnimView.layout(l, t, r, b);
            }
        }

        public void showVideoSnapshotButton(boolean show) {
            if (mVideoSnapshotButton != null) {
                mVideoSnapshotButton.setVisibility(show ? VISIBLE : GONE);
            }
        }

        @Override
        public void onClick(View v) {
            maybeStartHiding();
            if (mVideoSnapshotListener != null && mVideoSnapshotListener.canVideoSnapshot()) {
                mVideoSnapshotListener.onVideoSnapshot();
                startAnimation();
            }
        }

        private void startAnimation() {
            if (mVideoSnapshotAnimView != null) {
                mVideoSnapshotAnimView.startAnimation(mVideoSnapshotAnimation);
            }
        }
    }
}

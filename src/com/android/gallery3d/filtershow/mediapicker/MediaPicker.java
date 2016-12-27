/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above
 *     copyright notice, this list of conditions and the following
 *     disclaimer in the documentation and/or other materials provided
 *     with the distribution.
 *   * Neither the name of The Linux Foundation nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
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

package com.android.gallery3d.filtershow.mediapicker;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.android.photos.views.HeaderGridView;

import org.codeaurora.gallery.R;

public class MediaPicker extends ViewGroup {

    private LinearLayout mSelStrip;
    private LinearLayout mSlideStrip;
    private HeaderGridView mGridView;
    private ImageButton mArrow;

    private boolean mIsFullScreen, mLayoutChanged;
    private int mCurrentDesiredHeight;

    private final Handler mHandler = new Handler();
    private int mDefaultGridHeight;
    private TouchHandler mTouchHandler;

    static Context mContext;

    public MediaPicker(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mIsFullScreen = true;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mSlideStrip = (LinearLayout) findViewById(R.id.mediapicker_slidestrip);
        mSelStrip = (LinearLayout) findViewById(R.id.mediapicker_tabstrip);
        mGridView = (HeaderGridView) findViewById(R.id.grid);
        mArrow = (ImageButton) findViewById(R.id.arrow);
        mTouchHandler = new TouchHandler();
        mArrow.setOnTouchListener(mTouchHandler);

        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            private boolean mLandMode = isLandscapeMode();

            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                final boolean newLandMode = isLandscapeMode();
                if (mLandMode != newLandMode) {
                    mLandMode = newLandMode;
                    mLayoutChanged = true;
                    setupFullScreen(mIsFullScreen, false);
                }
            }
        });
    }

    @Override
    protected void onLayout(final boolean changed, final int left, final int top, final int right,
                            final int bottom) {
        int y = bottom;
        final int width = right - left;
        final int selHight = mSelStrip.getMeasuredHeight();
        mSelStrip.layout(0, y - selHight, width, y);
        y -= selHight;

        final int gridHeight = mGridView.getMeasuredHeight();
        mGridView.layout(0, y - gridHeight, width, y);
        y -= gridHeight;

        mSlideStrip.layout(0, y - mSlideStrip.getMeasuredHeight(), width, y);
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        int requestedHeight = MeasureSpec.getSize(heightMeasureSpec);
        measureChild(mSelStrip, widthMeasureSpec, heightMeasureSpec);

        int selStripHeight = mSelStrip.getMeasuredHeight();
        measureChild(mSlideStrip, widthMeasureSpec, heightMeasureSpec);
        int slideHeight = mSlideStrip.getMeasuredHeight();

        final int gridAdjustedHeight = mCurrentDesiredHeight - selStripHeight - slideHeight;
        int gridHeight;
        if (gridAdjustedHeight < 0)
            gridHeight = 0;
        else if (gridAdjustedHeight < mDefaultGridHeight)
            gridHeight = mDefaultGridHeight;
        else
            gridHeight = gridAdjustedHeight;

        int gridHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                gridHeight, MeasureSpec.EXACTLY);
        measureChild(mGridView, widthMeasureSpec, gridHeightMeasureSpec);
        setMeasuredDimension(mGridView.getMeasuredWidth(), requestedHeight);
    }

    public void setupFullScreen(final boolean isFullScreen, final boolean animate) {
        if (isFullScreen == mIsFullScreen && !mLayoutChanged) {
            return;
        }

        mIsFullScreen = isFullScreen;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setViewHeight(animate);
            }
        });
    }

    private void setViewHeight(boolean animate) {
        final int startHeight = mCurrentDesiredHeight;
        int height = getContext().getResources().getDisplayMetrics().heightPixels;
        if (!mIsFullScreen) {
            height = measureHeight();
        }
        clearAnimation();
        if (animate) {
            final int deltaHeight = height - startHeight;
            final Animation animation = new Animation() {
                @Override
                protected void applyTransformation(final float interpolatedTime,
                        final Transformation t) {
                    mCurrentDesiredHeight = (int) (startHeight + deltaHeight * interpolatedTime);
                    requestLayout();
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }
            };
            animation.setDuration(500);
            startAnimation(animation);
        } else {
            mCurrentDesiredHeight = height;
        }
        if (mIsFullScreen)
            mArrow.setImageResource(R.drawable.arrow_down);
        else
            mArrow.setImageResource(R.drawable.arrow);
        requestLayout();
    }

    private int measureHeight() {
        final int measureSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 1, MeasureSpec.AT_MOST);
        measureChild(mSelStrip, measureSpec, measureSpec);
        measureChild(mSlideStrip, measureSpec, measureSpec);
        if (isLandscapeMode())
            mDefaultGridHeight = getResources().getDimensionPixelSize(
                    R.dimen.mediapicker_land_height);
        else
            mDefaultGridHeight = getResources().getDimensionPixelSize(
                    R.dimen.mediapicker_default_height);
        return mDefaultGridHeight + mSelStrip.getMeasuredHeight() + mSlideStrip.getMeasuredHeight();
    }

    private boolean isLandscapeMode() {
        return mContext.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

    private class TouchHandler implements OnTouchListener {
        private boolean mMoved = false;
        private MotionEvent mDownEvent;
        private static final float DIRECTION_RATIO = 1.1f;
        private static final int TOUCH_THRESHOLD = 450;
        private static final int TOUCH_SLOP = 24;


        TouchHandler() {
        }

        boolean checkMoved(final MotionEvent mv) {
            final float dx = mDownEvent.getRawX() - mv.getRawX();
            final float dy = mDownEvent.getRawY() - mv.getRawY();
            if (Math.abs(dy) > TOUCH_SLOP &&
                    (Math.abs(dy) / Math.abs(dx)) > DIRECTION_RATIO) {
                return true;
            }
            return false;
        }

        @Override
        public boolean onTouch(final View view, final MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_UP: {
                    if (!mMoved || mDownEvent == null) {
                        return false;
                    }

                    final float dy = motionEvent.getRawY() - mDownEvent.getRawY();
                    final float dt =
                            (motionEvent.getEventTime() - mDownEvent.getEventTime()) / 1000.0f;
                    final float yVelocity = dy / dt;

                    if (checkMoved(motionEvent)) {
                        if ((yVelocity + TOUCH_THRESHOLD) < 0 && !mIsFullScreen) {
                            setupFullScreen(true, true);
                        } else if ((yVelocity - TOUCH_THRESHOLD) > 0 && mIsFullScreen) {
                            setupFullScreen(false, true);
                        }
                    }
                    mDownEvent = null;
                    mMoved = false;
                    break;
                }
                case MotionEvent.ACTION_DOWN: {
                    mDownEvent = MotionEvent.obtain(motionEvent);
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mDownEvent == null) {
                        return mMoved;
                    }
                    if (checkMoved(motionEvent)) {
                        mMoved = true;
                    }
                }
            }
            return mMoved;
        }
    }
}


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
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class GestureController {
    private static final int GESTURE_THRESHOLD = 40;
    private GestureDetector mGestureDetector;
    private GestureControlListener mGestureControlListener;

    private Rect mFullRect = new Rect();
    private Rect mBrightnessRect = new Rect();
    private Rect mVolumeRect = new Rect();
    private Type mType = Type.NONE;

    private MotionEvent mStartEvent;

    public GestureController(Context context, GestureControlListener gestureControlListener) {
        mGestureControlListener = gestureControlListener;
        mGestureDetector =
                new GestureDetector(context, new GestureListener(gestureControlListener));
    }

    public interface GestureControlListener {
        void onGestureDone(boolean notStart);
        /**
         * change current windows brightness by add adjustPercent.
         *
         * @param adjustPercent: -1.0f ~ 1.0f, increase if adjustPercent > 0,
         *            decrease if adjustPercent < 0;
         */
        void adjustBrightness(double adjustPercent);

        /**
         * change volume level by add adjustPercent.
         *
         * @param adjustPercent: -1.0f ~ 1.0f, increase if adjustPercent > 0,
         *            decrease if adjustPercent < 0;
         */
        void adjustVolumeLevel(double adjustPercent);

        /**
         * change video position by add adjustPercent.
         *
         * @param adjustPercent: -1.0f ~ 1.0f, increase if adjustPercent > 0,
         *            decrease if adjustPercent < 0;
         * @param forwardDirection: true if direction is forward.
         */
        void adjustVideoPosition(double adjustPercent, boolean forwardDirection);
    }

    private enum Type {
        NONE,
        BRIGHTNESS,
        VOLUME,
        SEEK,
    }

    public void setRect(Rect rect) {
        this.setRect(rect.left, rect.top, rect.right, rect.bottom);
    }

    public void setRect(int l, int t, int r, int b) {
        mFullRect.left = l;
        mFullRect.top = t;
        mFullRect.right = r;
        mFullRect.bottom = b;

        mBrightnessRect.left = l;
        mBrightnessRect.top = t;
        mBrightnessRect.right = r / 2;
        mBrightnessRect.bottom = b;

        mVolumeRect.left = l / 2;
        mVolumeRect.top = t;
        mVolumeRect.right = r;
        mVolumeRect.bottom = b;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private GestureControlListener mGestureControlListener;

        public GestureListener(GestureControlListener controlListener) {
            mGestureControlListener = controlListener;
        }

        @Override
        public boolean onScroll(MotionEvent start, MotionEvent end,
                                float distanceX, float distanceY) {
            if (mGestureControlListener == null) {
                return super.onScroll(start, end, distanceX, distanceY);
            }
            // function depends on start position. (volume or brightness or play position.)
            double yDistance = end.getY() - start.getY();
            double xDistance = end.getX() - start.getX();

            if (mType == Type.BRIGHTNESS) {
                // use half of BrightnessRect's height to calc percent.
                if (mBrightnessRect.height() != 0) {
                    double percent = yDistance / (mBrightnessRect.height() / 2.0f) * -1.0f;
                    mGestureControlListener.adjustBrightness(percent);
                }
            } else if (mType == Type.VOLUME) {
                // use half of VolumeRect's height to calc percent.
                if (mVolumeRect.height() != 0) {
                    double percent = yDistance / (mVolumeRect.height() / 2.0f) * -1.0f;
                    mGestureControlListener.adjustVolumeLevel(percent);
                }
            } else if (mType == Type.SEEK) {
                if (mFullRect.width() != 0) {
                    double percent = xDistance / mFullRect.width();
                    mGestureControlListener.adjustVideoPosition(percent, distanceX < 0);
                }
            }
            return true;
        }
    }

    private boolean isEventValid(MotionEvent event) {
        // this gesture just worked on single pointer.
        return (mGestureDetector != null && event.getPointerCount() == 1);
    }

    public boolean onTouchEvent(MotionEvent event) {
        if (!isEventValid(event)) {
            return false;
        }
        // decide which process is needed.
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartEvent = MotionEvent.obtain(event);
                break;
            case MotionEvent.ACTION_MOVE:
                if (mType == Type.NONE && mStartEvent != null) {
                    mType = calcControllerType(mStartEvent, event);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mGestureControlListener != null) {
                    if (mType == Type.NONE) {
                        mGestureControlListener.onGestureDone(true);
                    } else {
                        mGestureControlListener.onGestureDone(false);
                    }
                }
                mType = Type.NONE;
                mStartEvent = null;
                break;
            default:
                break;
        }

        return mGestureDetector.onTouchEvent(event);
    }

    private Type calcControllerType(MotionEvent startEvent, MotionEvent currentEvent) {
        float startX = startEvent.getX();
        float startY = startEvent.getY();
        float currentX = currentEvent.getX();
        float currentY = currentEvent.getY();
        if (Math.abs(currentX - startX) >= GESTURE_THRESHOLD) {
            if (mFullRect.contains((int) startX, (int) startY)) {
                return Type.SEEK;
            }
        } else if (Math.abs(currentY - startY) >= GESTURE_THRESHOLD) {
            if (mBrightnessRect.contains((int) startX, (int) startY)) {
                return Type.BRIGHTNESS;
            } else if (mVolumeRect.contains((int) startX, (int) startY)) {
                return Type.VOLUME;
            }
        }
        return Type.NONE;
    }
}

/*
 * Copyright (c) 2016, The Linux Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of The Linux Foundation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
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

package com.android.gallery3d.app.dualcam3d;

import android.graphics.Bitmap;
import android.util.Log;

import com.android.gallery3d.app.dualcam3d.threed.Controller;
import com.android.gallery3d.filtershow.tools.DualCameraNativeEngine;

public class Effect implements GLView.Listener {
    private static final String TAG = "Effect";
    public static final int INTENSITY_MAX = 10;
    public static final float DEFAULT_BRIGHTNESS_INTENSITY = 1.0f;
    private static final int PREVIEW_DIMENSION_MAX = 1080;

    enum Type {
        NONE, FOCUS, HALO, SKETCH, FUSION, ZOOM,
        MOTION, BW, BLACKBOARD, WHITEBOARD,
        POSTERIZE, NEGATIVE, THREE_DIMENSIONAL
    }

    private final ThreeDimensionalActivity mActivity;
    private Bitmap mBitmap;
    private Type mType = Type.NONE;
    private int mX;
    private int mY;
    private float mIntensity = 0.5f;
    private boolean mIsPreview = true;

    private int mScale;

    private Controller mController;

    private Thread mThread;
    private Boolean mCancelled;

    public Effect(ThreeDimensionalActivity activity) {
        mActivity = activity;
    }

    public void setBitmap(Bitmap bitmap, int scale) {
        mBitmap = bitmap;
        scaleBitmap(scale);
    }

    public static int scale(int width, int height) {
        int scale = 1;
        while (width / scale > PREVIEW_DIMENSION_MAX || height / scale > PREVIEW_DIMENSION_MAX) {
            scale *= 2;
        }
        return scale > 1 ? scale : 1;
    }

    private void scaleBitmap(int scale) {
        if (scale != 1) {
            mScale = scale;
        } else if (mBitmap != null) {
            int width = mBitmap.getWidth(), height = mBitmap.getHeight();
            mX = width / 2;
            mY = height / 2;
            mScale = scale(width, height);
            if (mScale != 1) {
                mBitmap = Bitmap.createScaledBitmap(mBitmap,
                        width / mScale, height / mScale, false);
            }
        }
        mActivity.sendMessage(ThreeDimensionalActivity.MSG_UPDATE_IMAGE, mBitmap);
    }

    public void set(Type type) {
        if (type == Type.THREE_DIMENSIONAL) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    DualCameraNativeEngine.DepthMap3D map = DualCameraNativeEngine.getInstance()
                            .getDepthMap3D(mBitmap);
                    if (map != null) {
                        Log.d(TAG, "got 3d map");
                        mActivity.sendMessage(ThreeDimensionalActivity.MSG_UPDATE_IMAGE, mBitmap);
                        mActivity.sendMessage(ThreeDimensionalActivity.MSG_UPDATE_3D_DEPTH_MAP, map);
                    } else {
                        Log.e(TAG, "cannot get 3d map");
                    }
                }
            }).start();
            if (mController == null) {
                mController = mActivity.getController();
            }
            mController.start();
        } else {
            if (mController != null) {
                mController.stop(true);
            }
            mActivity.sendMessage(ThreeDimensionalActivity.MSG_UPDATE_3D_DEPTH_MAP, null);
            request();
        }
        mType = type;
    }

    private void setCoordination(float x, float y) {
        if (x >= 0 && y >= 0 && x < mBitmap.getWidth() && y < mBitmap.getHeight()) {
            mX = (int) (x * mScale);
            mY = (int) (y * mScale);
        }
    }

    public void setIntensity(float intensity) {
        if (intensity < 0) intensity = 0;
        if (intensity > INTENSITY_MAX) intensity = 1;
        if (intensity != mIntensity) {
            mIntensity = intensity / INTENSITY_MAX;
            request();
        }
    }

    private synchronized void request() {
        notify();
    }


    public void recycle() {
        if (mBitmap != null) {
            mBitmap.recycle();
        }
    }

    @Override
    public void onMove(float deltaX, float deltaY) {
        if (mType == Type.THREE_DIMENSIONAL && mController != null) {
            mController.onMove(deltaX, deltaY);
        }
    }

    @Override
    public void onClick(float x, float y) {
        if (mType != Type.THREE_DIMENSIONAL) {
            setCoordination(x, y);
            request();
        }
    }

    @Override
    public void onLayout(int width, int height) {
    }

    public void resume() {
        if (mController != null && mType == Type.THREE_DIMENSIONAL) {
            mController.start();
        }
        mCancelled = false;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mCancelled) {
                    synchronized (Effect.this) {
                        try {
                            Effect.this.wait();
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
        });
        mThread.start();
    }

    public void pause() {
        if (mController != null) {
            mController.stop(false);
        }
        if (mThread != null) {
            mCancelled = true;
            synchronized (this) {
                notify();
            }
            try {
                mThread.join();
            } catch (InterruptedException ignored) {
            }
            mThread = null;
        }
    }
}

/*
 * Copyright (c) 2016-2017, The Linux Foundation. All rights reserved.
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.Surface;

public class GLView extends GLSurfaceView {
    private static final String TAG = "GLView";
    private final com.android.gallery3d.app.dualcam3d.gl.Renderer mRenderer;

    private Bitmap mBitmap;
    private Bitmap mDepthMap;
    private int mRotation;

    public GLView(Context context) {
        this(context, null);
    }

    public GLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        mRenderer = new com.android.gallery3d.app.dualcam3d.gl.Renderer();
        setRenderer(mRenderer);
    }

    @Override
    public void onResume() {
        super.onResume();
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mRenderer.setImageBitmap(mBitmap);
        mRenderer.setDepthMap(mDepthMap);
        requestRender();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRenderer.setImageBitmap(null);
        mRenderer.setDepthMap(null);
    }

    public void setImageBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
        mRenderer.setImageBitmap(bitmap);
        mRenderer.setDepthMap(null);
        requestRender();
    }

    public void setDepthMap(Bitmap map) {
        mDepthMap = map;
        mRenderer.setDepthMap(map);
        requestRender();
    }

    public void setOffset(float x, float y) {
        mRenderer.setOffset(x, y);
        requestRender();
    }

    public void setOffsetWithRotation(float x, float y) {
        if (mRotation == Surface.ROTATION_0 || mRotation == Surface.ROTATION_180) {
            //noinspection SuspiciousNameCombination
            setOffset(y, x);
        } else {
            setOffset(x, y);
        }
    }

    public void setOffsetDelta(float deltaX, float deltaY) {
        mRenderer.setOffsetDelta(deltaX, deltaY);
        requestRender();
    }

    public void recycle() {
        mListener = null;
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
        }
    }

    public void setRotation(int rotation) {
        mRotation = rotation;
    }

    private static final float MOVE_THRESHOLD = 1.0f;
    private PointF mLastPoint;
    private int mLastAction = MotionEvent.ACTION_DOWN;
    private Listener mListener;

    public interface Listener {
        void onMove(float deltaX, float deltaY);
        void onClick(float x, float y);
        void onLayout(int width, int height);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Matrix invertMatrix = mRenderer.getImageInvertMatrix();
        if (invertMatrix == null) {
            return true;
        }

        float[] point = new float[]{event.getX(), event.getY()};
        invertMatrix.mapPoints(point);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastPoint = new PointF(point[0], point[1]);
                mLastAction = MotionEvent.ACTION_DOWN;
                break;
            case MotionEvent.ACTION_MOVE:
                if (mLastPoint != null) {
                    float deltaX = point[0] - mLastPoint.x,
                            deltaY = point[1] - mLastPoint.y;
                    if (mLastAction == MotionEvent.ACTION_MOVE || (Math.abs(deltaX) > MOVE_THRESHOLD
                            && Math.abs(deltaY) > MOVE_THRESHOLD)) {
                        if (mListener != null) {
                            mListener.onMove(deltaX, deltaY);
                        }
                        mLastPoint.set(point[0], point[1]);
                        mLastAction = MotionEvent.ACTION_MOVE;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mLastAction != MotionEvent.ACTION_MOVE && mListener != null) {
                    mListener.onClick(point[0], point[1]);
                }
                mLastPoint = null;
                mLastAction = MotionEvent.ACTION_UP;
                break;
        }
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed && mListener != null) {
            // if the loyout is changed by rotation not by reLayoutGLView,
            // then save width and heigh of the view for caculating.
            if (left == 0 && top == 0) {
                mListener.onLayout(right - left, bottom - top);
            }
        }
    }
}

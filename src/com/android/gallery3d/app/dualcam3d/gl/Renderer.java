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
package com.android.gallery3d.app.dualcam3d.gl;

import android.graphics.Bitmap;
import android.graphics.RectF;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;


import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Renderer implements GLSurfaceView.Renderer {
    private static final String TAG = "Renderer";
    public static final float THETA_MAX = (float) Math.toRadians(6);
    private final float[] mProjectionMatrix = new float[16];

    private float mOffsetX;
    private float mOffsetY;

    private Bitmap mImageBitmap;
    private Bitmap mDepthMap;

    private boolean mImageChanged;
    private boolean mDepthMapChanged;

    private RectF mSurfaceRect;
    private android.graphics.Matrix mImageInvertMatrix;

    private final Mesh mMesh = Mesh.create();
    private Shader mShader;
    private final Texture mImageTexture = new Texture();

    public Renderer() {
    }

    public void setImageBitmap(Bitmap bitmap) {
        mImageBitmap = bitmap;
        mImageChanged = true;
        mDepthMap = null;
        mDepthMapChanged = true;
        setImageInvertMatrix();
    }

    public void setDepthMap(Bitmap map) {
        mDepthMap = map;
        mDepthMapChanged = true;
        if (map == null) {
            mOffsetX = 0;
            mOffsetY = 0;
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mShader = new Shader();
        mShader.bind();
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "w=" + width + "x" + height);
        float ratio = (float) width / height;
        float sinY = (float) Math.sin(Math.toRadians(Settings.FIELD_OF_VIEW / 2)) / ratio;
        float cosY = (float) Math.cos(Math.toRadians(Settings.FIELD_OF_VIEW / 2));
        float fovY = (float) Math.toDegrees(Math.atan2(sinY, cosY) * 2.f);
        Matrix.perspectiveM(mProjectionMatrix, 0, fovY, ratio, 0.1f, 500.f);
        GLES20.glViewport(0, 0, width, height);
        mSurfaceRect = new RectF(0, 0, width, height);
        setImageInvertMatrix();
    }

    private void updateImage() {
        if (mImageChanged) {
            mImageTexture.upload(mImageBitmap);
            mImageTexture.bind();
            mImageChanged = false;
        }
        if (mDepthMapChanged) {
            int width = mDepthMap == null ? mImageBitmap.getWidth() : mDepthMap.getWidth();
            int height = mDepthMap == null ? mImageBitmap.getHeight() : mDepthMap.getHeight();
            mMesh.update(mDepthMap, width, height, Settings.FOREGROUND_POSITION);
            mDepthMapChanged = false;
        }
    }

    private void updateMatrix() {
        float x = (float) (Math.sin(mOffsetX) * Math.cos(mOffsetY)) * Settings.CAMERA_POSITION;
        float y = (float) (Math.sin(mOffsetY)) * Settings.CAMERA_POSITION;
        float z = (float) (Math.cos(mOffsetX) * Math.cos(mOffsetY)) * Settings.CAMERA_POSITION;

        float[] viewMatrix = new float[16];
        Matrix.setLookAtM(viewMatrix, 0, x, y, z, 0f, 0f, Settings.FOREGROUND_POSITION,
                0f, 1f, 0f);
        float[] matrix = new float[16];
        Matrix.multiplyMM(matrix, 0, mProjectionMatrix, 0, viewMatrix, 0);
        mShader.setMatrix(matrix);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (mImageBitmap == null) {
            return;
        }

        updateImage();
        updateMatrix();

        GLES20.glClearColor(0, 0, 0, 0);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mMesh.render(mShader);
    }

    public void setOffset(float x, float y) {
        mOffsetX = limit(x);
        mOffsetY = limit(y);
    }

    public void setOffsetDelta(float deltaX, float deltaY) {
        mOffsetX = limit(mOffsetX + deltaX);
        mOffsetY = limit(mOffsetY + deltaY);
    }

    private float limit(float theta) {
        if (theta < -THETA_MAX) return -THETA_MAX;
        if (theta > THETA_MAX) return THETA_MAX;
        return theta;
    }

    public android.graphics.Matrix getImageInvertMatrix() {
        return mImageInvertMatrix;
    }

    private void setImageInvertMatrix() {
        if (mImageBitmap != null && mSurfaceRect != null) {
            RectF rect = new RectF(0, 0, mImageBitmap.getWidth(), mImageBitmap.getHeight());
            if (mImageInvertMatrix == null) {
                mImageInvertMatrix = new android.graphics.Matrix();
            }
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.setRectToRect(rect, mSurfaceRect, android.graphics.Matrix.ScaleToFit.CENTER);
            matrix.invert(mImageInvertMatrix);
        }
    }
}

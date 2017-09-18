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

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.LinearLayout;

import com.android.gallery3d.app.dualcam3d.threed.Controller;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.util.GDepth;

import org.codeaurora.gallery.R;

public class ThreeDimensionalActivity extends Activity {
    private static final String TAG = ThreeDimensionalActivity.class.getSimpleName();

    private GLView mImageView;
    private Controller mController;
    private LoadImageTask mTask;
    private int mWidth, mHeight;
    private int mOrientation;
    private Bitmap mBitmap = null;
    private boolean mOriChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_three_dimensional);
        init();
        processIntent();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mOrientation != newConfig.orientation) {
            mOriChanged = true;
            mOrientation = newConfig.orientation;
        }
    }


    private void init() {
        mImageView = (GLView) findViewById(R.id.image);
        mImageView.setRotation(getWindowManager().getDefaultDisplay().getRotation());
        mController = new Controller(mImageView, (LinearLayout) findViewById(R.id.mode_3d));
        mImageView.setListener(new GLView.Listener() {
            @Override
            public void onMove(float deltaX, float deltaY) {
                mController.onMove(deltaX, deltaY);
            }

            @Override
            public void onClick(float x, float y) {}

            @Override
            public void onLayout(int width, int height) {
                mWidth = width;
                mHeight = height;
                reLayoutGLView(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mImageView.onResume();
        mController.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mController.stop();
        mImageView.onPause();
    }

    @Override
    protected void onDestroy() {
        mTask.cancel(true);
        mTask = null;
        mBitmap = null;
        mImageView.recycle();
        super.onDestroy();
    }

    private void processIntent() {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) {
            finish();
            return;
        }
        mTask = new LoadImageTask();
        mTask.execute(uri);
    }

    private void reLayoutGLView(boolean force) {
        if (!mOriChanged && !force) return;
        mOriChanged = false;
        if (mBitmap == null) return;
        int width = mBitmap.getWidth();
        int height = mBitmap.getHeight();
        if (mWidth*height/width > mHeight) {
            int scaledWidth = width*mHeight/height;
            int move = (mWidth - scaledWidth)/2;
            mImageView.layout(move, 0, mWidth-move, mHeight);
        }
    }

    private class LoadImageTask extends AsyncTask<Uri, Void, GDepth.Image> {
        @Override
        protected GDepth.Image doInBackground(Uri... params) {
            GDepth.Parser parser = new GDepth.Parser();
            if (parser.parse(ThreeDimensionalActivity.this, params[0])) {
                GDepth.Image image = parser.decode();
                if (image.valid()) {
                    int orientation = ImageLoader.getMetadataOrientation(
                            ThreeDimensionalActivity.this, params[0]);
                    if (orientation != ImageLoader.ORI_NORMAL) {
                        image.bitmap = ImageLoader.orientBitmap(image.bitmap, orientation);
                        image.depthMap = ImageLoader.orientBitmap(image.depthMap, orientation);
                    }
                    return image;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(GDepth.Image image) {
            if (!isCancelled()) {
                if (image != null && image.valid()) {
                    mImageView.setImageBitmap(image.bitmap);
                    mImageView.setDepthMap(image.depthMap);
                    mBitmap = image.bitmap;
                    reLayoutGLView(true);
                } else {
                    finish();
                }
            }
        }
    }
}

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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Window;
import android.widget.LinearLayout;

import com.android.gallery3d.app.dualcam3d.mpo.Task;
import com.android.gallery3d.app.dualcam3d.threed.Controller;
import com.android.gallery3d.filtershow.tools.DualCameraNativeEngine;

import org.codeaurora.gallery.R;

public class ThreeDimensionalActivity extends Activity {
    private static final String TAG = ThreeDimensionalActivity.class.getSimpleName();

    final static int MSG_UPDATE_IMAGE = 1;
    final static int MSG_UPDATE_3D_DEPTH_MAP = 2;
    private final static int MSG_IMAGE_LOADED = 3;
    private final static int MSG_FINISH = 4;

    private Effect mEffect;
    private GLView mImageView;
    private Task mTask;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_IMAGE:
                    if (mImageView != null) {
                        mImageView.setImageBitmap((Bitmap) msg.obj);
                    }
                    break;
                case MSG_UPDATE_3D_DEPTH_MAP:
                    if (mImageView != null) {
                        mImageView.set3DEffectDepthMap((DualCameraNativeEngine.DepthMap3D) msg.obj);
                    }
                    break;
                case MSG_IMAGE_LOADED:
                    mEffect.setBitmap((Bitmap) msg.obj, msg.arg1);
                    break;
                case MSG_FINISH:
                    mEffect.set(Effect.Type.THREE_DIMENSIONAL);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_three_dimensional);

        mEffect = new Effect(this);
        processIntent();
        init();
    }

    private void init() {
        mImageView = (GLView) findViewById(R.id.image);
        mImageView.setListener(mEffect);
        mImageView.setRotation(getWindowManager().getDefaultDisplay().getRotation());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mImageView.onResume();
        if (mEffect != null) {
            mEffect.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImageView.onPause();
        if (mEffect != null) {
            mEffect.pause();
        }
    }

    @Override
    protected void onDestroy() {
        if (DualCameraNativeEngine.getInstance().isLibLoaded()) {
            DualCameraNativeEngine.getInstance().releaseDepthMap();
        }
        if (mEffect != null) {
            mEffect.recycle();
            mEffect = null;
        }
        mImageView.recycle();
        mTask.cancel();
        super.onDestroy();
    }

    private void processIntent() {
        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (uri == null) {
            finish();
            return;
        }
        startLoadImage(uri);
    }

    private void startLoadImage(Uri uri) {
        if (DualCameraNativeEngine.getInstance().isLibLoaded()) {
            mTask = new Task(this, uri, new Task.Listener() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, int scale) {
                    if (bitmap != null) {
                        mHandler.obtainMessage(MSG_IMAGE_LOADED, scale, 0, bitmap).sendToTarget();
                    } else {
                        finish();
                    }
                }

                @Override
                public int onScale(int width, int height) {
                    return Effect.scale(width, height);
                }

                @Override
                public void onFinish(boolean result) {
                    if (!result) {
                        finish();
                    } else {
                        sendMessage(MSG_FINISH, null);
                    }
                }
            });
            mTask.start(Effect.DEFAULT_BRIGHTNESS_INTENSITY);
        }
    }

    public void sendMessage(int what, Object obj) {
        mHandler.obtainMessage(what, obj).sendToTarget();
    }

    public Controller getController() {
        return new Controller(mImageView, (LinearLayout) findViewById(R.id.mode_3d));
    }
}

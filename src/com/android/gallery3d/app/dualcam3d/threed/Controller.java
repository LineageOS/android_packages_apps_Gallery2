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

package com.android.gallery3d.app.dualcam3d.threed;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.android.gallery3d.app.dualcam3d.GLView;
import com.android.gallery3d.app.dualcam3d.gl.Renderer;

import org.codeaurora.gallery.R;


public class Controller implements Gyro.Listener {
    private static final String TAG = "Controller";
    private static final float ANGLE_PER_PIXEL = (float) Math.toRadians(0.03f);

    private final GLView mGLView;
    private final LinearLayout mModeView;
    private Auto mAuto;
    private Gyro mGyro;

    public Controller(GLView glView, LinearLayout modeView) {
        mGLView = glView;
        mModeView = modeView;
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int id = v.getId();
                switch (id) {
                    case R.id.mode_gyro:
                        startGyro();
                        break;
                    case R.id.mode_auto:
                        startAuto();
                        break;
                    case R.id.mode_touch:
                        stop(false);
                        break;
                    case R.id.three_dimensional:
                        start();
                        break;
                }
            }
        };
        for (int i = modeView.getChildCount() - 1; i >= 0; --i) {
            ImageButton b = (ImageButton) modeView.getChildAt(i);
            b.setOnClickListener(listener);
        }
    }

    private boolean startGyro() {
        stop(false);
        if (mGyro == null) {
            mGyro = new Gyro(mGLView.getContext());
            if (mGyro.start()) {
                mGyro.setListener(this);
                return true;
            } else {
                mGyro = null;
                mModeView.findViewById(R.id.mode_gyro).setEnabled(false);
            }
        }
        return false;
    }

    private void startAuto() {
        stop(false);
        if (mAuto == null) {
            mAuto = new Auto();
            mAuto.start(mGLView);
        }
    }

    public void start() {
        if (!startGyro()) {
            startAuto();
        }
//		mModeView.setVisibility(View.VISIBLE);
    }

    public void stop(boolean hide) {
        if (mGyro != null) {
            mGyro.stop();
            mGyro = null;
        }
        if (mAuto != null) {
            mAuto.stop();
            mAuto = null;
        }
//		if (hide) {
//			mModeView.setVisibility(View.INVISIBLE);
//		}
    }

    @Override
    public void onGyroChanged(float thetaX, float thetaY) {
        mGLView.setOffsetWithRotation(thetaX, thetaY);
    }

    public void onMove(float deltaX, float deltaY) {
        stop(false);
        mGLView.setOffsetDelta(deltaX * ANGLE_PER_PIXEL, deltaY * ANGLE_PER_PIXEL);
    }

    private static class Auto {
        private Thread mThread;
        private boolean mStop;

        public void start(final GLView glView) {
            mStop = false;
            mThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    float x = 0, y = 0, speed = 0.003f, angle = 0.87f;
                    double deltaX = angle * speed;
                    double deltaY = Math.sqrt(speed * speed - deltaX * deltaX);

                    while (!mStop) {
                        x += deltaX;
                        y += deltaY;
                        glView.setOffset(x, y);

                        if (x >= Renderer.THETA_MAX || x <= -Renderer.THETA_MAX) {
                            deltaX = -deltaX;
                        }
                        if (y >= Renderer.THETA_MAX || y <= -Renderer.THETA_MAX) {
                            deltaY = -deltaY;
                        }
                        try {
                            Thread.sleep(15);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                }
            });
            mThread.start();
        }

        public void stop() {
            mStop = true;
            if (mThread != null) {
                try {
                    mThread.join();
                    mThread = null;
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
    }
}

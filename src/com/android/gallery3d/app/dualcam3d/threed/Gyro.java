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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

class Gyro implements SensorEventListener {
    private final SensorManager mSensorManager;
    private final Sensor mGyro;

    private static final float ALPHA = 0.4f;
    private static final float ALPHA_ORIGIN = 0.95f;

    private long mPrevTimestamp;
    private float mPrevThetaX;
    private float mPrevThetaY;
    private float mPrevOriginX;
    private float mPrevOriginY;
    private float mPrevOmegaX;
    private float mPrevOmegaY;

    private Listener mListener;

    interface Listener {
        void onGyroChanged(float thetaX, float thetaY);
    }

    Gyro(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    public boolean start() {
        return mGyro != null
                && mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            float omegaX = event.values[0];
            float omegaY = event.values[1];
            update(omegaX, omegaY, event.timestamp);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void update(float omegaX, float omegaY, long timestamp) {
        if (mPrevTimestamp == 0) {
            mPrevTimestamp = timestamp;
            return;
        }
        float deltaTimestamp = (float) (timestamp - mPrevTimestamp) / 1000_000_000;

        float thetaX = mPrevThetaX + 0.5f * (mPrevOmegaX + omegaX) * deltaTimestamp;
        float thetaY = mPrevThetaY + 0.5f * (mPrevOmegaY + omegaY) * deltaTimestamp;
        float smoothThetaX = ALPHA * mPrevThetaX + ((1.0f - ALPHA) * thetaX);
        float smoothThetaY = ALPHA * mPrevThetaY + ((1.0f - ALPHA) * thetaY);

        mPrevOriginX = ALPHA_ORIGIN * mPrevOriginX + (1 - ALPHA_ORIGIN) * smoothThetaX;
        mPrevOriginY = ALPHA_ORIGIN * mPrevOriginY + (1 - ALPHA_ORIGIN) * smoothThetaY;

        mPrevTimestamp = timestamp;
        mPrevOmegaX = omegaX;
        mPrevOmegaY = omegaY;
        mPrevThetaX = smoothThetaX;
        mPrevThetaY = smoothThetaY;

        if (mListener != null) {
            mListener.onGyroChanged(smoothThetaX - mPrevOriginX, smoothThetaY - mPrevOriginY);
        }
    }

    void setListener(Listener listener) {
        mListener = listener;
    }
}

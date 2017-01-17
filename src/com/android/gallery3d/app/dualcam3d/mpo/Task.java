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

package com.android.gallery3d.app.dualcam3d.mpo;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import com.android.gallery3d.filtershow.tools.DualCameraNativeEngine;
import com.android.gallery3d.mpo.MpoParser;


import java.io.OutputStream;

public class Task {
    private static final String TAG = "ParseMpoTask";

    public interface Listener {
        void onBitmapLoaded(Bitmap bitmap, int scale);

        int onScale(int width, int height);

        void onFinish(boolean result);
    }

    private final Context mContext;
    private final Uri mUri;
    private final Listener mListener;
    private boolean mCancelled;

    private Thread mThread1;
    private Thread mThread2;

    public Task(final Context context, final Uri uri, final Listener listener) {
        mContext = context;
        mUri = uri;
        mListener = listener;
        mCancelled = false;
    }

    public void start(final float brIntensity) {
        mThread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean result = parse(brIntensity);
                mListener.onFinish(result);
            }
        });
        mThread1.start();
    }

    public void cancel() {
        mCancelled = true;
        try {
            if (mThread1 != null) {
                mThread1.join();
            }
            if (mThread2 != null) {
                mThread2.join();
            }
        } catch (InterruptedException ignored) {
        }
    }

    private boolean parse(final float brIntensity) {
        final Bitmap[] bitmaps = new Bitmap[2];
        final byte[][] data = new byte[2][];

        MpoParser parser = MpoParser.parse(mContext, mUri);
        if (mCancelled) return false;
        final boolean primaryForDisplay = parser.isPrimaryForDisplay();
        if (!primaryForDisplay) {
            decodeDisplayImg(parser);
            try {
                mThread2.join();
                mThread2 = null;
            } catch (InterruptedException ignored) {
            }
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                bitmaps[0] = BitmapFactory.decodeByteArray(data[0], 0, data[0].length);
                data[0] = null;
                if (!mCancelled && primaryForDisplay && brIntensity == 0) {
                    mListener.onBitmapLoaded(bitmaps[0], 1);
                }
            }
        };
        data[0] = parser.readImgData(true);
        if (mCancelled || data[0] == null) return false;
        if (primaryForDisplay) {
            mThread2 = new Thread(runnable);
            mThread2.start();
        } else {
            runnable.run();
        }

        data[1] = parser.readImgData(false);
        if (mCancelled || data[1] == null) return false;
        bitmaps[1] = BitmapFactory.decodeByteArray(data[1], 0, data[1].length);
        data[1] = null;
        if (bitmaps[1] == null) return false;

        if (primaryForDisplay) {
            try {
                mThread2.join();
                mThread2 = null;
            } catch (InterruptedException ignored) {
            }
        }
        return loadDepthMap(bitmaps, brIntensity, primaryForDisplay);
    }

    private void decodeDisplayImg(final MpoParser parser) {
        mThread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                byte[] data = parser.readImgData(0);
                if (data == null && !mCancelled) {
                    mListener.onBitmapLoaded(null, 1);
                    return;
                }
                if (mCancelled) return;
                BitmapFactory.decodeByteArray(data, 0, data.length, o);
                o.inSampleSize = mListener.onScale(o.outWidth, o.outHeight);
                o.inJustDecodeBounds = false;
                if (mCancelled) return;
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, o);
                if (mCancelled) return;
                mListener.onBitmapLoaded(bitmap, o.inSampleSize);
            }
        });
        mThread2.start();
    }

    public static String getLocalPathFromUri(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri,
                new String[]{MediaStore.Images.Media.DATA}, null, null, null);
        if (cursor == null) {
            return null;
        }
        int index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        String s = cursor.moveToFirst() ? cursor.getString(index) : null;
        cursor.close();
        return s;
    }

    private boolean loadDepthMap(Bitmap[] bitmaps, float brIntensity, boolean primaryForDisplay) {
        // check for pre-generated dm file
        String mpoFilepath = getLocalPathFromUri(mContext, mUri);
        DualCameraNativeEngine engine = DualCameraNativeEngine.getInstance();

        if (mCancelled) return false;
        boolean ok = engine.initDepthMap(bitmaps[0], bitmaps[1], mpoFilepath,
                engine.getCalibFilepath(mContext), brIntensity);
        bitmaps[1].recycle();
        if (!ok) return false;

        Point size = new Point();
        ok = engine.getDepthMapSize(size);
        if (ok) {
            Log.d(TAG, "ddm size: " + size.x + "x" + size.y);
            if (size.x == 0 || size.y == 0) {
                Log.w(TAG, "invalid ddm size: " + size.x + "x" + size.y);
            } else {
                Bitmap depthMap = Bitmap.createBitmap(size.x, size.y, Bitmap.Config.ALPHA_8);
                if (engine.getDepthMap(depthMap)) {
                    if (!mCancelled && brIntensity != 0 && primaryForDisplay) {
                        scaleBitmap(bitmaps[0]);
                    }
                    return true;
                } else {
                    Log.w(TAG, "getDepthMap returned false");
                }
            }
        } else {
            Log.w(TAG, "getDepthMapSize returned false");
        }
        return false;
    }

    private void save(Bitmap bitmap, String name) {
        try {
            OutputStream os = mContext.openFileOutput(name, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, os);
            os.close();
        } catch (Exception ignored) {
        }
    }

    private void scaleBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth(), height = bitmap.getHeight();
        int scale = mListener.onScale(width, height);
        width /= scale;
        height /= scale;

        Bitmap b = Bitmap.createScaledBitmap(bitmap, width, height, false);
        int[] roi = new int[4];
        boolean result = DualCameraNativeEngine.getInstance().getPrimaryImg(0, 0, roi, true, b);
        if (result && roi[2] != width && roi[3] != height) {
            b = Bitmap.createBitmap(b, roi[0], roi[1], roi[2], roi[3]);
        }
        mListener.onBitmapLoaded(b, scale);
    }
}
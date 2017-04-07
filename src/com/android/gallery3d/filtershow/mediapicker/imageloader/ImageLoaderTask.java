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

package com.android.gallery3d.filtershow.mediapicker.imageloader;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.ReentrantLock;

final class ImageLoaderTask implements Runnable {

    private final String TAG = "ImageLoaderTask";
    private final ImageLoaderHandle handle;
    private final ImageLoaderInfo imageLoadingInfo;
    private final Handler handler;

    final String uri;
    private final String memoryCacheKey;
    final ImageViewImpl imageView;
    private int imageHeight, imageWidth;

    public ImageLoaderTask(ImageLoaderHandle handle, ImageLoaderInfo imageLoadingInfo, Handler handler) {
        this.handle = handle;
        this.imageLoadingInfo = imageLoadingInfo;
        this.handler = handler;

        uri = imageLoadingInfo.uri;
        memoryCacheKey = imageLoadingInfo.memoryCacheKey;
        imageView = imageLoadingInfo.imageView;
        imageHeight = imageLoadingInfo.targetHeight;
        imageWidth = imageLoadingInfo.targetWidth;
    }

    @Override
    public void run() {
        if (isTaskAvailable()) return;

        ReentrantLock loadFromUriLock = imageLoadingInfo.loadFromUriLock;
        Log.d(TAG, "Start display image task " + memoryCacheKey);
        loadFromUriLock.lock();
        Bitmap bmp = null;
        try {
            checkTaskNotActual();
            checkTaskInterrupted();
            bmp = handle.configuration.get(memoryCacheKey);
            if (bmp == null || bmp.isRecycled()) {
                bmp = parseImage(uri);
                if (bmp == null) return;

                checkTaskNotActual();
                checkTaskInterrupted();
                if (bmp != null) {
                    Log.d(TAG, "Cache image in memory " + memoryCacheKey);
                    handle.configuration.put(memoryCacheKey, bmp);
                }
            } else {
                Log.d(TAG, "Get cached bitmap from memory after waiting. " + memoryCacheKey);
            }
            checkTaskNotActual();
            checkTaskInterrupted();
        } catch (TaskInvalidException e) {
            return;
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        } catch (OutOfMemoryError e) {
            Log.e(TAG, e.toString());
        } catch (Throwable e) {
            Log.e(TAG, e.toString());
        } finally {
            loadFromUriLock.unlock();
        }
        ImageDisplayTask displayBitmapTask = new ImageDisplayTask(bmp, imageLoadingInfo, handle);
        runTask(displayBitmapTask, handler, handle);
    }


    private Bitmap parseImage(String imageUri) throws IOException {
        ViewScaleType viewScaleType = imageView.getScaleType();
        Bitmap parsedBitmap;
        BitmapFactory.Options opt;

        InputStream imageStream = getImageStream(imageUri);
        try {
            opt = getImageOpt(imageStream);
            imageStream = resetStream(imageStream, imageUri);
            int scale = computeImageSampleSize(opt, imageHeight, imageWidth, viewScaleType);
            BitmapFactory.Options decodingOptions = new BitmapFactory.Options();
            decodingOptions.inSampleSize = scale;
            parsedBitmap = BitmapFactory.decodeStream(imageStream, null, decodingOptions);
        } finally {
            closeSilently(imageStream);
        }

        if (parsedBitmap == null) {
            Log.e(TAG, "Image can't be parsed: " + memoryCacheKey);
        } else {
            parsedBitmap = checkScale(parsedBitmap);
        }
        return parsedBitmap;
    }

    public static void closeSilently(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            // Just catched here.
        }
    }

    protected InputStream getImageStream(String imageUri) throws FileNotFoundException {
        ContentResolver res = handle.configuration.context.getContentResolver();
        Uri uri = Uri.parse(imageUri);
        return res.openInputStream(uri);
    }

    protected BitmapFactory.Options getImageOpt(InputStream imageStream)
            throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(imageStream, null, options);

        return options;
    }

    protected InputStream resetStream(InputStream imageStream, String imageUri) throws IOException {
        try {
            imageStream.reset();
        } catch (IOException e) {
            closeSilently(imageStream);
            imageStream = getImageStream(imageUri);
        }
        return imageStream;
    }

    public static int computeImageSampleSize(BitmapFactory.Options opt, int h, int w,
                                             ViewScaleType viewScaleType) {
        final int width = opt.outWidth/2;
        final int height = opt.outHeight/2;
        final int maxWidth = 2048;
        final int maxHeight = 2048;

        int scale = 1;
        switch (viewScaleType) {
            case FIT_INSIDE:
                while ((width / scale) > w || (height / scale) > h) {
                    scale *= 2;
                }
                break;
            case CROP:
                while ((width / scale) > w && (height / scale) > h) {
                        scale *= 2;
                }
                break;
        }
        while ((opt.outWidth / scale) > maxWidth || (opt.outHeight / scale) > maxHeight) {
            scale *= 2;
        }
        return scale;
    }

    protected Bitmap checkScale(Bitmap subsampledBitmap) {
        Matrix m = new Matrix();

        Bitmap finalBitmap = Bitmap.createBitmap(subsampledBitmap, 0, 0, subsampledBitmap.getWidth(),
                subsampledBitmap.getHeight(), m, true);
        if (finalBitmap != subsampledBitmap) {
            subsampledBitmap.recycle();
        }
        return finalBitmap;
    }

    private boolean isTaskAvailable() {
        if (isRecyled() || isCached())
            return true;
        return false;
    }

    private void checkTaskNotActual() throws TaskInvalidException {
        if (isRecyled()) {
            throw new TaskInvalidException();
        }
        if (isCached()) {
            throw new TaskInvalidException();
        }
    }

    private boolean isRecyled() {
        if (imageView.isRecyled()) {
            Log.d(TAG,"ImageAware was collected by GC. Task is cancelled: " + memoryCacheKey);
            return true;
        }
        return false;
    }

    private boolean isCached() {
        String cntCacheKey = handle.cacheKeysForImageAwares.get(imageView.getId());
        if (!memoryCacheKey.equals(cntCacheKey)) {
            Log.d(TAG, "Imageview is cached for another image: " + memoryCacheKey);
            return true;
        }
        return false;
    }

    private void checkTaskInterrupted() throws TaskInvalidException {
        if (Thread.interrupted()) {
            throw new TaskInvalidException();
        }
    }

    static void runTask(Runnable r, Handler handler, ImageLoaderHandle handle) {
        if (handler == null) {
            handle.taskDistributor.execute(r);
        } else {
            handler.post(r);
        }
    }

    class TaskInvalidException extends Exception {
    }
}



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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class ImageLoaderConfig {

    private final String TAG = "ImageLoaderConfig";
    private static final String URI_AND_SIZE_SEPARATOR = "_";
    private static final String WIDTH_AND_HEIGHT_SEPARATOR = "x";
    final Resources resources;

    public static final int DEFAULT_THREAD_POOL_SIZE = 4;
    public static final int DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY - 2;
    public Context context;

    public Executor taskExecutor = null;
    public boolean customExecutor = false;

    public int threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
    public int threadPriority = DEFAULT_THREAD_PRIORITY;

    private LinkedHashMap<String, Bitmap> imageCacheMap;
    private int icmMaxSize;
    private int icmSize;
    public ImageLoaderOptions defaultOptions = null;

    public ImageLoaderConfig(Context context) {
        this.context = context.getApplicationContext();
        resources = context.getResources();
        initEmptyFieldsWithDefaultValues();
    }

    public void cacheMapSizePercentage(int availableMemoryPercent) {
        if (availableMemoryPercent <= 0 || availableMemoryPercent >= 100) {
            throw new IllegalArgumentException("availableMemoryPercent must be in range (0 < % < 100)");
        }

        long availableMemory = Runtime.getRuntime().maxMemory();
        icmMaxSize = (int) (availableMemory * (availableMemoryPercent / 100f));
    }

    private void initEmptyFieldsWithDefaultValues() {
        if (taskExecutor == null) {
            taskExecutor = createExecutor(threadPoolSize, threadPriority);
        } else {
            customExecutor = true;
        }
        if (imageCacheMap == null) {
            icmSize = 0;
            imageCacheMap = new LinkedHashMap<String, Bitmap>(0, 0.75f, true);
        }
        if (defaultOptions == null) {
            defaultOptions = new ImageLoaderOptions();
        }
    }

    public final Bitmap get(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        synchronized (this) {
            return imageCacheMap.get(key);
        }
    }

    public final boolean put(String key, Bitmap value) {
        if (key == null || value == null) {
            throw new NullPointerException("key == null || value == null");
        }

        synchronized (this) {
            icmSize += sizeOf(value);
            Bitmap previous = imageCacheMap.put(key, value);
            if (previous != null) {
                icmSize -= sizeOf(previous);
            }
        }

        trimToSize(icmMaxSize);
        return true;
    }

    private void trimToSize(int maxSize) {
        while (true) {
            String key;
            Bitmap value;
            synchronized (this) {
                if (icmSize < 0 || (imageCacheMap.isEmpty() && icmSize != 0)) {
                    throw new IllegalStateException(getClass().getName() + ".sizeOf() is inconsistent!");
                }

                if (icmSize <= maxSize || imageCacheMap.isEmpty()) {
                    break;
                }

                Map.Entry<String, Bitmap> toEvict = imageCacheMap.entrySet().iterator().next();
                if (toEvict == null) {
                    break;
                }
                key = toEvict.getKey();
                value = toEvict.getValue();
                imageCacheMap.remove(key);
                icmSize -= sizeOf(value);
            }
        }
    }

    public final Bitmap remove(String key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

        synchronized (this) {
            Bitmap previous = imageCacheMap.remove(key);
            if (previous != null) {
                icmSize -= sizeOf(previous);
            }
            return previous;
        }
    }

    private int sizeOf(Bitmap value) {
        return value.getRowBytes() * value.getHeight();
    }


    public int defineHeightForImage(ImageViewImpl imageView) {
        int height = imageView.getHeight();
        if (height <= 0)
            height = resources.getDisplayMetrics().heightPixels;
        return height;
    }

    public int defineWidthForImage(ImageViewImpl imageView) {
        int width = imageView.getWidth();
        if (width <= 0)
            width = resources.getDisplayMetrics().widthPixels;
        return width;
    }

    public static String generateKey(String imageUri, int w, int h) {
        return new StringBuilder(imageUri).append(URI_AND_SIZE_SEPARATOR).append(w)
                .append(WIDTH_AND_HEIGHT_SEPARATOR).append(h).toString();
    }

    public static Executor createExecutor(int threadPoolSize, int threadPriority) {
        BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>();
        return new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0L, TimeUnit.MILLISECONDS,
                taskQueue, new ThreadFactoryImpl(threadPriority));
    }

    public static Handler defineHandler(ImageLoaderOptions options) {
        Handler handler = options.getHandler();
        if (handler == null && Looper.myLooper() == Looper.getMainLooper()) {
            handler = new Handler();
        }
        return handler;
    }
}

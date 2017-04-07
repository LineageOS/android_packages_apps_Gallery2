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

import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;

public class ImageLoaderInstance {

    private ImageLoaderHandle handle;
    private ImageLoaderConfig configuration;
    private volatile static ImageLoaderInstance instance;
    private static final String TAG = "ImageLoaderInstance";

    /** Returns singleton class instance */
    public static ImageLoaderInstance getInstance() {
        if (instance == null) {
            synchronized (ImageLoaderInstance.class) {
                if (instance == null) {
                    instance = new ImageLoaderInstance();
                }
            }
        }
        return instance;
    }

    protected ImageLoaderInstance() {
    }

    public synchronized void init(ImageLoaderConfig configuration) {
        if (this.configuration == null) {
            handle = new ImageLoaderHandle(configuration);
            this.configuration = configuration;
        }
    }

    public void displayImage(String uri, ImageViewImpl imageView, ImageLoaderOptions options){
        if (imageView == null) {
            throw new IllegalArgumentException("Null iamgeView were passed.");
        }
        if (TextUtils.isEmpty(uri)) {
            handle.cacheKeysForImageAwares.remove(imageView.getId());
            imageView.setImageDrawable(options.getImageForEmptyUri(configuration.resources));
            return;
        }

        int imageHeight = configuration.defineHeightForImage(imageView);
        int imageWidth = configuration.defineWidthForImage(imageView);
        String memoryCacheKey = configuration.generateKey(uri, imageHeight, imageWidth);
        handle.prepareDisplayTaskFor(imageView, memoryCacheKey);

        Bitmap bmp = configuration.get(memoryCacheKey);
        if (bmp != null && !bmp.isRecycled()) {
            Log.d(TAG, "Load image from memory cache " + memoryCacheKey);
            imageView.setImageBitmap(bmp);
        } else {
            imageView.setImageDrawable(options.getImageOnLoading(configuration.resources));
            ImageLoaderInfo imageLoadingInfo = new ImageLoaderInfo(uri, imageView, imageHeight, imageWidth,
                    memoryCacheKey, options, handle.getLockForUri(uri));
            ImageLoaderTask displayTask = new ImageLoaderTask(handle, imageLoadingInfo,
                    configuration.defineHandler(options));
            handle.submit(displayTask);
        }
    }
}

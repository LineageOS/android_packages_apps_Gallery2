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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

class ImageLoaderHandle {

    final ImageLoaderConfig configuration;

    public Executor taskExecutor;
    public Executor taskDistributor;

    public final Map<Integer, String> cacheKeysForImageAwares = Collections
            .synchronizedMap(new HashMap<Integer, String>());
    private final Map<String, ReentrantLock> uriLocks = new WeakHashMap<String, ReentrantLock>();
    private final Object pauseLock = new Object();

    ImageLoaderHandle(ImageLoaderConfig configuration) {
        this.configuration = configuration;

        taskExecutor = configuration.taskExecutor;
        taskDistributor = createTaskDistributor();
    }

    /** Submits task to execution pool */
    void submit(final ImageLoaderTask task) {
        taskDistributor.execute(new Runnable() {
            @Override
            public void run() {
                if (!configuration.customExecutor && ((ExecutorService) taskExecutor).isShutdown()) {
                    taskExecutor = configuration.createExecutor(configuration.threadPoolSize,
                            configuration.threadPriority);
                }
                taskExecutor.execute(task);
            }
        });
    }

    ReentrantLock getLockForUri(String uri) {
        ReentrantLock lock = uriLocks.get(uri);
        if (lock == null) {
            lock = new ReentrantLock();
            uriLocks.put(uri, lock);
        }
        return lock;
    }

    public static Executor createTaskDistributor() {
        return Executors.newCachedThreadPool(new ThreadFactoryImpl(Thread.NORM_PRIORITY));
    }

    void prepareDisplayTaskFor(ImageViewImpl imageView, String memoryCacheKey) {
        cacheKeysForImageAwares.put(imageView.getId(), memoryCacheKey);
    }
}


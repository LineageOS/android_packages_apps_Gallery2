/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gallery3d.filtershow.filters;

import android.graphics.Bitmap;

import com.android.gallery3d.R;

public class ImageFilterEdge extends SimpleImageFilter {
    private static final String SERIALIZATION_NAME = "EDGE";
    public ImageFilterEdge() {
        mName = "Edge";
    }

    public FilterRepresentation getDefaultRepresentation() {
        FilterRepresentation representation = super.getDefaultRepresentation();
        representation.setName("Edge");
        representation.setSerializationName(SERIALIZATION_NAME);
        representation.setFilterClass(ImageFilterEdge.class);
        representation.setTextId(R.string.edge);
        representation.setSupportsPartialRendering(true);
        return representation;
    }

    native protected void nativeApplyFilter(Bitmap bitmap, int w, int h, float p);

    @Override
    public Bitmap apply(Bitmap bitmap, float scaleFactor, int quality) {
        if (getParameters() == null) {
            return bitmap;
        }
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        float p = getParameters().getValue() + 101;
        p = (float) p / 100;
        nativeApplyFilter(bitmap, w, h, p);
        return bitmap;
    }
}

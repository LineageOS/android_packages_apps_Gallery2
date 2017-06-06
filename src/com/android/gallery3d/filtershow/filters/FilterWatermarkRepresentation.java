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
package com.android.gallery3d.filtershow.filters;


import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.Gravity;

import com.android.gallery3d.filtershow.category.WaterMarkView;

import org.codeaurora.gallery.R;

public class FilterWatermarkRepresentation extends FilterRepresentation {
    public static final String NAME_LOCATION = "LOCATION";
    public static final String NAME_TIME = "TIME";
    public static final String NAME_WEATHER = "WEATHER";
    public static final String NAME_EMOTION = "EMOTION";
    public static final String NAME_FOOD = "FOOD";

    public static final int LOCATION = 12;
    public static final int TIME = 13;
    public static final int WEATHER = 14;
    public static final int EMOTIONS = 15;
    public static final int FOOD = 16;
    private int adapterId;
    private int waterMarkId;
    private int markAlpha = -1;
    private int markType = -1;
    private String textHint = "HELLO";
    private PositionInfo positionInfo;
    private Context mContext;
    private WaterMarkView currentMarkView;

    //for main panel
    public FilterWatermarkRepresentation(Context context, String name, int category) {
        this(name);
        mContext = context;
        setFilterType(FilterRepresentation.TYPE_WATERMARK_CATEGORY);
        loadCategory(category);
    }

    //for level-2 panel
    public FilterWatermarkRepresentation(Context context, String name, String textHint) {
        this(name);
        mContext = context;
        setFilterClass(SaveWaterMark.class);
        setName("");
        setFilterType(FilterRepresentation.TYPE_WATERMARK);
        setCurrentTheme(new ContextThemeWrapper(mContext, R.style.DefaultFillColor).getTheme());
        if (textHint != null) {
            this.textHint = textHint;
        }
    }

    public FilterWatermarkRepresentation(String name) {
        super(name);
        setSerializationName(name);
        setShowParameterValue(false);
        setSupportsPartialRendering(true);
        setSvgOverlay(true);
        setOverlayOnly(true);
    }

    private void loadCategory(int category) {
        switch (category) {
            case LOCATION:
                setAdapterId(LOCATION);
                break;
            case TIME:
                setAdapterId(TIME);
                break;
            case WEATHER:
                setAdapterId(WEATHER);
                break;
            case EMOTIONS:
                setAdapterId(EMOTIONS);
                break;
            case FOOD:
                setAdapterId(FOOD);
                break;
        }
    }

    public int getAdapterId() {
        return adapterId;
    }

    public void setAdapterId(int adapterId) {
        this.adapterId = adapterId;
    }


    public int getWaterMarkId() {
        return waterMarkId;
    }

    public void setWaterMarkId(int waterMarkId) {
        this.waterMarkId = waterMarkId;
    }

    public void setMarkAlpha(int alpha) {
        markAlpha = alpha;
    }

    public WaterMarkView getWaterMarkView() {
        return getWaterMarkView(textHint);
    }

    public WaterMarkView getWaterMarkView(String textHint) {
        if (currentMarkView != null) {
            return currentMarkView;
        }
        currentMarkView = new WaterMarkView(mContext,
                mContext.getResources().getDrawable(getWaterMarkId(), getCurrentTheme()), textHint);
        if (positionInfo != null) {
            currentMarkView.setTextPosition(positionInfo.marginLeft, positionInfo.marginTop,
                    positionInfo.marginRight, positionInfo.marginBottom, positionInfo.gravity, positionInfo.isDip);
        } else {
            currentMarkView.setTextVisibility(false);
        }
        currentMarkView.setImageAlpha(markAlpha >= 0 ? markAlpha : 128);
        return currentMarkView;
    }

    public void setMarkType(int markType) {
        this.markType = markType;
    }

    public int getMarkType() {
        return markType;
    }

    public void setTextHint(String textHint) {
        this.textHint = textHint;
    }

    public String getTextHint() {
        return textHint;
    }

    public void reset() {
        currentMarkView = null;
    }

    protected class PositionInfo {
        protected int marginLeft;
        protected int marginTop;
        protected int marginRight;
        protected int marginBottom;
        protected int gravity;
        protected boolean isDip;
        public PositionInfo() {
            this(0, 0, 0, 0, Gravity.NO_GRAVITY, false);
        }

        public PositionInfo(int left, int top, int right, int bottom) {
            this(left, top, right, bottom, Gravity.NO_GRAVITY, false);
        }

        public PositionInfo(int left, int top, int right, int bottom, int gravity) {
            this(left, top, right, bottom, gravity, false);
        }

        public PositionInfo(int left, int top, int right, int bottom, boolean isDip) {
            this(left, top, right, bottom, Gravity.NO_GRAVITY, isDip);
        }

        public PositionInfo(int left, int top, int right, int bottom, int gravity, boolean isDip) {
            marginLeft = left;
            marginTop = top;
            marginRight = right;
            marginBottom = bottom;
            this.gravity = gravity;
            this.isDip = isDip;
            FilterWatermarkRepresentation.this.positionInfo = PositionInfo.this;
        }
    }
}

/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;

import org.codeaurora.gallery.R;

import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.data.FilterPresetDBHelper;
import com.android.gallery3d.filtershow.data.FilterPresetSource;
import com.android.gallery3d.filtershow.data.FilterPresetSource.SaveOption;
import com.android.gallery3d.filtershow.editors.EditorTruePortraitBasic;
import com.android.gallery3d.filtershow.editors.EditorTruePortraitImageOnly;
import com.android.gallery3d.filtershow.editors.ImageOnlyEditor;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public abstract class BaseFiltersManager implements FiltersManagerInterface {
    protected HashMap<Class, ImageFilter> mFilters = null;
    protected HashMap<String, FilterRepresentation> mRepresentationLookup = null;
    private static final String LOGTAG = "BaseFiltersManager";

    protected ArrayList<FilterRepresentation> mLooks = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mBorders = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mTools = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mEffects = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mMakeup = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mDualCam = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mTrueScanner = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mHazeBuster = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mSeeStraight = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mTruePortrait = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mFilterPreset = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mWaterMarks = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mLocations = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mTimes = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mWeathers = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mEmotions = new ArrayList<>();
    protected ArrayList<FilterRepresentation> mFoods = new ArrayList<>();
    private static int mImageBorderSize = 4; // in percent

    protected void init() {
        mFilters = new HashMap<Class, ImageFilter>();
        mRepresentationLookup = new HashMap<String, FilterRepresentation>();
        Vector<Class> filters = new Vector<Class>();
        addFilterClasses(filters);
        addTrueScannerClasses(filters);
        addHazeBusterClasses(filters);
        addSeeStraightClasses(filters);
        for (Class filterClass : filters) {
            try {
                Object filterInstance = filterClass.newInstance();
                if (filterInstance instanceof ImageFilter) {
                    mFilters.put(filterClass, (ImageFilter) filterInstance);

                    FilterRepresentation rep =
                            ((ImageFilter) filterInstance).getDefaultRepresentation();
                    if (rep != null) {
                        addRepresentation(rep);
                    }
                }
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public void addRepresentation(FilterRepresentation rep) {
        mRepresentationLookup.put(rep.getSerializationName(), rep);
    }

    public FilterRepresentation createFilterFromName(String name) {
        try {
            return mRepresentationLookup.get(name).copy();
        } catch (Exception e) {
            Log.v(LOGTAG, "unable to generate a filter representation for \"" + name + "\"");
            e.printStackTrace();
        }
        return null;
    }

    public ImageFilter getFilter(Class c) {
        return mFilters.get(c);
    }

    @Override
    public ImageFilter getFilterForRepresentation(FilterRepresentation representation) {
        return mFilters.get(representation.getFilterClass());
    }

    public FilterRepresentation getRepresentation(Class c) {
        ImageFilter filter = mFilters.get(c);
        if (filter != null) {
            return filter.getDefaultRepresentation();
        }
        return null;
    }

    public void freeFilterResources(ImagePreset preset) {
        if (preset == null) {
            return;
        }
        Vector<ImageFilter> usedFilters = preset.getUsedFilters(this);
        for (Class c : mFilters.keySet()) {
            ImageFilter filter = mFilters.get(c);
            if (!usedFilters.contains(filter)) {
                filter.freeResources();
            }
        }
    }

    public void freeRSFilterScripts() {
        for (Class c : mFilters.keySet()) {
            ImageFilter filter = mFilters.get(c);
            if (filter != null && filter instanceof ImageFilterRS) {
                ((ImageFilterRS) filter).resetScripts();
            }
        }
    }

    protected void addFilterClasses(Vector<Class> filters) {
        filters.add(ImageFilterTinyPlanet.class);
        filters.add(ImageFilterRedEye.class);
        filters.add(ImageFilterWBalance.class);
        filters.add(ImageFilterExposure.class);
        filters.add(ImageFilterVignette.class);
        filters.add(ImageFilterGrad.class);
        filters.add(ImageFilterContrast.class);
        filters.add(ImageFilterShadows.class);
        filters.add(ImageFilterHighlights.class);
        filters.add(ImageFilterVibrance.class);
        filters.add(ImageFilterSharpen.class);
        filters.add(ImageFilterCurves.class);
        filters.add(ImageFilterDraw.class);
        filters.add(ImageFilterHue.class);
        filters.add(ImageFilterChanSat.class);
        filters.add(ImageFilterSaturated.class);
        filters.add(ImageFilterBwFilter.class);
        filters.add(ImageFilterNegative.class);
        filters.add(ImageFilterEdge.class);
        filters.add(ImageFilterKMeans.class);
        filters.add(ImageFilterFx.class);
        filters.add(ImageFilterBorder.class);
        filters.add(ImageFilterColorBorder.class);
        filters.add(ImageFilterDualCamera.class);
        filters.add(ImageFilterDualCamFusion.class);
        filters.add(ImageFilterTruePortrait.class);
        filters.add(ImageFilterTruePortraitFusion.class);
        filters.add(ImageFilterPreset.class);
        filters.add(SaveWaterMark.class);

        if(SimpleMakeupImageFilter.HAS_TS_MAKEUP) {
            filters.add(ImageFilterMakeupWhiten.class);
            filters.add(ImageFilterMakeupSoften.class);
            filters.add(ImageFilterMakeupTrimface.class);
            filters.add(ImageFilterMakeupBigeye.class);
        }
    }

    protected void addTrueScannerClasses(Vector<Class> filters) {
        filters.add(TrueScannerActs.class);
    }

    protected void addHazeBusterClasses(Vector<Class> filters) {
        filters.add(HazeBusterActs.class);
    }

    protected void addSeeStraightClasses(Vector<Class> filters) {
        filters.add(SeeStraightActs.class);
    }

    public ArrayList<FilterRepresentation> getLooks() {
        return mLooks;
    }

    public ArrayList<FilterRepresentation> getBorders() {
        return mBorders;
    }

    public ArrayList<FilterRepresentation> getDualCamera() {
        return mDualCam;
    }

    public ArrayList<FilterRepresentation> getTruePortrait() {
        return mTruePortrait;
    }

    public ArrayList<FilterRepresentation> getFilterPreset(){ return mFilterPreset; }

    public ArrayList<FilterRepresentation> getTools() {
        return mTools;
    }

    public ArrayList<FilterRepresentation> getEffects() {
        return mEffects;
    }

    public ArrayList<FilterRepresentation> getMakeup() {
        return mMakeup;
    }
    public ArrayList<FilterRepresentation> getTrueScanner() {
        return mTrueScanner;
    }
    public ArrayList<FilterRepresentation> getHazeBuster() {
        return mHazeBuster;
    }
    public ArrayList<FilterRepresentation> getSeeStraight() {
        return mSeeStraight;
    }
    public ArrayList<FilterRepresentation> getWaterMarks() {
        return mWaterMarks;
    }
    public ArrayList<FilterRepresentation> getLocations() {
        return mLocations;
    }
    public ArrayList<FilterRepresentation> getTimes() {
        return mTimes;
    }
    public ArrayList<FilterRepresentation> getWeathers() {
        return mWeathers;
    }
    public ArrayList<FilterRepresentation> getEmotions() {
        return mEmotions;
    }
    public ArrayList<FilterRepresentation> getFoods() {
        return mFoods;
    }

    public void addBorders(Context context) {
        // Do not localize
        String[] serializationNames = {
                "FRAME_4X5",
                "FRAME_BRUSH",
                "FRAME_GRUNGE",
                "FRAME_SUMI_E",
                "FRAME_TAPE",
                "FRAME_BLACK",
                "FRAME_BLACK_ROUNDED",
                "FRAME_WHITE",
                "FRAME_WHITE_ROUNDED",
                "FRAME_CREAM",
                "FRAME_CREAM_ROUNDED"
        };

        // The "no border" implementation
        int i = 0;
        FilterRepresentation rep = new FilterImageBorderRepresentation(0);
        mBorders.add(rep);

        // Regular borders
        ArrayList <FilterRepresentation> borderList = new ArrayList<FilterRepresentation>();


        rep = new FilterImageBorderRepresentation(R.drawable.filtershow_border_4x5);
        borderList.add(rep);

        rep = new FilterImageBorderRepresentation(R.drawable.filtershow_border_brush);
        borderList.add(rep);

        rep = new FilterImageBorderRepresentation(R.drawable.filtershow_border_grunge);
        borderList.add(rep);

        rep = new FilterImageBorderRepresentation(R.drawable.filtershow_border_sumi_e);
        borderList.add(rep);

        rep = new FilterImageBorderRepresentation(R.drawable.filtershow_border_tape);
        borderList.add(rep);

        rep = new FilterColorBorderRepresentation(Color.BLACK, mImageBorderSize, 0);
        borderList.add(rep);

        rep = new FilterColorBorderRepresentation(Color.BLACK, mImageBorderSize,
                mImageBorderSize);
        borderList.add(rep);

        rep = new FilterColorBorderRepresentation(Color.WHITE, mImageBorderSize, 0);
        borderList.add(rep);

        rep = new FilterColorBorderRepresentation(Color.WHITE, mImageBorderSize,
                mImageBorderSize);
        borderList.add(rep);

        int creamColor = Color.argb(255, 237, 237, 227);
        rep = new FilterColorBorderRepresentation(creamColor, mImageBorderSize, 0);
        borderList.add(rep);

        rep = new FilterColorBorderRepresentation(creamColor, mImageBorderSize,
                mImageBorderSize);
        borderList.add(rep);

        for (FilterRepresentation filter : borderList) {
            filter.setSerializationName(serializationNames[i++]);
            addRepresentation(filter);
            mBorders.add(filter);
        }

    }

    public void addLooks(Context context) {
        int[] drawid = {
                R.drawable.filtershow_fx_0005_punch,
                R.drawable.filtershow_fx_0000_vintage,
                R.drawable.filtershow_fx_0004_bw_contrast,
                R.drawable.filtershow_fx_0002_bleach,
                R.drawable.filtershow_fx_0001_instant,
                R.drawable.filtershow_fx_0007_washout,
                R.drawable.filtershow_fx_0003_blue_crush,
                R.drawable.filtershow_fx_0008_washout_color,
                R.drawable.filtershow_fx_0006_x_process
        };

        int[] fxNameid = {
                R.string.ffx_punch,
                R.string.ffx_vintage,
                R.string.ffx_bw_contrast,
                R.string.ffx_bleach,
                R.string.ffx_instant,
                R.string.ffx_washout,
                R.string.ffx_blue_crush,
                R.string.ffx_washout_color,
                R.string.ffx_x_process
        };

        // Do not localize.
        String[] serializationNames = {
                "LUT3D_PUNCH",
                "LUT3D_VINTAGE",
                "LUT3D_BW",
                "LUT3D_BLEACH",
                "LUT3D_INSTANT",
                "LUT3D_WASHOUT",
                "LUT3D_BLUECRUSH",
                "LUT3D_WASHOUT_COLOR",
                "LUT3D_XPROCESS"
        };

        int[] colorId = {
                R.color.filtershow_color_none,
                R.color.filtershow_color_punch,
                R.color.filtershow_color_vintage,
                R.color.filtershow_color_bw,
                R.color.filtershow_color_bleach,
                R.color.filtershow_color_instant,
                R.color.filtershow_color_latte,
                R.color.filtershow_color_blue,
                R.color.filtershow_color_litho,
                R.color.filtershow_color_xprocess
        };

        FilterFxRepresentation nullFx =
                new FilterFxRepresentation(context.getString(R.string.none),
                        0, R.string.none);
        nullFx.setColorId(colorId[0]);
        mLooks.add(nullFx);

        for (int i = 0; i < drawid.length; i++) {
            FilterFxRepresentation fx = new FilterFxRepresentation(
                    context.getString(fxNameid[i]), drawid[i], fxNameid[i]);
            fx.setSerializationName(serializationNames[i]);
            fx.setColorId(colorId[i] + 1);
            ImagePreset preset = new ImagePreset();
            preset.addFilter(fx);
            FilterUserPresetRepresentation rep = new FilterUserPresetRepresentation(
                    context.getString(fxNameid[i]), preset, -1);
            rep.setColorId(colorId[i] + 1);
            mLooks.add(rep);
            addRepresentation(fx);
        }
    }

    public void addEffects() {
        mEffects.add(getRepresentation(ImageFilterWBalance.class));
        mEffects.add(getRepresentation(ImageFilterExposure.class));
        mEffects.add(getRepresentation(ImageFilterContrast.class));
        mEffects.add(getRepresentation(ImageFilterVibrance.class));
        mEffects.add(getRepresentation(ImageFilterSharpen.class));
    }

    public void addMakeups(Context context) {
        if(SimpleMakeupImageFilter.HAS_TS_MAKEUP) {
            mMakeup.add(getRepresentation(ImageFilterMakeupWhiten.class));
            mMakeup.add(getRepresentation(ImageFilterMakeupSoften.class));
            mMakeup.add(getRepresentation(ImageFilterMakeupTrimface.class));
            mMakeup.add(getRepresentation(ImageFilterMakeupBigeye.class));
        }
    }

    public void addTrueScanner() {
        mTrueScanner.add(getRepresentation(TrueScannerActs.class));
    }

    public void addHazeBuster() {
        mHazeBuster.add(getRepresentation(HazeBusterActs.class));
    }

    public void addSeeStraight() {
        mSeeStraight.add(getRepresentation(SeeStraightActs.class));
    }

    public void addTools(Context context) {

        int[] textId = {
                R.string.crop,
                R.string.straighten,
                R.string.rotate
        };

        int[] overlayId = {
                R.drawable.crop_crop,
                R.drawable.crop_straighten,
                R.drawable.crop_rotate
        };

        FilterRepresentation[] geometryFilters = {
                new FilterCropRepresentation(),
                new FilterStraightenRepresentation(),
                new FilterRotateRepresentation()
        };

        for (int i = 0; i < textId.length; i++) {
            FilterRepresentation geometry = geometryFilters[i];
            geometry.setTextId(textId[i]);
            geometry.setOverlayId(overlayId[i]);
            geometry.setOverlayOnly(true);
            if (geometry.getTextId() != 0) {
                geometry.setName(context.getString(geometry.getTextId()));
            }
            mTools.add(geometry);
        }

        //mTools.add(getRepresentation(ImageFilterRedEye.class));
    }

    public void addWaterMarks(Context context) {
        int[] textId = {
                R.string.watermark_location,
                R.string.watermark_time,
                R.string.watermark_weather,
                R.string.watermark_emotions,
                R.string.watermark_food
        };

        int[] overlayId = {
                R.drawable.ic_watermark_location,
                R.drawable.ic_watermark_time,
                R.drawable.ic_watermark_weather,
                R.drawable.ic_watermark_emotion,
                R.drawable.ic_watermark_food
        };

        FilterWatermarkRepresentation[] waterMarkFilters = {
                new FilterWatermarkRepresentation(context, FilterWatermarkRepresentation.NAME_LOCATION,
                        FilterWatermarkRepresentation.LOCATION),
                new FilterWatermarkRepresentation(context, FilterWatermarkRepresentation.NAME_TIME,
                        FilterWatermarkRepresentation.TIME),
                new FilterWatermarkRepresentation(context, FilterWatermarkRepresentation.NAME_WEATHER,
                        FilterWatermarkRepresentation.WEATHER),
                new FilterWatermarkRepresentation(context, FilterWatermarkRepresentation.NAME_EMOTION,
                        FilterWatermarkRepresentation.EMOTIONS),
                new FilterWatermarkRepresentation(context, FilterWatermarkRepresentation.NAME_FOOD,
                        FilterWatermarkRepresentation.FOOD)
        };

        for (int i = 0; i < textId.length; i++) {
            FilterWatermarkRepresentation waterMark = waterMarkFilters[i];
            waterMark.setTextId(textId[i]);
            waterMark.setOverlayId(overlayId[i]);
            waterMark.setOverlayOnly(true);
            if (waterMark.getTextId() != 0) {
                waterMark.setName(context.getString(waterMark.getTextId()));
            }
            mWaterMarks.add(waterMark);
        }
    }
    public void addLocations(Context context) {
        int[] textId = {
                R.string.location_pin,
                R.string.location_city,
                R.string.location_hello,
                R.string.location_stamp
        };

        int[] overlayId = {
                R.drawable.icon_pin,
                R.drawable.icon_city,
                R.drawable.icon_hello,
                R.drawable.icon_stamp
        };

        FilterWatermarkRepresentation[] waterMarkFilters = {
                new FilterWatermarkRepresentation(context, "LOCATION_PIN", "SAN DIEGO"),
                new FilterWatermarkRepresentation(context, "LOCATION_CITY", "SAN DIEGO"),
                new FilterWatermarkRepresentation(context, "LOCATION_HELLO","SAN DIEGO"),
                new FilterWatermarkRepresentation(context, "LOCATION_STAMP","SAN DIEGO")
        };

        waterMarkFilters[0].new PositionInfo(0,
                context.getResources().getDimensionPixelSize(R.dimen.watermark_default_size),
                0,0, Gravity.CENTER_HORIZONTAL);
        waterMarkFilters[1].new PositionInfo(0,
                context.getResources().getDimensionPixelSize(R.dimen.watermark_default_size),
                0,0, Gravity.CENTER_HORIZONTAL);
        waterMarkFilters[2].new PositionInfo(0,60,0,0, Gravity.RIGHT, true);
        waterMarkFilters[3].new PositionInfo(0,0,0,0, Gravity.CENTER);

        for (int i = 0; i < waterMarkFilters.length; i++) {
            FilterWatermarkRepresentation waterMark = waterMarkFilters[i];
            waterMark.setTextId(textId[i]);
            waterMark.setOverlayId(overlayId[i],
                    new ContextThemeWrapper(context, R.style.DefaultFillColor).getTheme());
            waterMark.setWaterMarkId(overlayId[i]);
            waterMark.setMarkType(0);
            mLocations.add(waterMark);
        }
    }
    public void addTimes(Context context) {
        int[] textId = {
                R.string.time_hourglass,
                R.string.time_timestamp,
                R.string.time_sunrise,
                R.string.time_calendar
        };

        int[] overlayId = {
                R.drawable.icon_hourglass,
                R.drawable.icon_timestamp,
                R.drawable.icon_sunrise,
                R.drawable.icon_calendar
        };

        FilterWatermarkRepresentation[] waterMarkFilters = {
                new FilterWatermarkRepresentation(context, "TIME_HOURGLASS", null),
                new FilterWatermarkRepresentation(context, "TIME_TIMESTAMP", null),
                new FilterWatermarkRepresentation(context, "TIME_SUNRISE", null),
                new FilterWatermarkRepresentation(context, "TIME_CALENDAR", null)
        };

        FilterWatermarkRepresentation.PositionInfo[] positionInfos = {
                waterMarkFilters[0].new PositionInfo(
                        context.getResources().getDimensionPixelSize(R.dimen.watermark_default_size),
                        0,0,0, Gravity.CENTER_VERTICAL),
                waterMarkFilters[1].new PositionInfo(0,
                        context.getResources().getDimensionPixelSize(R.dimen.watermark_default_size),
                        0,0, Gravity.CENTER_HORIZONTAL),
                waterMarkFilters[2].new PositionInfo(0,
                        context.getResources().getDimensionPixelSize(R.dimen.watermark_default_size),
                        0,0, Gravity.RIGHT),
                waterMarkFilters[3].new PositionInfo(0,0,0,0, Gravity.CENTER)

        };

        for (int i = 0; i < waterMarkFilters.length; i++) {
            FilterWatermarkRepresentation waterMark = waterMarkFilters[i];
            waterMark.setTextId(textId[i]);
            waterMark.setOverlayId(overlayId[i],
                    new ContextThemeWrapper(context, R.style.DefaultFillColor).getTheme());
            waterMark.setWaterMarkId(overlayId[i]);
            waterMark.setMarkType(1);
            mTimes.add(waterMark);
        }
    }
    public void addWeather(Context context) {
        int[] textId = {
                R.string.weather_rain,
                R.string.weather_snow,
                R.string.weather_sun,
                R.string.weather_artistic_sun
        };

        int[] overlayId = {
                R.drawable.icon_rain,
                R.drawable.icon_snow,
                R.drawable.icon_sun,
                R.drawable.icon_artistic_sun
        };

        FilterWatermarkRepresentation[] waterMarkFilters = {
                new FilterWatermarkRepresentation(context, "WEATHER_RAIN", "50F"),
                new FilterWatermarkRepresentation(context, "WEATHER_SNOW", "30F"),
                new FilterWatermarkRepresentation(context, "WEATHER_SUN", "78F"),
                new FilterWatermarkRepresentation(context, "WEATHER_ARTISTIC_SUN", "78F")
        };

        waterMarkFilters[0].new PositionInfo(84, 40, 0, 0, true);
        waterMarkFilters[1].new PositionInfo(60, 50, 0, 0, true);
        waterMarkFilters[2].new PositionInfo(0, 66, 0, 0, Gravity.CENTER_HORIZONTAL, true);
        waterMarkFilters[3].new PositionInfo(0, 56, 0, 0, Gravity.CENTER_HORIZONTAL, true);

        for (int i = 0; i < waterMarkFilters.length; i++) {
            FilterWatermarkRepresentation waterMark = waterMarkFilters[i];
            waterMark.setTextId(textId[i]);
            waterMark.setOverlayId(overlayId[i],
                    new ContextThemeWrapper(context, R.style.DefaultFillColor).getTheme());
            waterMark.setWaterMarkId(overlayId[i]);
            waterMark.setMarkType(2);
            mWeathers.add(waterMark);
        }
    }
    public void addEmotions(Context context) {
        int[] textId = {
                R.string.emotion_party,
                R.string.emotion_peace,
                R.string.emotion_cry,
                R.string.emotion_happy
        };

        int[] overlayId = {
                R.drawable.icon_party,
                R.drawable.icon_peace,
                R.drawable.icon_cry,
                R.drawable.icon_happy
        };

        FilterWatermarkRepresentation[] waterMarkFilters = {
                new FilterWatermarkRepresentation(context, "EMOTION_PARTY", "Party!!"),
                new FilterWatermarkRepresentation(context, "EMOTION_PEACE", "PEACE"),
                new FilterWatermarkRepresentation(context, "EMOTION_CRY", null),
                new FilterWatermarkRepresentation(context, "EMOTION_HAPPY", null)
        };

        waterMarkFilters[0].new PositionInfo(0,
                context.getResources().getDimensionPixelSize(R.dimen.watermark_default_size),
                0, 0, Gravity.CENTER_HORIZONTAL);
        waterMarkFilters[1].new PositionInfo(0,
                context.getResources().getDimensionPixelSize(R.dimen.watermark_default_size),
                0, 0, Gravity.CENTER_HORIZONTAL);

        for (int i = 0; i < waterMarkFilters.length; i++) {
            FilterWatermarkRepresentation waterMark = waterMarkFilters[i];
            waterMark.setTextId(textId[i]);
            waterMark.setOverlayId(overlayId[i],
                    new ContextThemeWrapper(context, R.style.DefaultFillColor).getTheme());
            switch (i) {
                case 2:
                    waterMark.setWaterMarkId(R.drawable.icon_cry_color);
                    waterMark.setMarkAlpha(255);
                    break;
                case 3:
                    waterMark.setWaterMarkId(R.drawable.icon_happy_color);
                    waterMark.setMarkAlpha(255);
                    break;
                default:
                    waterMark.setWaterMarkId(overlayId[i]);
                    break;
            }
            waterMark.setMarkType(3);
            mEmotions.add(waterMark);
        }
    }
    public void addFoods(Context context) {
        int[] textId = {
                R.string.food_fork_knife,
                R.string.food_tea_time,
                R.string.food_cheers,
                R.string.food_yum
        };

        int[] overlayId = {
                R.drawable.icon_fork_and_knife,
                R.drawable.icon_tea_time,
                R.drawable.icon_cheers,
                R.drawable.icon_yum
        };

        FilterWatermarkRepresentation[] waterMarkFilters = {
                new FilterWatermarkRepresentation(context, "FOOD_FORK_KNIFE", "Breakfast"),
                new FilterWatermarkRepresentation(context, "FOOD_TEA_TIME", "Tea Time"),
                new FilterWatermarkRepresentation(context, "FOOD_CHEERS", "Cheers"),
                new FilterWatermarkRepresentation(context, "FOOD_YUM", null)
        };

        waterMarkFilters[0].new PositionInfo(0,32,0,0,Gravity.CENTER_HORIZONTAL,true);
        waterMarkFilters[1].new PositionInfo(0,
                context.getResources().getDimensionPixelSize(R.dimen.watermark_default_size),
                0, 0, Gravity.CENTER_HORIZONTAL);
        waterMarkFilters[2].new PositionInfo(0, 60, 0, 0, Gravity.CENTER_HORIZONTAL, true);

        for (int i = 0; i < waterMarkFilters.length; i++) {
            FilterWatermarkRepresentation waterMark = waterMarkFilters[i];
            waterMark.setTextId(textId[i]);
            waterMark.setOverlayId(overlayId[i],
                    new ContextThemeWrapper(context, R.style.DefaultFillColor).getTheme());
            waterMark.setWaterMarkId(overlayId[i]);
            waterMark.setMarkType(4);
            mFoods.add(waterMark);
        }
    }

    public void addDualCam(Context context) {
        int[] textId = {
                R.string.focus,
                R.string.halo,
                R.string.motion,
                R.string.posterize,
                R.string.sketch,
                R.string.zoom,
                R.string.bw,
                R.string.blackboard,
                R.string.whiteboard,
                R.string.dc_negative
        };

        int[] overlayId = {
                R.drawable.focus,
                R.drawable.halo,
                R.drawable.motion,
                R.drawable.posterize,
                R.drawable.sketch,
                R.drawable.zoom,
                R.drawable.bw,
                R.drawable.blackboard,
                R.drawable.whiteboard,
                R.drawable.negative
        };

        String[] serializationNames = {
                "DUAL_CAM_FOCUS",
                "DUAL_CAM_HALO",
                "DUAL_CAM_MOTION",
                "DUAL_CAM_POSTERIZE",
                "DUAL_CAM_SKETCH",
                "DUAL_CAM_ZOOM",
                "DUAL_CAM_BW",
                "DUAL_CAM_BLACKBOARD",
                "DUAL_CAM_WHITEBOARD",
                "DUAL_CAM_NEGATIVE"
        };

        // intensity range as defined by ddm lib
        int[][] ranges = {
                {0, 5, 10},
                {0, 5, 10},
                {0, 5, 10},
                {0, 5, 10}
        };

        FilterDualCamBasicRepresentation representation = new FilterDualCamBasicRepresentation(
                context.getString(R.string.none));
        representation.setTextId(R.string.none);
        representation.setEditorId(ImageOnlyEditor.ID);
        representation.setSerializationName("DUAL_CAM_NONE");
        mDualCam.add(representation);

        for (int i = 0; i < textId.length; i++) {
            if (i < ranges.length) {
                representation = new FilterDualCamBasicRepresentation(context.getString(textId[i]),
                        ranges[i][0], ranges[i][1], ranges[i][2]);
            } else {
                representation = new FilterDualCamBasicRepresentation(context.getString(textId[i]));
            }
            representation.setTextId(textId[i]);
            representation.setOverlayId(overlayId[i]);
            representation.setOverlayOnly(true);
            representation.setSerializationName(serializationNames[i]);
            mDualCam.add(representation);
            addRepresentation(representation);
        }

        mDualCam.add(getRepresentation(ImageFilterDualCamFusion.class));
    }

    public void addTruePortrait(Context context) {
        int[] textId = {
                R.string.none,
                R.string.blur,
                R.string.motion_blur,
                R.string.halo,
                R.string.sketch
        };

        int[] overlayId = {
                R.drawable.ic_tp_normal,
                R.drawable.ic_tp_bokeh,
                R.drawable.ic_tp_motion_blur,
                R.drawable.ic_tp_halo,
                R.drawable.ic_tp_sketch
        };

        String[] serializationNames = {
                "TRUE_PORTRAIT_NONE",
                "TRUE_PORTRAIT_BLUR",
                "TRUE_PORTRAIT_MOTION_BLUR",
                "TRUE_PORTRAIT_HALO",
                "TRUE_PORTRAIT_SKETCH"
        };

        int[][] minMaxValues = {
                {0,0,0},
                {0,3,7},
                {0,3,7},
                {0,3,7},
                {0,0,0}
        };

        boolean[] showParams = {
                false,
                true,
                true,
                true,
                false
        };

        int[] editorIDs = {
                ImageOnlyEditor.ID,
                EditorTruePortraitBasic.ID,
                EditorTruePortraitBasic.ID,
                EditorTruePortraitBasic.ID,
                EditorTruePortraitImageOnly.ID
        };

        for (int i = 0; i < textId.length; i++) {
            FilterRepresentation tPortrait =
                    new FilterBasicRepresentation(context.getString(textId[i]),
                            minMaxValues[i][0], minMaxValues[i][1], minMaxValues[i][2]);
            tPortrait.setFilterClass(ImageFilterTruePortrait.class);
            tPortrait.setFilterType(FilterRepresentation.TYPE_TRUEPORTRAIT);
            tPortrait.setTextId(textId[i]);
            tPortrait.setOverlayId(overlayId[i]);
            tPortrait.setOverlayOnly(true);
            tPortrait.setSerializationName(serializationNames[i]);
            tPortrait.setShowParameterValue(showParams[i]);
            tPortrait.setEditorId(editorIDs[i]);
            mTruePortrait.add(tPortrait);
            addRepresentation(tPortrait);
        }

        mTruePortrait.add(getRepresentation(ImageFilterTruePortraitFusion.class));
    }

    public void addFilterPreset (Context context) {
        FilterPresetSource fp = new FilterPresetSource(context);
        ArrayList<SaveOption> ret = fp.getAllUserPresets();
        if (ret == null) return;
        for (int id = 0; id<ret.size(); id++){
            FilterPresetRepresentation representation= new FilterPresetRepresentation (
                    ret.get(id).name,ret.get(id)._id,id+1);
            Uri filteredUri = Uri.parse(ret.get(id).Uri);
            representation.setUri(filteredUri);
            representation.setSerializationName("Custom");
            mFilterPreset.add(representation);
            ImagePreset preset = new ImagePreset();
            preset.addFilter(representation);
            addRepresentation(representation);
        }
    }

    public void removeRepresentation(ArrayList<FilterRepresentation> list,
            FilterRepresentation representation) {
        for (int i = 0; i < list.size(); i++) {
            FilterRepresentation r = list.get(i);
            if (r.getFilterClass() == representation.getFilterClass()) {
                list.remove(i);
                break;
            }
        }
    }

    public void setFilterResources(Resources resources) {
        ImageFilterBorder filterBorder = (ImageFilterBorder) getFilter(ImageFilterBorder.class);
        filterBorder.setResources(resources);
        ImageFilterFx filterFx = (ImageFilterFx) getFilter(ImageFilterFx.class);
        filterFx.setResources(resources);
        ImageFilterPreset filterPreset = (ImageFilterPreset) getFilter(ImageFilterPreset.class);
        filterPreset.setResources(resources);
    }
}

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

package com.android.gallery3d.filtershow.controller;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.colorpicker.ColorListener;
import com.android.gallery3d.filtershow.colorpicker.ColorOpacityView;
import com.android.gallery3d.filtershow.colorpicker.ColorSaturationView;
import com.android.gallery3d.filtershow.editors.Editor;

public class SliderSaturation implements Control {
    private ColorSaturationView mColorOpacityView;
    private ParameterSaturation mParameter;
    private Editor mEditor;

    @Override
    public void setUp(ViewGroup container, Parameter parameter, Editor editor) {
        container.removeAllViews();
        mEditor = editor;
        Context context = container.getContext();
        mParameter = (ParameterSaturation) parameter;
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout lp = (LinearLayout) inflater.inflate(
                R.layout.filtershow_saturation, container, true);

        mColorOpacityView = (ColorSaturationView) lp.findViewById(R.id.saturationView);
        updateUI();
        mColorOpacityView.addColorListener(new ColorListener() {
            @Override
            public void setColor(float[] hsvo) {
                mParameter.setValue((int) (255 * hsvo[3]));
                mEditor.commitLocalRepresentation();
            }
            @Override
            public void addColorListener(ColorListener l) {
            }
        });
    }

    @Override
    public View getTopView() {
        return mColorOpacityView;
    }

    @Override
    public void setPrameter(Parameter parameter) {
        mParameter = (ParameterSaturation) parameter;
        if (mColorOpacityView != null) {
            updateUI();
        }
    }

    @Override
    public void updateUI() {
        mColorOpacityView.setColor(mParameter.getColor());
    }
}

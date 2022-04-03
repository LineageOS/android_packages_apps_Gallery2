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

package com.android.gallery3d.filtershow.editors;

import java.util.ArrayList;
import java.util.Collection;

import android.content.Context;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.android.gallery3d.R;
import com.android.gallery3d.filtershow.controller.Control;
import com.android.gallery3d.filtershow.filters.FilterBasicRepresentation;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageShow;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ImagePreset;

/**
 * Base class for Editors Must contain a mImageShow and a top level view
 */
public class Editor implements OnSeekBarChangeListener, SwapButton.SwapButtonListener {
    protected Context mContext;
    protected View mView;
    protected ImageShow mImageShow;
    protected FrameLayout mFrameLayout;
    protected SeekBar mSeekBar;
    Button mEditTitle;
    protected Button mFilterTitle;
    protected int mID;
    private final String LOGTAG = "Editor";
    protected boolean mChangesGeometry = false;
    protected FilterRepresentation mLocalRepresentation = null;
    protected byte mShowParameter = SHOW_VALUE_UNDEFINED;
    private Button mButton;
    public static byte SHOW_VALUE_UNDEFINED = -1;
    public static byte SHOW_VALUE_OFF = 0;
    public static byte SHOW_VALUE_INT = 1;
    private View mActionButton, mEditControl;
    private TextView mBasicFilterText;
    private TextView mBasicFilterValue;

    public static void hackFixStrings(Menu menu) {
        int count = menu.size();
        for (int i = 0; i < count; i++) {
            MenuItem item = menu.getItem(i);
            item.setTitle(item.getTitle().toString().toUpperCase());
        }
    }

    public String calculateUserMessage(Context context, String effectName, Object parameterValue) {
        return effectName + " " + parameterValue;
    }

    protected Editor(int id) {
        mID = id;
    }

    public int getID() {
        return mID;
    }

    public byte showParameterValue() {
        return mShowParameter;
    }

    public boolean showsSeekBar() {
        return true;
    }

    public boolean showsActionBar() {
        // ActionBar is not need if has seek bar in default.
        // Subclasses should override if need.
        return !showsSeekBar();
    }

    public boolean showsActionBarControls() {
        return showsActionBar();
    }

    /*public void setUpEditorUI(View actionButton, View editControl,
                              Button editTitle, Button stateButton) {
        mEditTitle = editTitle;
        mFilterTitle = stateButton;
        mButton = editTitle;
        MasterImage.getImage().resetGeometryImages(false);
        setUtilityPanelUI(actionButton, editControl);
    }*/

    public void setUpEditorUI(View editControl, Button stateButton) {

        mFilterTitle = stateButton;
        MasterImage.getImage().resetGeometryImages(false);
        mEditControl = editControl;
        setEditPanelUI(editControl);
    }

    public void setEditorTitle(Button editTitle) {
        mEditTitle = editTitle;
        mButton = editTitle;
    }

    public void setBasicFilterUI(TextView textFilterName,
            TextView textFilterValue) {
        mBasicFilterText = textFilterName;
        mBasicFilterValue = textFilterValue;
    }

    public boolean showsPopupIndicator() {
        return false;
    }

    /**
     * @param editControl this is the black area for sliders etc
     */
    public void setEditPanelUI(View editControl) {
        Context context = editControl.getContext();
        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View lp = inflater.inflate(
                R.layout.filtershow_seekbar, (ViewGroup) editControl, true);
        mSeekBar = (SeekBar) lp.findViewById(R.id.primarySeekBar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mSeekBar.setVisibility(View.GONE);
        editControl.setVisibility(View.GONE);
        if (context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT) {
            if (showsSeekBar()) {
                mSeekBar.setVisibility(View.VISIBLE);
                editControl.setVisibility(View.VISIBLE);
            }
        }

        if (mButton != null) {
            setMenuIcon(showsPopupIndicator());
        }
    }

    @Override
    public void onProgressChanged(SeekBar sbar, int progress, boolean arg2) {

    }

    public void setPanel() {

    }

    public void createEditor(Context context, FrameLayout frameLayout) {
        mContext = context;
        mFrameLayout = frameLayout;
        mLocalRepresentation = null;
    }

    protected void unpack(int viewid, int layoutid) {

        if (mView == null) {
            mView = mFrameLayout.findViewById(viewid);
            if (mView == null) {
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                mView = inflater.inflate(layoutid, mFrameLayout, false);
                mFrameLayout.addView(mView, mView.getLayoutParams());
            }
        }
        mImageShow = findImageShow(mView);
    }

    private ImageShow findImageShow(View view) {
        if (view instanceof ImageShow) {
            return (ImageShow) view;
        }
        if (!(view instanceof ViewGroup)) {
            return null;
        }
        ViewGroup vg = (ViewGroup) view;
        int n = vg.getChildCount();
        for (int i = 0; i < n; i++) {
            View v = vg.getChildAt(i);
            if (v instanceof ImageShow) {
                return (ImageShow) v;
            } else if (v instanceof ViewGroup) {
                return findImageShow(v);
            }
        }
        return null;
    }

    public View getTopLevelView() {
        return mView;
    }

    public ImageShow getImageShow() {
        return mImageShow;
    }

    public void setVisibility(int visible) {
        mView.setVisibility(visible);
    }

    public FilterRepresentation getLocalRepresentation() {
        if (mLocalRepresentation == null) {
            ImagePreset preset = MasterImage.getImage().getPreset();
            FilterRepresentation filterRepresentation = MasterImage.getImage().getCurrentFilterRepresentation();
            mLocalRepresentation = preset.getFilterRepresentationCopyFrom(filterRepresentation);
            if (mShowParameter == SHOW_VALUE_UNDEFINED && filterRepresentation != null) {
                boolean show = filterRepresentation.showParameterValue();
                mShowParameter = show ? SHOW_VALUE_INT : SHOW_VALUE_OFF;
            }

        }
        return mLocalRepresentation;
    }

    /**
     * Call this to update the preset in MasterImage with the current representation
     * returned by getLocalRepresentation.  This causes the preview bitmap to be
     * regenerated.
     */
    public void commitLocalRepresentation() {
        commitLocalRepresentation(getLocalRepresentation());
    }

    /**
     * Call this to update the preset in MasterImage with a given representation.
     * This causes the preview bitmap to be regenerated.
     */
    public void commitLocalRepresentation(FilterRepresentation rep) {
        ArrayList<FilterRepresentation> filter = new ArrayList<FilterRepresentation>(1);
        filter.add(rep);
        commitLocalRepresentation(filter);
    }

    /**
     * Call this to update the preset in MasterImage with a collection of FilterRepresentations.
     * This causes the preview bitmap to be regenerated.
     */
    public void commitLocalRepresentation(Collection<FilterRepresentation> reps) {
        ImagePreset preset = MasterImage.getImage().getPreset();
        preset.updateFilterRepresentations(reps);
        if (mButton != null) {
            updateText();
        }
        if (mChangesGeometry) {
            // Regenerate both the filtered and the geometry-only bitmaps
            MasterImage.getImage().resetGeometryImages(true);
        }
        // Regenerate the filtered bitmap.
        MasterImage.getImage().invalidateFiltersOnly();
        preset.fillImageStateAdapter(MasterImage.getImage().getState());
    }

    /**
     * This is called in response to a click to apply and leave the editor.
     */
    public void finalApplyCalled() {
        commitLocalRepresentation();
    }

    protected void updateText() {
        String s = "";
        if (mLocalRepresentation != null) {
            s = mContext.getString(mLocalRepresentation.getTextId());
        }
        mButton.setText(calculateUserMessage(mContext, s, ""));
        if (mLocalRepresentation instanceof FilterBasicRepresentation
                && mBasicFilterText != null) {
            mBasicFilterText.setText(s);
            String text = calculateUserMessage(mContext, s, "");
            String[] split = text.split("[+-]");
            int length = split.length;
            if (length == 2)
                mBasicFilterValue.setText(split[1] != null ? split[1] : "");
            mBasicFilterText.setVisibility(View.VISIBLE);
            mBasicFilterValue.setVisibility(View.VISIBLE);
            mButton.setVisibility(View.INVISIBLE);

        }
    }

    /**
     * called after the filter is set and the select is called
     */
    public void reflectCurrentFilter() {
        mLocalRepresentation = null;
        FilterRepresentation representation = getLocalRepresentation();
        if (representation != null && mFilterTitle != null && representation.getTextId() != 0) {
            String text = mContext.getString(representation.getTextId()).toUpperCase();
            mFilterTitle.setText(text);
            updateText();
        }
    }

    public boolean useUtilityPanel() {
        return true;
    }

    public void openUtilityPanel(LinearLayout mAccessoryViewList) {
        setMenuIcon(showsPopupIndicator());
        if (mImageShow != null) {
            mImageShow.openUtilityPanel(mAccessoryViewList);
        }
    }

    protected void setMenuIcon(boolean on) {
        mEditTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(
                0, 0, on ? R.drawable.spinner_triangle : 0, 0);
    }

    protected void createMenu(int[] strId, View button) {
        PopupMenu pmenu = new PopupMenu(mContext, button);
        Menu menu = pmenu.getMenu();
        for (int i = 0; i < strId.length; i++) {
            menu.add(Menu.NONE, Menu.FIRST + i, 0, mContext.getString(strId[i]));
        }
        setMenuIcon(true);

    }

    public Control[] getControls() {
        return null;
    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar arg0) {

    }

    @Override
    public void swapLeft(MenuItem item) {

    }

    @Override
    public void swapRight(MenuItem item) {

    }

    public void attach() {
        if (mImageShow != null) {
            mImageShow.attach();
        }
    }

    public void detach() {
        if (mImageShow != null) {
            mImageShow.detach();
        }
    }

    public void resume() {

    }

    public void onPrepareOptionsMenu(Menu menu) {

    }
}

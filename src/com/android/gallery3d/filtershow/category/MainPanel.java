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

package com.android.gallery3d.filtershow.category;

import android.content.res.Configuration;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import org.codeaurora.gallery.R;
import org.json.JSONObject;

import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.editors.EditorPanel;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.filters.HazeBusterActs;
import com.android.gallery3d.filtershow.filters.SeeStraightActs;
import com.android.gallery3d.filtershow.filters.SimpleMakeupImageFilter;
import com.android.gallery3d.filtershow.filters.TrueScannerActs;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.state.StatePanel;
import com.android.gallery3d.filtershow.tools.DualCameraEffect;
import com.android.gallery3d.filtershow.tools.TruePortraitNativeEngine;
import com.android.gallery3d.filtershow.ui.DoNotShowAgainDialog;
import com.android.gallery3d.util.GalleryUtils;

import java.net.HttpURLConnection;
import java.net.URL;

public class MainPanel extends Fragment implements BottomPanel.BottomPanelDelegate {

    private static final String LOGTAG = "MainPanel";

    private LinearLayout mMainView;
    private View mBottomPanelView;
    private ImageButton looksButton;
    private ImageButton bordersButton;
    private ImageButton geometryButton;
    private ImageButton filtersButton;
    private ImageButton dualCamButton;
    private ImageButton makeupButton;
    private ImageButton trueScannerButton;
    private ImageButton hazeBusterButton;
    private ImageButton seeStraightButton;
    private ImageButton truePortraitButton;
    private ImageButton waterMarkButton;

    public static final String FRAGMENT_TAG = "MainPanel";
    public static final String EDITOR_TAG = "coming-from-editor-panel";
    public static final int LOOKS = 0;
    public static final int BORDERS = 1;
    public static final int GEOMETRY = 2;
    public static final int FILTERS = 3;
    public static final int MAKEUP = 4;
    public static final int DUALCAM = 5;
    public static final int VERSIONS = 6;
    public static final int TRUESCANNER = 7;
    public static final int HAZEBUSTER = 8;
    public static final int SEESTRAIGHT = 9;
    public static final int TRUEPORTRAIT = 10;
    public static final int WATERMARK = 11;

    private int mCurrentSelected = -1;
    private int mPreviousToggleVersions = -1;

    private void selection(int position, boolean value) {
        if (value) {
            FilterShowActivity activity = (FilterShowActivity) getActivity();
            activity.setCurrentPanel(position);
        }
        switch (position) {
            case LOOKS: {
                looksButton.setSelected(value);
                break;
            }
            case BORDERS: {
                bordersButton.setSelected(value);
                break;
            }
            case GEOMETRY: {
                geometryButton.setSelected(value);
                break;
            }
            case FILTERS: {
                filtersButton.setSelected(value);
                break;
            }
            case MAKEUP: {
                if(makeupButton != null) {
                    makeupButton.setSelected(value);
                }
                break;
            }
            case DUALCAM: {
                dualCamButton.setSelected(value);
                break;
            }
            case TRUEPORTRAIT: {
                truePortraitButton.setSelected(value);
                break;
            }
            case TRUESCANNER: {
                if (trueScannerButton != null) {
                    trueScannerButton.setSelected(value);
                }
                break;
            }
            case HAZEBUSTER: {
                if (hazeBusterButton != null) {
                    hazeBusterButton.setSelected(value);
                }
                break;
            }
            case SEESTRAIGHT: {
                if (seeStraightButton != null) {
                    seeStraightButton.setSelected(value);
                }
                break;
            }
            case WATERMARK: {
                waterMarkButton.setSelected(value);
                break;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mMainView != null) {
            if (mMainView.getParent() != null) {
                ViewGroup parent = (ViewGroup) mMainView.getParent();
                parent.removeView(mMainView);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mMainView = (LinearLayout) inflater.inflate(
                R.layout.filtershow_main_panel, null, false);

        getBottomPanelView(inflater);
        initBottomPanel();

        FilterShowActivity activity = (FilterShowActivity) getActivity();
        showPanel(activity.getCurrentPanel());
        return mMainView;
    }

    private void initBottomPanel() {
        BottomPanel panel = new BottomPanel();
        panel.setBottomPanelDelegate(this);
        setActionFragment(panel, BottomPanel.FRAGMENT_TAG);
    }

    @Override
    public View getBottomPanelView(LayoutInflater inflater) {
        if (mBottomPanelView != null) {
            return mBottomPanelView;
        }

        LinearLayout bottomPanel = (LinearLayout) inflater.inflate(
                R.layout.filtershow_bottom_panel, null, false);

        looksButton = (ImageButton) bottomPanel.findViewById(R.id.fxButton);
        bordersButton = (ImageButton) bottomPanel.findViewById(R.id.borderButton);
        geometryButton = (ImageButton) bottomPanel.findViewById(R.id.geometryButton);
        filtersButton = (ImageButton) bottomPanel.findViewById(R.id.colorsButton);
        dualCamButton = (ImageButton) bottomPanel.findViewById(R.id.dualCamButton);

        if (SimpleMakeupImageFilter.HAS_TS_MAKEUP) {
            makeupButton = (ImageButton) bottomPanel.findViewById(R.id.makeupButton);
            makeupButton.setVisibility(View.VISIBLE);
        }

        if (makeupButton != null) {
            makeupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPanel(MAKEUP);
                }
            });
        }
        trueScannerButton = (ImageButton) bottomPanel.findViewById(R.id.trueScannerButton);
        if (!TrueScannerActs.isTrueScannerEnabled()) {
            trueScannerButton.setVisibility(View.GONE);
        }

        hazeBusterButton = (ImageButton) bottomPanel.findViewById(R.id.hazeBusterButton);
        if (!HazeBusterActs.isHazeBusterEnabled()) {
            hazeBusterButton.setVisibility(View.GONE);
        }

        seeStraightButton = (ImageButton) bottomPanel.findViewById(R.id.seeStraightButton);
        if (!SeeStraightActs.isSeeStraightEnabled()) {
            seeStraightButton.setVisibility(View.GONE);
        }

        truePortraitButton = (ImageButton) bottomPanel.findViewById(R.id.truePortraitButton);
        if(!TruePortraitNativeEngine.getInstance().isLibLoaded()) {
            truePortraitButton.setVisibility(View.GONE);
        }

        waterMarkButton = (ImageButton) bottomPanel.findViewById(R.id.waterMarkButton);

        waterMarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPanel(WATERMARK);
            }
        });
        looksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(LOOKS);
            }
        });
        bordersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(BORDERS);
            }
        });
        geometryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(GEOMETRY);
            }
        });
        filtersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(FILTERS);
            }
        });
        dualCamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getActivity();
                boolean skipIntro = GalleryUtils.getBooleanPref(context,
                        context.getString(R.string.pref_dualcam_intro_show_key), false);
                if (skipIntro) {
                    showPanel(DUALCAM);
                } else {
                    DoNotShowAgainDialog dialog = new DoNotShowAgainDialog(
                            R.string.dual_camera_effects, R.string.dual_camera_effects_intro,
                            R.string.pref_dualcam_intro_show_key);
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            showPanel(DUALCAM);
                        }
                    });
                    dialog.show(getFragmentManager(), "dualcam_intro");
                }
            }
        });

        truePortraitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Context context = getActivity();
                boolean skipIntro = GalleryUtils.getBooleanPref(context,
                        context.getString(R.string.pref_trueportrait_intro_show_key), false);
                final boolean facesDetected = TruePortraitNativeEngine.getInstance().facesDetected();
                if(skipIntro && facesDetected) {
                    showPanel(TRUEPORTRAIT);
                } else if(!skipIntro) {
                    DoNotShowAgainDialog dialog = new DoNotShowAgainDialog(
                            R.string.trueportrait, R.string.trueportrait_intro,
                            R.string.pref_trueportrait_intro_show_key);
                    dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            if(facesDetected) {
                                showPanel(TRUEPORTRAIT);
                            } else {
                                v.setEnabled(false);
                                TruePortraitNativeEngine.getInstance().showNoFaceDetectedDialog(getFragmentManager());
                            }
                        }
                    });
                    dialog.show(getFragmentManager(), "trueportrait_intro");
                } else {
                    v.setEnabled(false);
                    TruePortraitNativeEngine.getInstance().showNoFaceDetectedDialog(getFragmentManager());
                }
            }
        });

        trueScannerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showPanel(TRUESCANNER);
            }
        });

        hazeBusterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(HAZEBUSTER);
            }
        });

        seeStraightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPanel(SEESTRAIGHT);
            }
        });

        mBottomPanelView = bottomPanel;
        return bottomPanel;
    }

    private boolean isRightAnimation(int newPos) {
        if (newPos < mCurrentSelected) {
            return false;
        }
        return true;
    }

    private void setActionFragment(Fragment actionFragment, String tag) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.bottom_panel_container, actionFragment, tag);
        transaction.commitAllowingStateLoss();
    }

    public boolean hasEditorPanel() {
        Fragment fragment = getChildFragmentManager().findFragmentByTag(EditorPanel.FRAGMENT_TAG);
        return (fragment != null);
    }

    public void setEditorPanelFragment(Fragment editorFragment) {
        setActionFragment(editorFragment, EditorPanel.FRAGMENT_TAG);
    }

    public void removeEditorPanelFragment() {
        // use bottom panel replace editor panel.
        initBottomPanel();
    }

    private void setCategoryFragment(Fragment category, boolean fromRight) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        if (fromRight) {
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right);
        } else {
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left);
        }
        activity.setActionBar();
        transaction.replace(R.id.category_panel_container, category, CategoryPanel.FRAGMENT_TAG);
        transaction.commitAllowingStateLoss();
    }

    public void loadCategoryLookPanel(boolean force) {
        if (!force && mCurrentSelected == LOOKS) {
            return;
        }
        boolean fromRight = isRightAnimation(LOOKS);
        selection(mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(LOOKS);
        setCategoryFragment(categoryPanel, fromRight);
        mCurrentSelected = LOOKS;
        selection(mCurrentSelected, true);
    }

    public void loadCategoryBorderPanel() {
        if (mCurrentSelected == BORDERS) {
            return;
        }
        boolean fromRight = isRightAnimation(BORDERS);
        selection(mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(BORDERS);
        setCategoryFragment(categoryPanel, fromRight);
        mCurrentSelected = BORDERS;
        selection(mCurrentSelected, true);
    }

    public void loadCategoryMakeupPanel() {
        if (makeupButton == null) {
            return;
        }
        boolean fromRight = isRightAnimation(MAKEUP);
        selection(mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(MAKEUP);
        setCategoryFragment(categoryPanel, fromRight);
        mCurrentSelected = MAKEUP;
        selection(mCurrentSelected, true);
    }

    public void loadCategoryGeometryPanel() {
        if (mCurrentSelected == GEOMETRY) {
            return;
        }
        if (MasterImage.getImage().hasTinyPlanet()) {
            return;
        }
        boolean fromRight = isRightAnimation(GEOMETRY);
        selection(mCurrentSelected, false);
        GeometryPanel categoryPanel = new GeometryPanel();
        setCategoryFragment(categoryPanel, fromRight);
        mCurrentSelected = GEOMETRY;
        selection(mCurrentSelected, true);
    }

    public void loadCategoryTrueScannerPanel() {
        final FilterShowActivity activity = (FilterShowActivity) getActivity();
        CategoryAdapter adapter = activity.getCategoryTrueScannerAdapter();
        if (adapter.getCount() == 1) {
            activity.showRepresentation(adapter.getItem(0).getRepresentation());
        } else {
            boolean fromRight = isRightAnimation(TRUESCANNER);
            selection(mCurrentSelected, false);
            CategoryPanel categoryPanel = new CategoryPanel();
            categoryPanel.setAdapter(TRUESCANNER);
            setCategoryFragment(categoryPanel, fromRight);
            mCurrentSelected = TRUESCANNER;
            selection(mCurrentSelected, true);
        }
    }

    public void loadCategoryHazeBusterPanel() {
        final FilterShowActivity activity = (FilterShowActivity) getActivity();
        CategoryAdapter adapter = activity.getCategoryHazeBusterAdapter();
        if (adapter.getCount() == 1) {
            activity.showRepresentation(adapter.getItem(0).getRepresentation());
        } else {
            boolean fromRight = isRightAnimation(HAZEBUSTER);
            selection(mCurrentSelected, false);
            CategoryPanel categoryPanel = new CategoryPanel();
            categoryPanel.setAdapter(HAZEBUSTER);
            setCategoryFragment(categoryPanel, fromRight);
            mCurrentSelected = HAZEBUSTER;
            selection(mCurrentSelected, true);
        }
    }

    public void loadCategorySeeStraightPanel() {
        final FilterShowActivity activity = (FilterShowActivity) getActivity();
        CategoryAdapter adapter = activity.getCategorySeeStraightAdapter();
        if (adapter.getCount() == 1) {
            activity.showRepresentation(adapter.getItem(0).getRepresentation());
        } else {
            boolean fromRight = isRightAnimation(SEESTRAIGHT);
            selection(mCurrentSelected, false);
            CategoryPanel categoryPanel = new CategoryPanel();
            categoryPanel.setAdapter(SEESTRAIGHT);
            setCategoryFragment(categoryPanel, fromRight);
            mCurrentSelected = SEESTRAIGHT;
            selection(mCurrentSelected, true);
        }
    }

    public void loadCategoryFiltersPanel() {
        if (mCurrentSelected == FILTERS) {
            return;
        }
        boolean fromRight = isRightAnimation(FILTERS);
        selection(mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(FILTERS);
        setCategoryFragment(categoryPanel, fromRight);
        mCurrentSelected = FILTERS;
        selection(mCurrentSelected, true);
    }

    public void loadCategoryVersionsPanel() {
        if (mCurrentSelected == VERSIONS) {
            return;
        }
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        activity.updateVersions();
        boolean fromRight = isRightAnimation(VERSIONS);
        selection(mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(VERSIONS);
        setCategoryFragment(categoryPanel, fromRight);
        mCurrentSelected = VERSIONS;
        selection(mCurrentSelected, true);
    }

    public void loadCategoryDualCamPanel() {
        if (mCurrentSelected == DUALCAM) {
            return;
        }
        boolean fromRight = isRightAnimation(DUALCAM);
        selection(mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(DUALCAM);
        setCategoryFragment(categoryPanel, fromRight);
        mCurrentSelected = DUALCAM;
        selection(mCurrentSelected, true);
    }

    public void loadCategoryTruePortraitPanel() {
        if (mCurrentSelected == TRUEPORTRAIT) {
            return;
        }
        boolean fromRight = isRightAnimation(TRUEPORTRAIT);
        selection(mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(TRUEPORTRAIT);
        setCategoryFragment(categoryPanel, fromRight);
        mCurrentSelected = TRUEPORTRAIT;
        selection(mCurrentSelected, true);
    }

    public void loadWaterMarkPanel() {
        if (mCurrentSelected == WATERMARK) {
            return;
        }
        boolean fromRight = isRightAnimation(WATERMARK);
        selection(mCurrentSelected, false);
        CategoryPanel categoryPanel = new CategoryPanel();
        categoryPanel.setAdapter(WATERMARK);
        setCategoryFragment(categoryPanel, fromRight);
        mCurrentSelected = WATERMARK;
        selection(mCurrentSelected, true);
        final FilterShowActivity activity = (FilterShowActivity) getActivity();
//        String url = "http://api.map.baidu.com/location/ip?ak=PR0TaSQODfSbVr7hkcNF4NkAYEixoxSy&coor=bd09ll";
        String url = "";
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... strings) {
                HttpURLConnection connection = null;
                String location = "";
                try {
                    URL url = new URL(strings[0]);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Charset", "UTF-8");
                    connection.connect();
                    if (connection.getResponseCode() == 200) {
                        String result = WaterMarkView.convertStream2String(connection.getInputStream());
                        JSONObject resultJson = new JSONObject(result);
                        location = resultJson.getJSONObject("content").getString("address");
                        if (!TextUtils.isEmpty(location)) {
                            getWeather(activity, location);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                return location;
            }

            @Override
            protected void onPostExecute(String s) {
                activity.setLocation(s);
            }
        }.execute(url);
    }

    private void getWeather(final FilterShowActivity activity, String city) {
//        String url = "http://api.map.baidu.com/telematics/v3/weather?location="
//                + city + "&output=json&ak=PR0TaSQODfSbVr7hkcNF4NkAYEixoxSy";
        String url = "";
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... strings) {
                HttpURLConnection connection = null;
                String temperature = "";
                try {
                    URL url = new URL(strings[0]);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Charset", "UTF-8");
                    connection.connect();
                    if (connection.getResponseCode() == 200) {
                        String result = WaterMarkView.convertStream2String(connection.getInputStream());
                        JSONObject resultJson = new JSONObject(result);
                        String weather = resultJson.getJSONArray("results").getJSONObject(0).getJSONArray("weather_data").getJSONObject(0).getString("date");
                        temperature = weather.substring(weather.indexOf(":")+1, weather.indexOf(")"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                return temperature;
            }

            @Override
            protected void onPostExecute(String s) {
                activity.setTemperature(s);
            }
        }.execute(url);
    }

    public void showPanel(int currentPanel) {
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        if (null == activity) {
            return;
        }
        switch (currentPanel) {
            case LOOKS: {
                loadCategoryLookPanel(false);
                break;
            }
            case BORDERS: {
                loadCategoryBorderPanel();
                break;
            }
            case GEOMETRY: {
                loadCategoryGeometryPanel();
                break;
            }
            case FILTERS: {
                loadCategoryFiltersPanel();
                break;
            }
            case DUALCAM: {
                loadCategoryDualCamPanel();
                break;
            }
            case TRUESCANNER: {
                loadCategoryTrueScannerPanel();
                break;
            }
            case HAZEBUSTER: {
                loadCategoryHazeBusterPanel();
                break;
            }
            case SEESTRAIGHT: {
                loadCategorySeeStraightPanel();
                break;
            }
            case VERSIONS: {
                loadCategoryVersionsPanel();
                break;
            }
            case MAKEUP: {
                loadCategoryMakeupPanel();
                break;
            }
            case TRUEPORTRAIT: {
                loadCategoryTruePortraitPanel();
                break;
            }
            case WATERMARK: {
                loadWaterMarkPanel();
                break;
            }
        }
        if (currentPanel > 0) {
            activity.adjustCompareButton(true);
        } else {
            activity.adjustCompareButton(false);
        }
    }

    public void setToggleVersionsPanelButton(ImageButton button) {
        if (button == null) {
            return;
        }
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentSelected == VERSIONS) {
                    showPanel(mPreviousToggleVersions);
                } else {
                    mPreviousToggleVersions = mCurrentSelected;
                    showPanel(VERSIONS);
                }
            }
        });
    }

    public void showImageStatePanel(boolean show) {
        View container = mMainView.findViewById(R.id.state_panel_container);
        FragmentTransaction transaction = null;
        if (container == null) {
            FilterShowActivity activity = (FilterShowActivity) getActivity();
            container = activity.getMainStatePanelContainer(R.id.state_panel_container);
        } else {
            transaction = getChildFragmentManager().beginTransaction();
        }
        if (container == null) {
            return;
        } else {
            transaction = getFragmentManager().beginTransaction();
        }
        int currentPanel = mCurrentSelected;
        if (show) {
            container.setVisibility(View.VISIBLE);
            StatePanel statePanel = new StatePanel();
            statePanel.setMainPanel(this);
            FilterShowActivity activity = (FilterShowActivity) getActivity();
            activity.updateVersions();
            transaction.replace(R.id.state_panel_container, statePanel, StatePanel.FRAGMENT_TAG);
        } else {
            container.setVisibility(View.GONE);
            Fragment statePanel = getChildFragmentManager().findFragmentByTag(StatePanel.FRAGMENT_TAG);
            if (statePanel != null) {
                transaction.remove(statePanel);
            }
            if (currentPanel == VERSIONS) {
                currentPanel = LOOKS;
            }
        }
        mCurrentSelected = -1;
        showPanel(currentPanel);
        transaction.commit();
    }

    public void showDualCameraButton(boolean visible) {
        if (dualCamButton != null) {
            dualCamButton.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}

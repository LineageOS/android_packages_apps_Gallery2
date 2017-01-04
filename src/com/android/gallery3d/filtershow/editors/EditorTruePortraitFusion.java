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

package com.android.gallery3d.filtershow.editors;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import org.codeaurora.gallery.R;

import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.filters.FilterRepresentation;
import com.android.gallery3d.filtershow.filters.FilterTruePortraitFusionRepresentation;
import com.android.gallery3d.filtershow.imageshow.ImageTruePortraitFusion;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.ui.DoNotShowAgainDialog;
import com.android.gallery3d.util.GalleryUtils;

public class EditorTruePortraitFusion extends Editor {
    public static final String TAG = "EditorTruePortraitFusion";
    public static final int ID = R.id.editorTruePortraitFusion;

    protected ImageTruePortraitFusion mImageFusion;
    private Uri mUnderlayUri = Uri.EMPTY;

    public EditorTruePortraitFusion() {
        super(ID);
    }

    @Override
    public boolean showsActionBar() {
        return true;
    }

    @Override
    public boolean showsActionBarControls() {
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        if (mImageFusion == null) {
            mImageFusion = new ImageTruePortraitFusion(context);
        }
        mView = mImageShow = mImageFusion;
        mImageFusion.setEditor(this);
    }

    @Override
    public void setEditPanelUI(View editControl) {
        ViewGroup controlContainer = (ViewGroup)editControl;
        controlContainer.removeAllViews();

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View controls = inflater.inflate(R.layout.filtershow_seekbar, controlContainer);
        View seekbar = controls.findViewById(R.id.primarySeekBar);
        seekbar.setVisibility(View.GONE);
        View saveButton = controls.findViewById(R.id.slider_save);
        if (saveButton != null) {
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FilterShowActivity activity = (FilterShowActivity) mContext;
                    finalApplyCalled();
                    activity.leaveSeekBarPanel();
                }
            });
        }
        View cancelButton = controls.findViewById(R.id.slider_cancel);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FilterShowActivity activity = (FilterShowActivity) mContext;
                    activity.cancelCurrentFilter();
                    activity.leaveSeekBarPanel();
                }
            });
        }
    }

    @Override
    public void openUtilityPanel(final LinearLayout accessoryViewList) {
        accessoryViewList.removeAllViews();

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.filtershow_actionbar_trueportrait_fusion, accessoryViewList);

        View pickUnderlayBtn = accessoryViewList.findViewById(R.id.pick_underlay);
        pickUnderlayBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                MasterImage.getImage().getActivity().pickImage(FilterShowActivity.SELECT_FUSION_UNDERLAY);
            }
        });

        View editMaskBtn = accessoryViewList.findViewById(R.id.editMask);
        editMaskBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterRepresentation representation = new FilterRepresentation("");
                representation.setEditorId(EditorTruePortraitMask.ID);
                ((FilterShowActivity)mContext).loadEditorPanel(representation);
            }
        });

        // Look for previous underlay
        String fusionUnderlay = GalleryUtils.getStringPref(mContext,
                mContext.getString(R.string.pref_trueportrait_fusion_underlay_key), null);
        Uri fusionUri = Uri.EMPTY;

        if(fusionUnderlay != null) {
            fusionUri = Uri.parse(fusionUnderlay);
            if(!uriExists(mContext, fusionUri))
                fusionUri = Uri.EMPTY;
        }

        setUnderlayImageUri(fusionUri);
    }

    @Override
    public void resume() {
        if(mUnderlayUri.equals(Uri.EMPTY)) {
            // No underlay set.
            boolean skipIntro = GalleryUtils.getBooleanPref(mContext,
                    mContext.getString(R.string.pref_trueportrait_fusion_intro_show_key), false);
            if(!skipIntro) {
                FragmentManager fm = ((FilterShowActivity)mContext).getSupportFragmentManager();
                DoNotShowAgainDialog dialog =
                        (DoNotShowAgainDialog) fm.findFragmentByTag("trueportrait_fusion_intro");
                if(dialog == null) {
                    dialog = new DoNotShowAgainDialog(
                            R.string.fusion_pick_background, R.string.trueportrait_fusion_intro,
                            R.string.pref_trueportrait_fusion_intro_show_key);
                    dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            FilterShowActivity activity = (FilterShowActivity) mContext;
                            activity.cancelCurrentFilter();
                            activity.leaveSeekBarPanel();
                        }
                    });
                    dialog.setOnOkButtonClickListener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            MasterImage.getImage().getActivity().pickImage(FilterShowActivity.SELECT_FUSION_UNDERLAY);
                        }
                    });
                    dialog.setCancelable(true);
                    dialog.show(fm, "trueportrait_fusion_intro");
                } else if (dialog.isDetached()) {
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.attach(dialog);
                    ft.commit();
                } else if (dialog.isHidden()) {
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.show(dialog);
                    ft.commit();
                }
            }
        }
    }

    public void setUnderlayImageUri(Uri uri) {
        mUnderlayUri = uri;
        FilterRepresentation filter = getLocalRepresentation();
        if(filter instanceof FilterTruePortraitFusionRepresentation) {
            mImageFusion.setUnderlay(uri);
            commitLocalRepresentation();

            // save fusion underlay uri
            GalleryUtils.setStringPref(mContext,
                    mContext.getString(R.string.pref_trueportrait_fusion_underlay_key),
                    (uri != null)?uri.toString():null);
        }
    }

    @Override
    public void reflectCurrentFilter() {
        super.reflectCurrentFilter();
        FilterRepresentation rep = getLocalRepresentation();
        if (rep != null && rep instanceof FilterTruePortraitFusionRepresentation) {
            FilterTruePortraitFusionRepresentation fusionRep = (FilterTruePortraitFusionRepresentation) rep;
            mImageFusion.setRepresentation(fusionRep);
        }
    }

    private boolean uriExists(Context context, Uri uri) {
        try {
            InputStream stream = context.getContentResolver().openInputStream(uri);
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}

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


import android.content.Context;
import androidx.fragment.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.codeaurora.gallery.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.filtershow.imageshow.ImageTruePortraitMask;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.ui.AlertMsgDialog;

public class EditorTruePortraitMask extends Editor  {
    private static final String LOGTAG = "EditorTruePortraitMask";
    public static final int ID = R.id.editorTruePortraitMask;

    private AlertMsgDialog mHelpDialog;
    private ImageTruePortraitMask mTruePortraitImage;
    private ToggleButton mForeground;
    private ToggleButton mBackground;
    private ImageButton mExitButton;
    private ImageButton mApplyButton;
    private View mMaskUndoButton;
    private View mBrushSizeButton;
    private View[] mBrushSizes = new View[3];
    private int mBrushIndex = 0;

    private static final int[] BRUSH_SIZES = {
        10, 30, 50
    };

    private OnClickListener mDoneClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            if(view.getId() == R.id.done) {
                finalApplyCalled();
            }
            FilterShowActivity activity = (FilterShowActivity) mContext;
            activity.backToMain();
            activity.setActionBar();
        }
    };

    public EditorTruePortraitMask() {
        super(ID);
    }

    @Override
    public void createEditor(Context context, FrameLayout frameLayout) {
        super.createEditor(context, frameLayout);
        unpack(R.id.truePortraitMaskEditor, R.layout.filtershow_trueportrait_mask_editor);
        mTruePortraitImage = (ImageTruePortraitMask)mImageShow;
        mTruePortraitImage.setEditor(this);
    }

    @Override
    public boolean showsSeekBar() {
        return false;
    }

    @Override
    public boolean showsActionBarControls() {
        return false;
    }

    @Override
    public void openUtilityPanel(LinearLayout accessoryViewList) {
        accessoryViewList.removeAllViews();

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.filtershow_actionbar_trueportrait_mask, accessoryViewList);

        mMaskUndoButton = accessoryViewList.findViewById(R.id.maskUndo);
        mMaskUndoButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mTruePortraitImage.undoLastEdit();
            }
        });
        refreshUndoButton();

        View maskHelp = accessoryViewList.findViewById(R.id.maskHelp);
        maskHelp.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mHelpDialog == null)
                    mHelpDialog = new AlertMsgDialog(R.string.help, R.string.trueportrait_edit_help);

                FragmentManager fm = ((FilterShowActivity)mContext).getSupportFragmentManager();
                mHelpDialog.show(fm, "tp_edit_help");
            }
        });

        mForeground = (ToggleButton)accessoryViewList.findViewById(R.id.maskForeground);
        mForeground.setChecked(true);
        mForeground.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mBackground.isChecked() == isChecked)
                    mBackground.toggle();
                mTruePortraitImage.invalidate();
                showToast();
            }
        });

        mBackground = (ToggleButton)accessoryViewList.findViewById(R.id.maskBackground);
        mBackground.setChecked(false);
        mBackground.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(mForeground.isChecked() == isChecked)
                    mForeground.toggle();
            }
        });
    }

    @Override
    public void setUpEditorUI(View editControl, Button stateButton) {
        mExitButton = (ImageButton) editControl.findViewById(R.id.cancel);
        mApplyButton = (ImageButton) editControl.findViewById(R.id.done);
        mExitButton.setOnClickListener(mDoneClickListener);
        mApplyButton.setOnClickListener(mDoneClickListener);

        mBrushSizeButton = editControl.findViewById(R.id.brush_size);
        mBrushSizeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                cycleBrushSize();
            }
        });

        mBrushSizes[0] = editControl.findViewById(R.id.brush_size_sm);
        mBrushSizes[0].setSelected(false);
        mBrushSizes[1] = editControl.findViewById(R.id.brush_size_med);
        mBrushSizes[1].setSelected(false);
        mBrushSizes[2] = editControl.findViewById(R.id.brush_size_large);
        mBrushSizes[2].setSelected(false);

        mBrushSizes[mBrushIndex].setSelected(true);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
    }

    @Override
    public void finalApplyCalled() {
        mTruePortraitImage.applyMaskUpdates();
        MasterImage.getImage().invalidateFiltersOnly();
    }

    private void showToast() {
        Toast toast;
        if(isBackgroundMode()) {
            toast = Toast.makeText(mContext, R.string.trueportrait_edit_background_toast, Toast.LENGTH_LONG);
        } else {
            toast = Toast.makeText(mContext, R.string.trueportrait_edit_foreground_toast, Toast.LENGTH_LONG);
        }

        toast.show();
    }

    public void refreshUndoButton() {
        mMaskUndoButton.setEnabled(mTruePortraitImage.canUndoEdit());
    }

    public boolean isBackgroundMode() {
        return mBackground.isChecked();
    }

    public int cycleBrushSize() {
        mBrushSizes[mBrushIndex].setSelected(false);
        mBrushIndex = (mBrushIndex+1)%BRUSH_SIZES.length;
        mBrushSizes[mBrushIndex].setSelected(true);
        return getBrushSize();
    }

    public int getBrushSize() {
        return BRUSH_SIZES[mBrushIndex];
    }
}

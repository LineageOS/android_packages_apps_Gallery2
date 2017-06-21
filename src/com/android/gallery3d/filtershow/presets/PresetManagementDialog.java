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

package com.android.gallery3d.filtershow.presets;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import org.codeaurora.gallery.R;
import com.android.gallery3d.filtershow.FilterShowActivity;
import com.android.gallery3d.ui.BaseDialogFragment;
import com.android.gallery3d.util.GalleryUtils;

//import static com.android.gallery3d.filtershow.FilterShowActivity.SELECT_FILTER;

public class PresetManagementDialog extends BaseDialogFragment implements View.OnClickListener {
    private CheckBox mCheckBox;
    private boolean checked;
    private boolean mDismissInternel = false;
    private DialogInterface.OnDismissListener mDialogDismissListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.filtershow_presets_dialog, container);
        mCheckBox = (CheckBox) view.findViewById(R.id.filtershow_check_box);
        view.findViewById(R.id.cancel).setOnClickListener(this);
        view.findViewById(R.id.ok).setOnClickListener(this);
        view.findViewById(R.id.filtershow_check_box).setOnClickListener(this);
        return view;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        mDialogDismissListener = listener;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if ((mDialogDismissListener != null) && mDismissInternel) {
            mDialogDismissListener.onDismiss(dialog);
        }
    }

    public void dismissInternal() {
        mDismissInternel = true;
        dismiss();
    }

    @Override
    public void onClick(View v) {
        FilterShowActivity activity = (FilterShowActivity) getActivity();
        switch (v.getId()) {
            case R.id.cancel:
                dismissInternal();
                break;
            case R.id.ok:
                checked = mCheckBox.isChecked();
                GalleryUtils.setBooleanPref(activity,activity.getString(R.string.pref_filtergenerator_intro_show_key),checked);
                activity.onMediaPickerStarted ();
                dismissInternal();
                break;
        }
    }
}

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

package com.android.gallery3d.filtershow.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.util.Log;

import com.android.gallery3d.filtershow.data.FilterPresetDBHelper.FilterPreset;

import java.util.ArrayList;

public class FilterPresetSource {
    private static final String LOGTAG = "FilterStackSource";

    private SQLiteDatabase database = null;
    private final FilterPresetDBHelper dbHelper;

    public FilterPresetSource(Context context) {
        dbHelper = new FilterPresetDBHelper(context);
        open();
    }

    public void open() {
        try {
            database = dbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            Log.w(LOGTAG, "could not open database", e);
        }
    }

    public void close() {
        database = null;
        dbHelper.close();
    }

    public static class SaveOption {
        public int _id;
        public String name;
        public String Uri;
    }

    public boolean insertPreset(String presetName, String presetUri) {
        boolean ret = true;
        ContentValues val = new ContentValues();
        val.put(FilterPreset.PRESET_ID, presetName);
        val.put(FilterPreset.FILTER_PRESET, presetUri);
        database.beginTransaction();
        try {
            ret = (-1 != database.insert(FilterPreset.TABLE, null, val));
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return ret;
    }

    public void updatePresetName(int id, String presetName) {
        ContentValues val = new ContentValues();
        val.put(FilterPreset.PRESET_ID, presetName);
        database.beginTransaction();
        try {
            database.update(FilterPreset.TABLE, val,FilterPreset._ID + " = ?",
                    new String[] { "" + id});
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    public boolean removePreset(int id) {
        boolean ret = true;
        database.beginTransaction();
        try {
            ret = (0 != database.delete(FilterPreset.TABLE, FilterPreset._ID + " = ?",
                    new String[] { "" + id }));
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        return ret;
    }

    public ArrayList<SaveOption> getAllUserPresets() {
        ArrayList<SaveOption> ret = new ArrayList<SaveOption>();
        Cursor c = null;
        database.beginTransaction();
        try {
            c = database.query(FilterPreset.TABLE,
                    new String[] { FilterPreset._ID,
                            FilterPreset.PRESET_ID,
                            FilterPreset.FILTER_PRESET },
                    null, null, null, null, null, null);
            if (c != null) {
                boolean loopCheck = c.moveToFirst();
                while (loopCheck) {
                    String id = (c.isNull(0)) ?  null : c.getString(0);
                    String name = (c.isNull(1)) ?  null : c.getString(1);
                    String filterUri = (c.isNull(2)) ? null : c.getString(2);
                    SaveOption so = new SaveOption();
                    try {
                        so._id = Integer.parseInt(id);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    so.name = name;
                    so.Uri = filterUri;
                    ret.add(so);
                    loopCheck = c.moveToNext();
                }
            }
            database.setTransactionSuccessful();
        } finally {
            if (c != null) {
                c.close();
            }
            database.endTransaction();
        }
        return ret;
    }
}

package org.codeaurora.gallery3d.video;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.System;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.MenuItem;
import org.codeaurora.gallery.R;

import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.ApiHelper.SystemProperties;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AbstractPermissionPreferenceActivity {

    private static final String LOG_TAG = "SettingsActivity";

    public  static final String PREFERENCE_RTP_MINPORT = "rtp_min_port";
    public  static final String PREFERENCE_RTP_MAXPORT = "rtp_max_port";
    private static final String PREFERENCE_KEEP_ALIVE_INTERVAL_SECOND = "keep_alive_interval_second";
    private static final String PREFERENCE_CACHE_MIN_SIZE = "cache_min_size";
    private static final String PREFERENCE_CACHE_MAX_SIZE = "cache_max_size";
    public  static final String PREFERENCE_BUFFER_SIZE = "buffer_size";
    public  static final String PREFERENCE_APN_CATEGORY = "apn_category";
    public  static final String PREFERENCE_APN = "apn";
    private static final String PACKAGE_NAME  = "com.android.settings";

    private static final int DEFAULT_RTP_MINPORT = 8192;
    private static final int DEFAULT_RTP_MAXPORT = 65535;
    private static final int DEFAULT_CACHE_MIN_SIZE = 4 * 1024 * 1024;
    private static final int DEFAULT_CACHE_MAX_SIZE = 20 * 1024 * 1024;
    private static final int DEFAULT_KEEP_ALIVE_INTERVAL_SECOND = 15;

    private static final int RTP_MIN_PORT = 1;
    private static final int RTP_MAX_PORT = 2;
    private static final int BUFFER_SIZE  = 3;
    private static final boolean DBG = true;
    private static final String TAG = SettingsActivity.class.getSimpleName();

    private SharedPreferences  mPref;
    private EditTextPreference mRtpMinPort;
    private EditTextPreference mRtpMaxPort;
    private EditTextPreference mBufferSize;
    private PreferenceCategory mApnCategory;
    private PreferenceScreen   mApn;

    private static final int    SELECT_APN = 1;
    public  static final String PREFERRED_APN_URI = "content://telephony/carriers/preferapn";
    private static final Uri    PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);
    private static final int    COLUMN_ID_INDEX = 0;
    private static final int    NAME_INDEX = 1;

    private boolean mUseNvOperatorForEhrpd = SystemProperties.getBoolean(
            "persist.radio.use_nv_for_ehrpd", false);

    private SubscriptionManager mSubscriptionManager;
    private List<SubscriptionInfo> mActiveSubInfos;

    @Override
    protected void onGetPermissionsSuccess() {
        init();
    }

    @Override
    protected void onGetPermissionsFailure() {
        finish();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.rtsp_settings_preferences);

        mPref = getPreferenceScreen().getSharedPreferences();
        mRtpMinPort = (EditTextPreference) findPreference(PREFERENCE_RTP_MINPORT);
        mRtpMaxPort = (EditTextPreference) findPreference(PREFERENCE_RTP_MAXPORT);
        mBufferSize = (EditTextPreference) findPreference(PREFERENCE_BUFFER_SIZE);
        mApnCategory = (PreferenceCategory) findPreference(PREFERENCE_APN_CATEGORY);
        mApn = (PreferenceScreen) findPreference(PREFERENCE_APN);

        setPreferenceListener(RTP_MIN_PORT, mRtpMinPort);
        setPreferenceListener(RTP_MAX_PORT, mRtpMaxPort);
        setPreferenceListener(BUFFER_SIZE, mBufferSize);
        setApnListener();

        ActionBar ab = getActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        ab.setDisplayHomeAsUpEnabled(true);
        ab.setTitle(R.string.setting);

        if (isPermissionGranted()) {
            init();
        }
    }

    private void init() {
        mSubscriptionManager = SubscriptionManager.from(this);
        try{
            mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        }catch (Exception e) {
            Log.e(TAG,e.toString());
            mSubscriptionManager = null;
            return;
        }
        // Initialize mActiveSubInfo
        int max = mSubscriptionManager.getActiveSubscriptionInfoCountMax();
        mActiveSubInfos = new ArrayList<SubscriptionInfo>(max);

        initializeSubscriptions();

        if (!hasActiveSubscriptions()) {
            ((PreferenceGroup)mApnCategory).removePreference(mApn);
            getPreferenceScreen().removePreference(mApnCategory);

        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (null != mSubscriptionManager) {
            mSubscriptionManager
                    .removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        }
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DBG) Log.i(TAG, "onSubscriptionsChanged:");
            initializeSubscriptions();
            if (hasActiveSubscriptions()) {
                getPreferenceScreen().addPreference(mApnCategory);
                mApnCategory.addPreference(mApn);
            } else{
                ((PreferenceGroup)mApnCategory).removePreference(mApn);
                getPreferenceScreen().removePreference(mApnCategory);
            }
        }
    };

    private void initializeSubscriptions() {
        if (isDestroyed()) { // Process preferences in activity only if its not destroyed
            return;
        }
        if (DBG) Log.i(TAG, "initializeSubscriptions:+");

        // Before updating the the active subscription list check
        // if tab updating is needed as the list is changing.
         List<SubscriptionInfo> sil = mSubscriptionManager.getActiveSubscriptionInfoList();

        // Update to the active subscription list
        mActiveSubInfos.clear();
        if (sil != null) {
            mActiveSubInfos.addAll(sil);
        }

        if (DBG) Log.i(TAG, "initializeSubscriptions:-");
    }

    private boolean hasActiveSubscriptions() {
        return mActiveSubInfos.size() > 0;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_APN) {
            setResult(resultCode);
            finish();
            Log.w(LOG_TAG, "onActivityResult requestCode = " + requestCode);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }

    private String getOperatorNumericSelection() {
        String[] mccmncs = getOperatorNumeric();
        String where;
        where = (mccmncs[0] != null) ? "numeric=\"" + mccmncs[0] + "\"" : "";
        where += (mccmncs[1] != null) ? " or numeric=\"" + mccmncs[1] + "\"" : "";
        Log.d(LOG_TAG, "getOperatorNumericSelection: " + where);
        return where;
    }

    private String[] getOperatorNumeric() {
        ArrayList<String> result = new ArrayList<String>();
        if (mUseNvOperatorForEhrpd) {
            String mccMncForEhrpd = SystemProperties.get("ro.cdma.home.operator.numeric", null);
            if (mccMncForEhrpd != null && mccMncForEhrpd.length() > 0) {
                result.add(mccMncForEhrpd);
            }
        }

        TelephonyManager manager = (TelephonyManager) getApplicationContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        String mccMncFromSim = manager.getSimOperator();
        if (mccMncFromSim != null && mccMncFromSim.length() > 0) {
            result.add(mccMncFromSim);
        }
        return result.toArray(new String[2]);
    }

    private void setPreferenceListener(final int which, final EditTextPreference etp) {

        final String DIGITS_ACCEPTABLE = "0123456789";
        String summaryStr = "";
        String preferStr  = "";

        switch (which) {
            case RTP_MIN_PORT:
                preferStr = mPref.getString(PREFERENCE_RTP_MINPORT,
                        Integer.toString(DEFAULT_RTP_MINPORT));
                summaryStr = "streaming_min_udp_port";
                break;
            case RTP_MAX_PORT:
                preferStr = mPref.getString(PREFERENCE_RTP_MAXPORT,
                        Integer.toString(DEFAULT_RTP_MAXPORT));
                summaryStr = "streaming_max_udp_port";
                break;
            case BUFFER_SIZE:
                preferStr = mPref.getString(PREFERENCE_BUFFER_SIZE,
                        Integer.toString(DEFAULT_CACHE_MAX_SIZE));
                break;
            default:
                return;

        }

        final String summaryString = summaryStr;
        etp.getEditText().setKeyListener(DigitsKeyListener.getInstance(DIGITS_ACCEPTABLE));
        etp.setSummary(preferStr);
        etp.setText(preferStr);
        etp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                final int value;
                try {
                    value = Integer.valueOf(summary);
                } catch (NumberFormatException e) {
                    Log.e(LOG_TAG, "NumberFormatException");
                    return false;
                }
                etp.setSummary(summary);
                etp.setText(summary);
                Log.d(LOG_TAG, "z66/z82 summary = " + summary);
                return true;
            }
        });

    }

    private void setApnListener() {
        final String SUBSCRIPTION_KEY = "subscription";
        final String SUB_ID = "sub_id";
        mApn.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(Settings.ACTION_APN_SETTINGS);
                int subscription = 0;
                try {
                    subscription = Settings.Global.getInt(
                            SettingsActivity.this.getContentResolver(),
                            ApiHelper.MULTI_SIM_DATA_CALL_SUBSCRIPTION);
                } catch (Exception e) {
                    Log.d("SettingActivity", "Can't get subscription for Exception: " + e);
                } finally {
                    intent.putExtra(SUBSCRIPTION_KEY, subscription);
                    intent.putExtra(SUB_ID, subscription);
                }
                startActivityForResult(intent, SELECT_APN);
                return true;
            }
        });
    }
}

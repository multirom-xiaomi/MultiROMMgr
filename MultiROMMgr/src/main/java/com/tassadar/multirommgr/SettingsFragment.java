package com.tassadar.multirommgr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.Toast;

import com.tassadar.multirommgr.installfragment.UbuntuInstallTask;

import java.lang.ref.WeakReference;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    public static final String GENERAL_UPDATE_CHECK =  "general_update_check";
    public static final String GENERAL_AUTO_REBOOT = "general_auto_reboot";
    public static final String GENERAL_DOWNLOAD_DIR = "general_download_dir";
    public static final String UTOUCH_SHOW_HIDDEN = "utouch_show_hidden";
    public static final String UTOUCH_DELETE_FILES = "utouch_delete_files";
    public static final String ABOUT_VERSION = "about_version";
    public static final String ABOUT_LICENSES = "about_licenses";
    public static final String DEV_ENABLE = "dev_enable";
    public static final String DEV_OVERRIDE_MANIFEST = "dev_manifest_url_override";
    public static final String DEV_MANIFEST_URL = "dev_manifest_url_v2";
    public static final String DEV_DEVICE_NAME = "dev_device_name";

    private static final String SRC_URL = "http://github.com/multirom-xiaomi/MultiROMMgr";
    private static final int DEV_STEPS = 7;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        SharedPreferences p = MgrApp.getPreferences();
        if(p.getString(GENERAL_DOWNLOAD_DIR, null) == null)
            Utils.setDownloadDir(Utils.getDefaultDownloadDir());

        addDevOptions();

        p.registerOnSharedPreferenceChangeListener(this);
        updatePrefText();

        try {
            PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            Preference pref = findPreference(ABOUT_VERSION);
            pref.setSummary(pInfo.versionName);
            pref.setOnPreferenceClickListener(this);
        } catch(PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Preference pref = findPreference(ABOUT_LICENSES);
        pref.setOnPreferenceClickListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        SharedPreferences p = MgrApp.getPreferences();
        p.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void updatePrefText() {
        Preference pref = findPreference(UTOUCH_DELETE_FILES);
        pref.setSummary(Utils.getString(R.string.pref_delete_utouch_files_summ,
                Utils.getDownloadDir(), UbuntuInstallTask.DOWN_DIR));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences p, String key) {
        if(key.equals(GENERAL_UPDATE_CHECK)) {
            UpdateChecker.updateAlarmStatus();
        } else if(key.equals(GENERAL_DOWNLOAD_DIR)) {
            Utils.setDownloadDir(p.getString(key, Utils.getDefaultDownloadDir()));
            updatePrefText();
        } else if(key.equals(DEV_DEVICE_NAME) && !p.getBoolean(DEV_OVERRIDE_MANIFEST, false)) {
            SharedPreferences.Editor e = p.edit();
            e.remove(DEV_MANIFEST_URL);
            e.commit();

            Device dev = Device.load(p.getString(DEV_DEVICE_NAME, Build.DEVICE));
            EditTextPreference pref = (EditTextPreference)findPreference(DEV_MANIFEST_URL);
            pref.setText(dev != null ? dev.getDefaultManifestUrl() : Build.DEVICE);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if(pref.getKey().equals(ABOUT_VERSION)) {
            if(m_clickCounter == -1) {
                showDevToast(R.string.already_developer);
                return true;
            }

            ++m_clickCounter;

            if(m_clickCounter == DEV_STEPS) {
                SharedPreferences.Editor p = MgrApp.getPreferences().edit();
                p.putBoolean(DEV_ENABLE, true);
                p.commit();

                addDevOptions();
                showDevToast(R.string.now_developer);
            } else if(m_clickCounter >= 3) {
                showDevToast(R.string.steps_developer, DEV_STEPS - m_clickCounter);
            }
            return true;
        } else if(pref.getKey().equals(ABOUT_LICENSES)) {
            final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(SRC_URL));
            startActivity(intent);
            return true;
        }
        return false;
    }

    private void showDevToast(int stringId, Object... args) {
        showDevToast(getString(stringId, args));
    }

    private void showDevToast(String text) {
        Toast t = m_clickCountToast.get();
        if(t == null) {
            t = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
            m_clickCountToast = new WeakReference<Toast>(t);
        } else {
            t.setText(text);
        }

        t.show();
    }

    private void addDevOptions() {
        SharedPreferences p = MgrApp.getPreferences();
        if(!p.getBoolean(DEV_ENABLE, false))
            return;

        m_clickCounter = -1;
        addPreferencesFromResource(R.xml.dev_options);

        Device dev = Device.load(p.getString(DEV_DEVICE_NAME, Build.DEVICE));

        CheckBoxPreference cpref = (CheckBoxPreference)findPreference(DEV_OVERRIDE_MANIFEST);
        cpref.setChecked(p.getBoolean(DEV_OVERRIDE_MANIFEST, false));

        EditTextPreference pref = (EditTextPreference)findPreference(DEV_MANIFEST_URL);
        pref.setText(p.getString(DEV_MANIFEST_URL, dev != null ? dev.getDefaultManifestUrl() : Build.DEVICE));
        pref = (EditTextPreference)findPreference(DEV_DEVICE_NAME);
        pref.setText(p.getString(DEV_DEVICE_NAME, Build.DEVICE));

    }

    private int m_clickCounter;
    private WeakReference<Toast> m_clickCountToast = new WeakReference<Toast>(null);
}

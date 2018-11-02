package ir.mahdi.circulars;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Locale;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import ir.mahdi.circulars.helper.Prefs;

public class SettingsActivity extends AppCompatPreferenceActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String languageToLoad  = "fa";
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());

        // load settings fragment
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }
    public static class MainPreferenceFragment extends PreferenceFragment {

        PackageInfo pInfo;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);

            try {
                pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            // feedback preference click listener
            Preference myPref = findPreference(getString(R.string.key_send_feedback));
            Preference myPrefStar = findPreference(getString(R.string.key_send_star));
            Preference myPrefRegion = findPreference(getString(R.string.key_region));
            Preference myPrefChangelog = findPreference(getString(R.string.key_check_changelog));
            Preference myPrefTelegram = findPreference(getString(R.string.key_telegram));
            Preference myPrefVer = findPreference(getString(R.string.key_version));

            myPrefVer.setTitle(getString(R.string.app_version)+ " " + pInfo.versionName);
            myPrefVer.setSummary(getString(R.string.app_version_name) + " " + pInfo.versionCode);


            myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    sendFeedback(getActivity());
                    return true;
                }
            });

            myPrefStar.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        Intent intents = new Intent(Intent.ACTION_EDIT);
                        intents.setData(Uri.parse("bazaar://details?id=ir.mahdi.circulars"));
                        intents.setPackage("com.farsitel.bazaar");
                        startActivity(intents);
                    } catch (Exception e) {
                        snackbarShow(getString(R.string.cafebazaar_error));
                    }
                    return true;
                }
            });

            myPrefRegion.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        chooseServer();
                    } catch (Exception e) {
                    }
                    return true;
                }
            });

            myPrefChangelog.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    changelog();
                    return true;
                }
            });

            myPrefTelegram.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    String url = getString(R.string.telegram_user);
                    Intent next = new Intent(Intent.ACTION_VIEW);
                    try {
                        next.setData(Uri.parse(url));
                        startActivity(next);
                    } catch (Exception e) {
                        snackbarShow(getString(R.string.telegram_error));
                    }
                    return true;
                }
            });
        }
        private void chooseServer() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustom);
            builder.setTitle(getString(R.string.select_region));
            builder.setCancelable(false);

            builder.setSingleChoiceItems(R.array.server, Prefs.getSERVER(getActivity()), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Prefs.setSERVER(getActivity(), i);
                    Prefs.setLUANCH(getActivity(), 1);
                }
            });
            builder.setPositiveButton("ذخیره سرور", null);
            builder.setNegativeButton("بیخیال", null);
            builder.show();
        }
        private void changelog() {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AlertDialogCustom);
            builder.setTitle(getString(R.string.changeLogTitle));
            builder.setItems(R.array.changeLog,null);
            builder.setPositiveButton("حله", null);
            builder.setCancelable(false);
            builder.show();
        }
        private void snackbarShow(String message) {
            Snackbar snackbar = Snackbar.make(getView(), message, 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                TextView view1 = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                view1.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            }
            snackbar.setAction("تایید", null);
            snackbar.show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Email client intent to send support mail
     * Appends the necessary device information to email body
     * useful when providing support
     */
    public static void sendFeedback(Context context) {
        String body = null;
        try {
            body = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            body = "\n\n-----------------------------\nPlease don't remove this information\n Device OS: Android \n Device OS version: " +
                    Build.VERSION.RELEASE + "\n App Version: " + body + "\n Device Brand: " + Build.BRAND +
                    "\n Device Model: " + Build.MODEL + "\n Device Manufacturer: " + Build.MANUFACTURER;
        } catch (PackageManager.NameNotFoundException e) {
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{context.getString(R.string.email)});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Query from android app");
        intent.putExtra(Intent.EXTRA_TEXT, body);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.choose_email_client)));
    }
}

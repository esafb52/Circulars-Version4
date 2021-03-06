package ir.mahdi.circulars;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.folderselector.FileChooserDialog;

import java.io.File;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import ir.mahdi.circulars.fragment.CircularFragment;
import ir.mahdi.circulars.fragment.ImageFragment;
import ir.mahdi.circulars.fragment.LocalFragment;
import ir.mahdi.circulars.fragment.PdfFragment;
import ir.mahdi.circulars.helper.BottomNavigationBehavior;
import ir.mahdi.circulars.helper.CustomTypefaceSpan;
import ir.mahdi.circulars.helper.Prefs;

public class MainActivity extends AppCompatActivity implements FileChooserDialog.FileCallback {

    private ActionBar toolbar;
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            Fragment fragment;
            switch (item.getItemId()) {
                case R.id.navigation_circular:
                    getToolbarTitle();
                    fragment = new CircularFragment();
                    loadFragment(fragment);
                    return true;
                case R.id.navigation_local:
                    toolbar.setTitle(getString(R.string.title_gifts));
                    fragment = new LocalFragment();
                    loadFragment(fragment);
                    return true;
                case R.id.navigation_profile:
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        forceRTLIfSupported();
        toolbar = getSupportActionBar();

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        Menu m = navigation.getMenu();
        Typeface tf1 = Typeface.createFromAsset(getAssets(), "fonts/IRANSansMobile.ttf");

        for (int i = 0; i < m.size(); i++) {

            MenuItem mi = m.getItem(i);

            SpannableString s = new SpannableString(mi.getTitle());
            s.setSpan(new CustomTypefaceSpan("", tf1), 0, s.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mi.setTitle(s);
        }
        navigation.getMenu().findItem(R.id.navigation_circular).setChecked(true);

        // attaching bottom sheet behaviour - hide / show on scroll
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) navigation.getLayoutParams();
        layoutParams.setBehavior(new BottomNavigationBehavior());

        // load the store fragment by default
        getToolbarTitle();
        loadFragment(new CircularFragment());

    }
    @Override
    public void onBackPressed(){
        if (getSupportFragmentManager().getBackStackEntryCount() == 1){
            finish();
        }
        else {
            super.onBackPressed();
        }
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void forceRTLIfSupported()
    {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1){
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
    }
    /**
     * loading fragment into FrameLayout
     *
     * @param fragment
     */
    private void loadFragment (Fragment fragment){
        String backStateName =  fragment.getClass().getName();
        String fragmentTag = backStateName;

        FragmentManager manager = getSupportFragmentManager();
        boolean fragmentPopped = manager.popBackStackImmediate (backStateName, 0);

        if (!fragmentPopped && manager.findFragmentByTag(fragmentTag) == null){ //fragment not in back stack, create it.
            FragmentTransaction ft = manager.beginTransaction();
            ft.replace(R.id.frame_container, fragment, fragmentTag);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(backStateName);
            ft.commit();
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    private void getToolbarTitle() {
        int title_Position = Prefs.getSERVER(getApplicationContext());
        String[] title_Name = getResources().getStringArray(R.array.server);
        toolbar.setTitle(title_Name[title_Position]);
    }

    public void setToolbarTitle(String Title) {
        toolbar.setTitle(Title);
    }

    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
        if (file.getName().contains(".pdf")) {
            loadFragment(true, "FILE_NAME", file.getPath());
        } else {
            loadFragment(false, "FILE_NAME", file.getPath());
        }
    }

    @Override
    public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {

    }

    private void loadFragment(Boolean isPdf, String KEY, String Data) {
        // load fragment
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment;
        Bundle bundle = new Bundle();

        if (isPdf) {
            fragment = new PdfFragment();
            bundle.putString(KEY, Data);
            fragment.setArguments(bundle);

        } else {
            fragment = new ImageFragment();
            bundle.putString(KEY, Data);
            fragment.setArguments(bundle);
        }
        transaction.replace(R.id.frame_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }
}

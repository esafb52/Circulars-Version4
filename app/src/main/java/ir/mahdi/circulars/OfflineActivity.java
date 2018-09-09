package ir.mahdi.circulars;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.folderselector.FileChooserDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.inflationx.viewpump.ViewPumpContextWrapper;
import ir.mahdi.circulars.adapter.OfflineAdapter;
import ir.mahdi.circulars.helper.BottomNavigationViewHelper;
import ir.mahdi.circulars.model.OfflineItem;

public class OfflineActivity extends AppCompatActivity implements FileChooserDialog.FileCallback, SearchView.OnQueryTextListener{
    private RecyclerView rv;
    String _Path = "/sdcard/بخشنامه/";
    String myTitle;


    List<OfflineItem> filteredModelList;
    private List<OfflineItem> persons;
    OfflineAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavView_Bar);
        BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) bottomNavigationView.getLayoutParams();
        layoutParams.setBehavior(new BottomNavigationViewHelper());

        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(1);
        menuItem.setChecked(true);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()){

                    case R.id.navigation_circular:
                        Intent intent1 = new Intent(OfflineActivity.this, CircularActivity.class);
                        startActivity(intent1);
                        break;

                    case R.id.navigation_offline:
                        Intent intent2 = new Intent(OfflineActivity.this, OfflineActivity.class);
                        startActivity(intent2);
                        break;

                    case R.id.navigation_profile:
                        startActivity(new Intent(OfflineActivity.this, AboutActivity.class));
                        break;
                }


                return false;
            }
        });

        initializeData();
        rv = findViewById(R.id.rv);
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getApplicationContext());
        rv.setLayoutManager(llm);
        adapter = new OfflineAdapter(persons);
        rv.setAdapter(adapter);

        rv.addOnItemTouchListener(new OfflineAdapter.RecyclerTouchListener(getApplicationContext(), rv, new OfflineAdapter.ClickListener() {

            @Override
            public void onClick(View view, int position) {
                final String loc = ((TextView) rv.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.person_name)).getText().toString();
                myTitle = loc;
                chooseFile();
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
    }

    private void chooseFile() {
        new FileChooserDialog.Builder(this)
                .initialPath(_Path)  // changes initial path, defaults to external storage directory
                .mimeType("application/*") // Optional MIME type filter
                .extensionsFilter(".pdf", ".jpg", ".png") // Optional extension filter, will override mimeType()
                .goUpLabel("قبلی") // custom go up label, default label is "..."
                .show(this);
    }
    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
        if (file.getName().contains(".pdf")) {
            startActivity(new Intent(OfflineActivity.this, PdfViewerActivity.class).putExtra("FILE_NAME", file.getPath()));
        } else {
            startActivity(new Intent(OfflineActivity.this, ImageViewerActivity.class).putExtra("FILE_NAME", file.getPath()));
        }
    }

    @Override
    public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {

    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
    }

    private void initializeData(){
        persons = new ArrayList<>();

        File file = new File(Environment.getExternalStorageDirectory() + "/بخشنامه/");
        if(file.isDirectory() == false)
        {
            return;
        }
        File[] files = file.listFiles();
        int i = 1;
        for(File f : files)
        {
            if(f.isFile() || f.isDirectory())
            {
                try
                {
                    persons.add(new OfflineItem(f.getName()));
                    i++;
                }
                catch(Exception e){}
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        final MenuItem item = menu.findItem(R.id.action_search);
        final MenuItem goneItem = menu.findItem(R.id.action_server);
        goneItem.setVisible(false);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            searchView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            searchView.setTextDirection(View.TEXT_DIRECTION_RTL);
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String query) {
        filteredModelList = filter(persons, query);
        adapter.setFilter(filteredModelList);
        return true;
    }
    private List<OfflineItem> filter(List<OfflineItem> models, String query) {
        query = query.toLowerCase();

        final List<OfflineItem> filteredModelList = new ArrayList<>();
        for (OfflineItem model : models) {
            final String text = model.getTitle().toLowerCase();
            if (text.contains(query)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }
}

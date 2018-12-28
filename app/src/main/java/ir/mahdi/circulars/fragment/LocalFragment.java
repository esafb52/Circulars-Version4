package ir.mahdi.circulars.fragment;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.afollestad.materialdialogs.folderselector.FileChooserDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ir.mahdi.circulars.R;
import ir.mahdi.circulars.adapter.OfflineAdapter;
import ir.mahdi.circulars.model.OfflineItem;

public class LocalFragment extends Fragment implements SearchView.OnQueryTextListener {

    private static final int STORAGE_PERMISSION_RC = 69;
    String _Path = "/sdcard/بخشنامه/";
    String myTitle;
    List<OfflineItem> filteredModelList;
    OfflineAdapter adapter;
    private RecyclerView rv;
    private List<OfflineItem> persons;

    public LocalFragment() {
        // Required empty public constructor
    }

    public static LocalFragment newInstance() {
        LocalFragment fragment = new LocalFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_local, container, false);

        initializeData();
        rv = view.findViewById(R.id.rv);
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        rv.setLayoutManager(llm);
        adapter = new OfflineAdapter(persons);
        rv.setAdapter(adapter);

        rv.addOnItemTouchListener(new OfflineAdapter.RecyclerTouchListener(getContext(), rv, new OfflineAdapter.ClickListener() {

            @Override
            public void onClick(View view, int position) {
                final String loc = ((TextView) rv.findViewHolderForAdapterPosition(position).itemView.findViewById(R.id.person_name)).getText().toString();
                myTitle = loc;
                chooseFile(_Path + loc);
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        return view;
    }

    private void chooseFile(String path) {

        if (ActivityCompat.checkSelfPermission(
                getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    STORAGE_PERMISSION_RC);
            return;
        }
        new FileChooserDialog.Builder(getContext())
                .initialPath(path)
                .mimeType("application/*")
                .extensionsFilter(".pdf", ".jpg", ".png", ".tif")
                .goUpLabel("قبلی")
                .show(getActivity());
    }

    private void initializeData() {
        persons = new ArrayList<>();

        File file = new File(Environment.getExternalStorageDirectory() + "/بخشنامه/");
        if (file.isDirectory() == false) {
            return;
        }
        File[] files = file.listFiles();
        int i = 1;
        for (File f : files) {
            if (f.isFile() || f.isDirectory()) {
                try {
                    persons.add(new OfflineItem(f.getName()));
                    i++;
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
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

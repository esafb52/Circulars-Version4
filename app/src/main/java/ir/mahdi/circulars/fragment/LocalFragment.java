package ir.mahdi.circulars.fragment;


import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
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

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ir.mahdi.circulars.R;
import ir.mahdi.circulars.adapter.OfflineAdapter;
import ir.mahdi.circulars.model.OfflineItem;


public class LocalFragment extends Fragment implements SearchView.OnQueryTextListener {

    private RecyclerView rv;
    String _Path = "/sdcard/بخشنامه/";
    String myTitle;


    List<OfflineItem> filteredModelList;
    private List<OfflineItem> persons;
    OfflineAdapter adapter;

    public LocalFragment() {
        // Required empty public constructor
    }

    public static LocalFragment newInstance(String param1, String param2) {
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
                chooseFile();
            }

            @Override
            public void onLongClick(View view, int position) {

            }
        }));
        return view;
    }
    private void chooseFile() {

        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(_Path);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        String[] extens = new String[]{".pdf", ".jpg", ".png"};
        properties.extensions = extens;
        FilePickerDialog dialog = new FilePickerDialog(getContext(),properties);
        dialog.setTitle("انتخاب بخشنامه");
        dialog.setNegativeBtnName("انصراف");
        dialog.setPositiveBtnName("انتخاب");
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                if (files[0].contains(".pdf")) {
                    loadFragment(true,"FILE_NAME", files[0]);
                } else {
                    loadFragment(false,"FILE_NAME", files[0]);
                }
            }
        });
        dialog.show();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu,inflater);
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
    private void loadFragment(Boolean isPdf, String KEY, String Data) {
        // load fragment
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        Fragment fragment;
        Bundle bundle = new Bundle();

        if(isPdf){
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

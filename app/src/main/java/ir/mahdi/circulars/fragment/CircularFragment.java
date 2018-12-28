package ir.mahdi.circulars.fragment;


import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.downloader.Error;
import com.downloader.OnDownloadListener;
import com.downloader.OnProgressListener;
import com.downloader.PRDownloader;
import com.downloader.Progress;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import co.ronash.pushe.Pushe;
import ir.mahdi.circulars.MainActivity;
import ir.mahdi.circulars.R;
import ir.mahdi.circulars.adapter.MessagesAdapter;
import ir.mahdi.circulars.archive.Rar.Archive;
import ir.mahdi.circulars.archive.Rar.exception.RarException;
import ir.mahdi.circulars.archive.Rar.rarfile.FileHeader;
import ir.mahdi.circulars.archive.Zip.Decompress;
import ir.mahdi.circulars.helper.DividerItemDecoration;
import ir.mahdi.circulars.helper.Prefs;
import ir.mahdi.circulars.model.Message;

public class CircularFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        MessagesAdapter.MessageAdapterListener, SearchView.OnQueryTextListener {

    private static final int STORAGE_PERMISSION_RC = 69;
    public boolean isClickable = true;
    Message message;
    String _Path = "/sdcard/بخشنامه/";
    boolean isSearchActive = false;
    List<Message> filteredModelList;
    String fileFullName;
    ProgressBar progressBar;
    String fileext;
    String mylFileName;
    String ArchivePath;
    String ArchiveFolderName;
    RecyclerView.LayoutManager mLayoutManager;
    ShimmerFrameLayout shimmerContainer;
    TextView txtNonItem;
    private ArrayList<Message> messages;
    private RecyclerView recyclerView;
    private MessagesAdapter mAdapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ActionMode actionMode;
    private ActionModeCallback actionModeCallback;

    public CircularFragment() {
        // Required empty public constructor
    }

    public static CircularFragment newInstance() {
        CircularFragment fragment = new CircularFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    //region Create Dir and Extract Archive
    public static boolean createDirIfNotExists(String path) {
        boolean ret = true;

        File file = new File(Environment.getExternalStorageDirectory(), path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                ret = false;
            }
        }
        return ret;
    }

    public static void extractArchive(String archive, String destination) {
        if (archive == null || destination == null) {
            throw new RuntimeException("archive and destination must me set");
        }
        File arch = new File(archive);
        if (!arch.exists()) {
            throw new RuntimeException("the archive does not exit: " + archive);
        }
        File dest = new File(destination);
        if (!dest.exists() || !dest.isDirectory()) {
            throw new RuntimeException(
                    "the destination must exist and point to a directory: "
                            + destination);
        }
        extractArchive(arch, dest);
    }

    public static void extractArchive(File archive, File destination) {
        Archive arch = null;
        try {
            arch = new Archive(archive);
        } catch (RarException e) {
        } catch (IOException e1) {
        }
        if (arch != null) {
            if (arch.isEncrypted()) {
                // logger.warn("archive is encrypted cannot extreact");
                return;
            }
            FileHeader fh = null;
            while (true) {
                fh = arch.nextFileHeader();
                if (fh == null) {
                    break;
                }
                if (fh.isEncrypted()) {
                    // logger.warn("file is encrypted cannot extract: "
                    //        + fh.getFileNameString());
                    continue;
                }
                // logger.info("extracting: " + fh.getFileNameString());
                try {
                    if (fh.isDirectory()) {
                        createDirectory(fh, destination);
                    } else {
                        File f = createFile(fh, destination);
                        OutputStream stream = new FileOutputStream(f);
                        arch.extractFile(fh, stream);
                        stream.close();
                    }
                } catch (IOException e) {
                    //   logger.error("error extracting the file", e);
                } catch (RarException e) {
                    //   logger.error("error extraction the file", e);
                }
            }
        }
    }

    private static File createFile(FileHeader fh, File destination) {
        File f = null;
        String name = null;
        if (fh.isFileHeader() && fh.isUnicode()) {
            name = fh.getFileNameW();
        } else {
            name = fh.getFileNameString();
        }
        f = new File(destination, name);
        if (!f.exists()) {
            try {
                f = makeFile(destination, name);
            } catch (IOException e) {
                //logger.error("error creating the new file: " + f.getName(), e);
            }
        }
        return f;
    }

    private static File makeFile(File destination, String name)
            throws IOException {
        String[] dirs = name.split("\\\\");
        if (dirs == null) {
            return null;
        }
        String path = "";
        int size = dirs.length;
        if (size == 1) {
            return new File(destination, name);
        } else if (size > 1) {
            for (int i = 0; i < dirs.length - 1; i++) {
                path = path + File.separator + dirs[i];
                new File(destination, path).mkdir();
            }
            path = path + File.separator + dirs[dirs.length - 1];
            File f = new File(destination, path);
            f.createNewFile();
            return f;
        } else {
            return null;
        }
    }

    private static void createDirectory(FileHeader fh, File destination) {
        File f = null;
        if (fh.isDirectory() && fh.isUnicode()) {
            f = new File(destination, fh.getFileNameW());
            if (!f.exists()) {
                makeDirectory(destination, fh.getFileNameW());
            }
        } else if (fh.isDirectory() && !fh.isUnicode()) {
            f = new File(destination, fh.getFileNameString());
            if (!f.exists()) {
                makeDirectory(destination, fh.getFileNameString());
            }
        }
    }

    private static void makeDirectory(File destination, String fileName) {
        String[] dirs = fileName.split("\\\\");
        if (dirs == null) {
            return;
        }
        String path = "";
        for (String dir : dirs) {
            path = path + File.separator + dir;
            new File(destination, path).mkdir();
        }

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    View view;
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (view != null){
            if (view.getParent() != null)
                ((ViewGroup) view.getParent()).removeView(view);
            return view;
        }
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_circular, container, false);

        AppCenter.start(getActivity().getApplication(), "442e4224-ce5c-42ce-a183-08b03eb28414",
                Analytics.class, Crashes.class);
        Pushe.initialize(getContext(), true);

        isStoragePermissionGranted();
        createDirIfNotExists("بخشنامه");

        getToolbarTitle();

        txtNonItem = view.findViewById(R.id.txtNonItem);
        progressBar = view.findViewById(R.id.progressBar);
        recyclerView = view.findViewById(R.id.recycler_view);

        shimmerContainer = view.findViewById(R.id.shimmer_view_container);

        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeColors(getRandomMaterialColor("400"));


        mLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        messages = new ArrayList<>();
        mAdapter = new MessagesAdapter(getContext(), messages, this);
        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayoutManager.VERTICAL, 16));


        recyclerView.setAdapter(mAdapter);
        if (Prefs.getLUANCH(getContext()) == 0) {
            chooseServer();
        } else {
            showShimmerEffect(true);
            new JsoupListView().execute();
        }

        actionModeCallback = new ActionModeCallback();
        swipeRefreshLayout.post(
                new Runnable() {
                    @Override
                    public void run() {
                        new JsoupListView().execute();
                    }
                }
        );
        return view;
    }

    private void getToolbarTitle() {
        int title_Position = Prefs.getSERVER(getContext());
        String[] title_Name = getResources().getStringArray(R.array.server);
        ((MainActivity) getActivity())
                .setToolbarTitle(title_Name[title_Position]);
    }
    //endregion

    private void showShimmerEffect(boolean isTrue) {
        shimmerContainer.setAngle(ShimmerFrameLayout.MaskAngle.CW_180);
        if (isTrue) {
            shimmerContainer.setVisibility(View.VISIBLE);
            swipeRefreshLayout.setRefreshing(false);
            shimmerContainer.startShimmerAnimation();
        } else {
            shimmerContainer.setVisibility(View.GONE);
            swipeRefreshLayout.setRefreshing(false);
            shimmerContainer.stopShimmerAnimation();
        }
    }

    /**
     * @param message
     * @param length  Long = 0, Short = 1
     */

    private void snackbarShow(String message, int length) {
        try {
            Snackbar snackbar = Snackbar.make(getActivity().getWindow().getDecorView(), message, length);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                TextView view1 = snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                view1.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            }
            snackbar.setAction("تایید", null);
            snackbar.show();
        } catch (NullPointerException nx) {
            Log.d("NullPointerException", nx.getMessage());
        }
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

    public void clear() {
        final int size = messages.size();
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                messages.remove(0);
            }
            mAdapter.notifyItemRangeRemoved(0, size);
        }
    }

    // deleting the messages from recycler view
    private void deleteMessages() {
        mAdapter.resetAnimationIndex();
        List<Integer> selectedItemPositions =
                mAdapter.getSelectedItems();
        for (int i = selectedItemPositions.size() - 1; i >= 0; i--) {
            message = messages.get(selectedItemPositions.get(i));
            File file = new File(_Path + message.getFrom());
            deleteRecursive(file);
            mAdapter.removeData(selectedItemPositions.get(i));
        }

        mAdapter.notifyDataSetChanged();
    }

    void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        fileOrDirectory.delete();
    }

    private void chooseServer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext(), R.style.AlertDialogCustom);
        builder.setTitle(getString(R.string.select_region));
        builder.setCancelable(false);

        builder.setSingleChoiceItems(R.array.server, Prefs.getSERVER(getContext()), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                clear();
                Prefs.setSERVER(getContext(), i);
                getToolbarTitle();
            }
        });
        builder.setPositiveButton("ذخیره سرور", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                showShimmerEffect(true);
                Prefs.setLUANCH(getContext(), 1);
                new JsoupListView().execute();
            }
        });
        builder.setNegativeButton("بیخیال", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (Prefs.getLUANCH(getContext()) == 0) {
                    getActivity().finish();
                }
            }
        });
        builder.show();
    }

    private int getRandomMaterialColor(String typeColor) {
        int returnColor = Color.GRAY;
        int arrayId = getResources().getIdentifier("mdcolor_" + typeColor, "array", getActivity().getPackageName());

        if (arrayId != 0) {
            TypedArray colors = getResources().obtainTypedArray(arrayId);
            int index = (int) (Math.random() * colors.length());
            returnColor = colors.getColor(index, Color.GRAY);
            colors.recycle();
        }
        return returnColor;
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (getActivity().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    private List<Message> filter(List<Message> models, String query) {
        query = query.toLowerCase();

        final List<Message> filteredModelList = new ArrayList<>();
        for (Message model : models) {
            final String text = model.getFrom().toLowerCase();
            if (text.contains(query)) {
                filteredModelList.add(model);
            }
        }
        return filteredModelList;
    }

    @Override
    public void onRefresh() {
        clear();
        messages.clear();
        getToolbarTitle();
        showShimmerEffect(true);
        new JsoupListView().execute();
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        filteredModelList = filter(messages, newText);
        mAdapter.setFilter(filteredModelList);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public void onIconClicked(int position) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }
        toggleSelection(position);
    }

    @Override
    public void onIconImportantClicked(int position) {
        if (isSearchActive) {
            Message message = filteredModelList.get(position);
            message.setImportant(!message.isImportant());
            filteredModelList.set(position, message);
            mAdapter.notifyDataSetChanged();
        } else {
            Message message = messages.get(position);
            message.setImportant(!message.isImportant());
            messages.set(position, message);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMessageRowClicked(int position) {
        try {
            if (mAdapter.getSelectedItemCount() > 0) {
                enableActionMode(position);
            } else {
                if (isSearchActive) {
                    message = filteredModelList.get(position);
                    message.setRead(true);
                    filteredModelList.set(position, message);
                    mAdapter.notifyDataSetChanged();
                    String FixFileName = message.getFrom();
                    FixFileName = FixFileName.replaceAll("/", "");
                    File checkExist = new File(_Path + FixFileName + ".pdf");
                    File checkExistZip = new File(_Path + FixFileName);
                    if (checkExist.exists()) {
                        loadFragment(true, "FILE_NAME", _Path + FixFileName + ".pdf");
                    } else {
                        if (checkExistZip.exists()) {
                            chooseFile(_Path + FixFileName);
                        } else {
                            downloader(message.getFrom(), getBaseUrl() + message.getLink());
                        }
                    }
                } else {
                    message = messages.get(position);
                    message.setRead(true);
                    messages.set(position, message);
                    mAdapter.notifyDataSetChanged();
                    String FixFileName = message.getFrom();
                    FixFileName = FixFileName.replaceAll("/", "");
                    File checkExist = new File(_Path + FixFileName + ".pdf");
                    File checkExistZip = new File(_Path + FixFileName);
                    if (checkExist.exists()) {
                        loadFragment(true, "FILE_NAME", _Path + FixFileName + ".pdf");
                    } else {
                        if (checkExistZip.exists()) {
                            chooseFile(_Path + FixFileName);
                        } else {
                            downloader(message.getFrom(), getBaseUrl() + message.getLink());
                        }
                    }
                }
            }
        } catch (IndexOutOfBoundsException index) {
            Log.d("IndexOOfBounds", index.getMessage());
        }

    }

    private String getBaseUrl() {
        String[] get_BaseUrl = getResources().getStringArray(R.array.url);
        return get_BaseUrl[Prefs.getSERVER(getContext())];
    }

    private void downloader(final String filename, final String path) {


        if (isClickable) {
            isClickable = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isStoragePermissionGranted();
            }
            progressBar.setProgress(0);

            new GetFileInfo(new GetFileInfo.GetFileInfoListener() {
                @Override
                public void onTaskCompleted(String fileName) {
                    try {

                        mylFileName = fileName.replaceAll("/", "");
                        fileext = FilenameUtils.getExtension(mylFileName);
                        String dPath;
                        if (fileext.contains("pdf")) {

                            ArchivePath = _Path + filename;
                            ArchiveFolderName = filename.replaceAll("." + fileext, "");
                            dPath = _Path + ArchiveFolderName;
                            fileFullName = ArchiveFolderName + "/" + mylFileName;
                        } else {
                            mylFileName = fileName.replaceAll("/", "");
                            fileext = FilenameUtils.getExtension(mylFileName);
                            ArchivePath = _Path + mylFileName;

                            ArchiveFolderName = mylFileName.replaceAll("." + fileext, "");
                            dPath = _Path;
                        }
                        int downloadId = PRDownloader.download(path, dPath, mylFileName)
                                .build()
                                .setOnProgressListener(new OnProgressListener() {
                                    @Override
                                    public void onProgress(Progress progress) {
                                        long progressPercent = progress.currentBytes * 100 / progress.totalBytes;
                                        progressBar.setProgress((int) progressPercent);

                                    }
                                })
                                .start(new OnDownloadListener() {
                                    @Override
                                    public void onDownloadComplete() {
                                        progressBar.setProgress(0);

                                        isClickable = true;

                                        if (ArchivePath.contains("zip")) {
                                            Decompress.unzip(ArchivePath, _Path + ArchiveFolderName, "");
                                            File file = new File(ArchivePath);
                                            file.delete();
                                            chooseFile(_Path + ArchiveFolderName);
                                        } else if (ArchivePath.contains("rar")) {
                                            createDirIfNotExists("بخشنامه" + "/" + message.getFrom());
                                            extractArchive(ArchivePath, _Path + message.getFrom());
                                            File file = new File(ArchivePath);
                                            file.delete();
                                            chooseFile(_Path + message.getFrom());
                                        } else {
                                            loadFragment(true, "FILE_NAME", _Path + fileFullName);
                                        }
                                    }

                                    @Override
                                    public void onError(Error error) {
                                        snackbarShow("دانلود با خطا مواجه شد", -1);
                                        progressBar.setProgress(0);
                                        isClickable = true;
                                    }
                                });
                    } catch (NullPointerException nx) {
                        Log.d("NullPointerException", nx.getMessage());
                    }
                }
            }).execute(path);
        }
    }

    @Override
    public void onRowLongClicked(int position) {
        enableActionMode(position);
    }

    private void enableActionMode(int position) {
        if (actionMode == null) {
            actionMode = ((AppCompatActivity) getActivity()).startSupportActionMode(actionModeCallback);
        }
        toggleSelection(position);
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        int count = mAdapter.getSelectedItemCount();

        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(String.valueOf(count));
            actionMode.invalidate();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_server:
                chooseServer();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadFragment(Boolean isPdf, String KEY, String Data) {
        // load fragment
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
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

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        super.onCreateOptionsMenu(menu, inflater);
        final MenuItem item = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            searchView.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            searchView.setTextDirection(View.TEXT_DIRECTION_RTL);
        }
        searchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                isSearchActive = true;
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                isSearchActive = false;
            }
        });
    }

    static class GetFileInfo extends AsyncTask<String, Integer, String> {
        private final GetFileInfoListener mListener;

        public GetFileInfo(GetFileInfoListener listener) {
            mListener = listener;
        }

        protected String doInBackground(String... urls) {
            URL url;
            String filename = null;
            try {
                url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.connect();
                conn.setInstanceFollowRedirects(false);

                String depo = conn.getHeaderField("Content-Disposition");

                if (depo != null)
                    filename = depo.replaceFirst("(?i)^.*filename=\"?([^\"]+)\"?.*$", "$1");
                else
                    filename = URLUtil.guessFileName(urls[0], null, null);
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            } catch (IOException e) {
            } catch (NullPointerException x) {
            }
            return filename;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (mListener != null)
                mListener.onTaskCompleted(result);
        }

        public interface GetFileInfoListener {
            void onTaskCompleted(String result);
        }
    }

    private class ActionModeCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_action_mode, menu);

            showShimmerEffect(false);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteMessages();
                    mode.finish();
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.clearSelections();
            showShimmerEffect(false);
            swipeRefreshLayout.setColorSchemeColors(getRandomMaterialColor("400"));
            actionMode = null;
            recyclerView.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.resetAnimationIndex();
                }
            });
        }
    }

    class JsoupListView extends AsyncTask<Void, String, ArrayList<Message>> {

        String url;
        String[] arrURL;

        @Override
        protected ArrayList<Message> doInBackground(Void... params) {
            arrURL = getResources().getStringArray(R.array.url);
            url = arrURL[Prefs.getSERVER(getContext())];
            try {
                Document doc = Jsoup.connect(url).maxBodySize(0).get();
                ArrayList<Message> cl = new ArrayList<>();
                Message item;
                Elements table = doc.select("table[class=\"table table-striped table-hover\"]");
                for (Element myTable : table) {
                    Elements rows = myTable.select("tr");
                    for (int i = 1; i < rows.size(); i++) {
                        item = new Message();
                        Element row = rows.get(i);
                        Elements cols = row.select("td");
                        Elements href = row.select("a");
                        String strhref = href.attr("href");
                        item.setFrom(cols.get(2).text());
                        item.setTimestamp(cols.get(3).text());
                        item.setSubject(cols.get(4).text());
                        item.setLink(strhref);
                        String FixFileName = cols.get(2).text();
                        FixFileName = FixFileName.replaceAll("/", "");
                        File existCirculars = new File(_Path + FixFileName);
                        File existCircularsPdf = new File(_Path + FixFileName + ".pdf");
                        if (existCirculars.exists() || existCircularsPdf.exists()) {
                            item.setMessage(getString(R.string.downloaded_Message));
                            item.setRead(true);
                        }
                        if (strhref.contains("fileLoader"))
                            cl.add(item);
                    }
                    mLayoutManager.smoothScrollToPosition(recyclerView, null, 0);
                }
                Collections.sort(cl, new Comparator<Message>() {
                    public int compare(Message o1, Message o2) {
                        return o2.getTimestamp().compareTo(o1.getTimestamp());
                    }
                });

                return cl;
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException ils) {
                Log.d("IllegalStateException", ils.getMessage());
            } catch (RuntimeException x) {
                Log.d("RuntimeException", x.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Message> ts) {
            super.onPostExecute(ts);
            try {
                showShimmerEffect(false);
                swipeRefreshLayout.setColorSchemeColors(getRandomMaterialColor("400"));
                messages.clear();

                for (Message message : ts) {
                    message.setColor(getRandomMaterialColor("400"));
                    messages.add(message);
                }
                if (messages.isEmpty())
                    txtNonItem.setVisibility(View.VISIBLE);
                else
                    txtNonItem.setVisibility(View.GONE);

                mAdapter.notifyDataSetChanged();
                onQueryTextChange("");
            } catch (Exception ex) {

            }
        }
    }
}

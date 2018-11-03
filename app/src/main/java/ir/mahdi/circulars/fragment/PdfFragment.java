package ir.mahdi.circulars.fragment;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;

import ir.mahdi.circulars.R;

public class PdfFragment extends Fragment {

    PDFView pdfView;

    public PdfFragment() {
        // Required empty public constructor
    }

    public static PdfFragment newInstance(String param1, String param2) {
        PdfFragment fragment = new PdfFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pdf, container, false);

        try {
            Bundle bundle = getArguments();
            String filename = bundle.getString("FILE_NAME");
            pdfView = view.findViewById(R.id.pdfView);
            File pdf = new File(filename);
            pdfView.fromFile(pdf)
                    .enableDoubletap(true)
                    .enableSwipe(true)
                    .load();
        } catch (Exception ex) {
            Snackbar.make(getActivity().getWindow().getDecorView(), "فایل خراب است", Snackbar.LENGTH_LONG).show();
        }
        return view;
    }
}

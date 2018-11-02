package ir.mahdi.circulars.fragment;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import ir.mahdi.circulars.R;

public class ImageFragment extends Fragment {
    public ImageFragment() {
        // Required empty public constructor
    }

    public static ImageFragment newInstance(String param1, String param2) {
        ImageFragment fragment = new ImageFragment();
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
        View view = inflater.inflate(R.layout.fragment_image, container, false);

        ImageView imageView = view.findViewById(R.id.imgActivity);

        try {

            Bundle bundle=getArguments();
            String filename=bundle.getString("FILE_NAME");

            Glide.with(this).load(filename).into(imageView);
        } catch (Exception ex) {
            Snackbar.make(getActivity().getWindow().getDecorView(), "فایل خراب است", Snackbar.LENGTH_LONG).show();
        }
        return view;
    }
}

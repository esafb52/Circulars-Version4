package ir.mahdi.circulars.fragment;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import org.apache.commons.io.FilenameUtils;
import org.beyka.tiffbitmapfactory.TiffBitmapFactory;

import java.io.File;

import ir.mahdi.circulars.R;

public class ImageFragment extends Fragment {

    int tifPages = 0;

    public ImageFragment() {
        // Required empty public constructor
    }

    public static ImageFragment newInstance() {
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

        final ImageView imageView = view.findViewById(R.id.imgActivity);

        FloatingActionButton fabNext = view.findViewById(R.id.fabNext);
        final FloatingActionButton fabPrev = view.findViewById(R.id.fabPrev);
        final TextView textView = view.findViewById(R.id.tifViewNumber);

        try {

            Bundle bundle = getArguments();
            String filename = bundle.getString("FILE_NAME");


            String ext = FilenameUtils.getExtension(filename);
            if (ext.contains("tif")) {

                final File file = new File(filename);

                final TiffBitmapFactory.Options options = new TiffBitmapFactory.Options();
//                options.inJustDecodeBounds = true;
                TiffBitmapFactory.decodeFile(file, options);

                final int dirCount = options.outDirectoryCount;

                fabNext.show();
                fabPrev.show();
                textView.setVisibility(View.VISIBLE);
                textView.setText(tifPages + " از " + dirCount);

                Bitmap bmp = TiffBitmapFactory.decodeFile(file, options);
                Glide.with(this).load(bmp).into(imageView);

                fabNext.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (tifPages < dirCount) {
                            tifPages++;
                            options.inDirectoryNumber = tifPages;

                            Bitmap bmp = TiffBitmapFactory.decodeFile(file, options);
                            Glide.with(getContext()).load(bmp).into(imageView);
                            textView.setText(tifPages + " از " + dirCount);
                        }
                    }
                });

                fabPrev.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (tifPages > 0) {
                            tifPages--;
                            options.inDirectoryNumber = tifPages;
                            Bitmap bmp = TiffBitmapFactory.decodeFile(file, options);
                            Glide.with(getContext()).load(bmp).into(imageView);
                            textView.setText(tifPages + " از " + dirCount);
                        }
                    }
                });

            } else {
                Glide.with(this).load(filename).into(imageView);
            }

        } catch (Exception ex) {
            Snackbar.make(getActivity().getWindow().getDecorView(), "فایل خراب است", Snackbar.LENGTH_LONG).show();
        }
        return view;
    }
}

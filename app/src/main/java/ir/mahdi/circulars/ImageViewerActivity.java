package ir.mahdi.circulars;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class ImageViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.imageviewer);
        ImageView imageView = findViewById(R.id.imgActivity);

        if (getWindow().getDecorView().getLayoutDirection() == View.LAYOUT_DIRECTION_LTR) {
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
        try {

            String filename;
            Bundle extra = getIntent().getExtras();
            filename = extra.getString("FILE_NAME");
            Glide.with(this).load(filename).into(imageView);
        } catch (Exception ex) {
            Snackbar.make(getWindow().getDecorView(), "فایل خراب است", Snackbar.LENGTH_LONG).show();
        }

    }
}

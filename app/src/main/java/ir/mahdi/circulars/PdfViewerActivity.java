package ir.mahdi.circulars;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.github.barteksc.pdfviewer.PDFView;

import java.io.File;

public class PdfViewerActivity extends AppCompatActivity {

    PDFView pdfView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pdfviewer);

        if (getWindow().getDecorView().getLayoutDirection() == View.LAYOUT_DIRECTION_LTR){
            getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        }
        try {
            String filename;
            Bundle extra = getIntent().getExtras();
            filename = extra.getString("FILE_NAME");
            pdfView = findViewById(R.id.pdfView);
            File pdf = new File(filename);
            pdfView.fromFile(pdf)
                    .enableDoubletap(true)
                    .enableSwipe(true)
                    .load();
        } catch (Exception ex) {
            Snackbar.make(getWindow().getDecorView(), "فایل خراب است", Snackbar.LENGTH_LONG).show();
        }

    }
}

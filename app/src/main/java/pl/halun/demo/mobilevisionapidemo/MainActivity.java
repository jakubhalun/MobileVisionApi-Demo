package pl.halun.demo.mobilevisionapidemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private static final int FILE_SELECT_CODE = 0;
    private static final String TAG = "MobileVisionApiDemo";

    private ImageView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button).setOnClickListener(new HandleClick());
    }

    @Override
    protected void onResume() {
        super.onResume();
        view = findViewById(R.id.imageView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_SELECT_CODE && resultCode == RESULT_OK) {
            processFile(data.getData());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private class HandleClick implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            chooseFile();
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inMutable = true;
        }
    }

    private void chooseFile() {
        String[] mimeTypes = {"image/jpeg", "image/png"};
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")
                .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
                .addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(
                    Intent.createChooser(intent, "Select a file to upload"),
                    FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void processFile(Uri uri) {
        Bitmap originalBitmap = loadFile(uri);
        Paint rectPaint = preparePaint();
        Bitmap drawBitmap = prepareDrawableBitmap(originalBitmap);
        Canvas canvas = prepareCanvas(originalBitmap, drawBitmap);
        FaceDetector faceDetector = prepareOperationalFaceDetector();
        SparseArray<Face> faces = detectFaces(originalBitmap, faceDetector);
        drawFaces(canvas, rectPaint, faces, drawBitmap);
        releaseFaceDetector(faceDetector);
    }

    private Bitmap loadFile(Uri uri) {
        ParcelFileDescriptor parcelFD;
        try {
            parcelFD = getContentResolver().openFileDescriptor(uri, "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        FileDescriptor imageSource = parcelFD.getFileDescriptor();

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inMutable = true;
        return BitmapFactory.decodeFileDescriptor(imageSource, null, bitmapOptions);
    }

    private Paint preparePaint() {
        Paint rectPaint = new Paint();
        rectPaint.setStrokeWidth(5);
        rectPaint.setColor(Color.CYAN);
        rectPaint.setStyle(Paint.Style.STROKE);
        return rectPaint;
    }

    private Bitmap prepareDrawableBitmap(Bitmap bitmap) {
        return Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
    }

    private Canvas prepareCanvas(Bitmap defaultBitmap, Bitmap drawableBitmap) {
        Canvas canvas = new Canvas(drawableBitmap);
        canvas.drawBitmap(defaultBitmap, 0, 0, null);
        return canvas;
    }

    private FaceDetector prepareOperationalFaceDetector() {
        FaceDetector faceDetector = new FaceDetector.Builder(this)
                .setTrackingEnabled(false)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .build();
        if (!faceDetector.isOperational()) {
            Log.i(TAG, "Face detector not operational");
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
            }
            if (!faceDetector.isOperational()) {
                new AlertDialog.Builder(this)
                        .setMessage("Face Detector could not be set up on your device")
                        .show();
                throw new RuntimeException("FaceDetector unavailable");
            }
        }
        return faceDetector;
    }

    private SparseArray<Face> detectFaces(Bitmap bitmap, FaceDetector faceDetector) {
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        return faceDetector.detect(frame);
    }

    private void drawFaces(Canvas canvas, Paint rectPaint, SparseArray<Face> faces, Bitmap bitmap) {
        for (int i = 0; i < faces.size(); i++) { //SparseArray non-iterable
            Face face = faces.get(i);
            float left = face.getPosition().x;
            float top = face.getPosition().y;
            float right = left + face.getWidth();
            float bottom = top + face.getHeight();
            float cornerRadius = 2.0f;

            RectF rectF = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, rectPaint);

            for (Landmark landmark : face.getLandmarks()) {
                int x = (int) (landmark.getPosition().x);
                int y = (int) (landmark.getPosition().y);
                float radius = 10.0f;
                canvas.drawCircle(x, y, radius, rectPaint);
            }
        }
        view.setImageDrawable(new BitmapDrawable(getResources(), bitmap));
    }

    private void releaseFaceDetector(FaceDetector faceDetector) {
        faceDetector.release();
    }
}

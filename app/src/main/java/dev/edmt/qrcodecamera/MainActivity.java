package dev.edmt.qrcodecamera;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    SurfaceView cameraPreview;
    TextView txtResult;
    BarcodeDetector barcodeDetector;
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;
    static int found = 0;


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(cameraPreview.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreview = findViewById(R.id.cameraPreview);
        txtResult = findViewById(R.id.txtResult);

        barcodeDetector = new BarcodeDetector.Builder(this)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();
        cameraSource = new CameraSource
                .Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(1000, 1000)
                .build();

        //Add Event
        cameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    //Request permission
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA}, RequestCameraPermissionID);
                    return;
                }
                try {
                    cameraSource.start(cameraPreview.getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                cameraSource.stop();

            }
        });

        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                if (found != 0)
                    return;
                final SparseArray<Barcode> qrcodes = detections.getDetectedItems();
                if (qrcodes.size() != 0) {
                    txtResult.post(new Runnable() {
                        @Override
                        public void run() {
                            //Create vibrate
                            Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(700);
                            int state =  ShowDialog(qrcodes.valueAt(0).displayValue);
                            txtResult.setText("Last person was " + ((state == 2)? "Invalid": (state == 0)? "entering": "leaving"));
                        }
                    });
                }
            }
        });


    }


    public int ShowDialog(final String msg) {
        found = 1;
        final Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.result_dialog);
        TextView res = dialog.findViewById(R.id.res);
        TextView output = dialog.findViewById(R.id.output);
        output.setText(msg);
        HttpConnect http = new HttpConnect();
        String out = null;

        try {
            out = http.sendGet(msg);
        } catch (Exception e) {
            e.printStackTrace();
            out = "error"; //msg + " can enter";
        }

        String toast = "";
        int state = 0;
        RelativeLayout layout = dialog.findViewById(R.id.relative_l);
        if (out.charAt(1) == 'n') {
            layout.setBackgroundColor(Color.RED);
            toast = "INVALID";
            res.setText(toast);
            state = 2;
        } else {
            JSONArray array = null;
            try {
                array = new JSONArray(out);
                if (array.get(0).equals("e")) {
                    layout.setBackgroundColor(Color.GREEN);
                    toast = "ENTERING";
                    res.setText(toast);
                    state = 0;
                } else {
                    layout.setBackgroundColor(Color.BLUE);
                    toast = "LEAVING";
                    res.setText(toast);
                    state = 1;
                }
                output.setText(array.get(1) + "\n" + array.get(2));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        Button dial_btn = dialog.findViewById(R.id.button2);
        final String finalToast = toast;
        dial_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(MainActivity.this, finalToast, Toast.LENGTH_SHORT).show();
                found = 0;
            }
        });
        dialog.show();

        return state;
    }
}

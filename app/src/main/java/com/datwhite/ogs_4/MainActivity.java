package com.datwhite.ogs_4;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    enum Filters {
        DEFAULT,
        NEGATIVE,
        BLUR,
        CANNY,
        CONTRAST
    }

    private JavaCameraView javaCameraView;
    private Mat mRGBA, mRGBAT, src, dst, forNegative, gray, edges, cannyDst;

    private Button negativeButton;
    private Button blurButton;
    private Button cannyButton;
    private Button contrastButton;
    private Button cancelButton;

    private Filters selectedFilter = Filters.DEFAULT;

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(MainActivity.this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("onManagerConnected", "OpenCV loaded successfully");
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        negativeButton = (Button) findViewById(R.id.negativeButton);
        blurButton = (Button) findViewById(R.id.blurButton);
        cannyButton = (Button) findViewById(R.id.cannyButton);
        contrastButton = (Button) findViewById(R.id.contrastButton);
        cancelButton = (Button) findViewById(R.id.cancelButton);

        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedFilter = Filters.NEGATIVE;
            }
        });

        blurButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedFilter = Filters.BLUR;
            }
        });

        cannyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedFilter = Filters.CANNY;
            }
        });

        contrastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedFilter = Filters.CONTRAST;
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedFilter = Filters.DEFAULT;
            }
        });

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CAMERA},
                1);

        javaCameraView = (JavaCameraView) findViewById(R.id.my_camera_view);

        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.setCvCameraViewListener(MainActivity.this);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    javaCameraView.setCameraPermissionGranted();  // <------ THIS!!!
                }
                return;
            }
        }

    }


    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(javaCameraView);
    }

    private void initializeCamera(JavaCameraView javaCameraView, int activeCamera) {
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.setCameraIndex(activeCamera);
        javaCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBAT = new Mat();
        dst = new Mat();
        src = new Mat(height, width, CvType.CV_8UC4);
        forNegative = new Mat(src.rows(), src.cols(), src.type(), new Scalar(255, 255, 255));
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        Core.transpose(mRGBA, mRGBAT);
        Core.flip(mRGBAT, mRGBAT, 1);
        Imgproc.resize(mRGBAT, dst, mRGBA.size());
        mRGBA.release();
        mRGBAT.release();

        switch (selectedFilter) {
            case DEFAULT:
                return dst;
            case NEGATIVE:
                src = dst;
                Core.subtract(forNegative, src, dst);
                return dst;
            case BLUR:
                src = dst;
                Imgproc.GaussianBlur(src, dst, new Size(21, 21), 21, 21);
                return dst;
            case CANNY:
                src = dst;
                Mat gray = new Mat(src.rows(), src.cols(), src.type());
                Mat edges = new Mat(src.rows(), src.cols(), src.type());
                Mat cannyDst = new Mat(src.rows(), src.cols(), src.type(), new Scalar(0));
                Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGB2GRAY);
                Imgproc.blur(gray, edges, new Size(3, 3));
                Imgproc.Canny(edges, edges, 5, 5 * 3);
                src.copyTo(cannyDst, edges);
                cannyDst.copyTo(dst);
                gray.release();
                edges.release();
                cannyDst.release();
                return dst;
            case CONTRAST:
                src = dst;
                src.convertTo(dst, -1, 2, -100);
                return dst;
            default:
                return dst;
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (javaCameraView != null) {
            javaCameraView.disableView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (OpenCVLoader.initDebug()) {
            Log.d("Check", "OpenCv configured successfully");
            baseLoaderCallback.onManagerConnected(BaseLoaderCallback.SUCCESS);
        } else {
            Log.d("Check", "OpenCv doesn’t configured successfully");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }
    }
}
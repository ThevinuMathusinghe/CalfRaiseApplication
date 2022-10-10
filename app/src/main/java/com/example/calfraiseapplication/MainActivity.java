package com.example.calfraiseapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {
    private static MatOfPoint2f dst;
    //    String path;
//    Uri uri;
    Mat mat1,mat2,mat3;
    int n = 0;
    //    Bitmap myBitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(OpenCVLoader.initDebug()){
            Log.i("Check","OpenCV Loaded");

        }
    }
    public void openGallery(View v)
    {
        n = 0;
        Log.i("Check","1");
        String filename = "/DCIM/Camera/testvideo.avi";
        File sdDir = Environment.getExternalStorageDirectory();
        File file = new File(sdDir + filename /* what you want to load in SD card */);
        Log.i("Check",file.getAbsolutePath());

        if(file.exists())
            Log.i("Check","File Exists");
        try {
            InputStream inputStream = new FileInputStream(file.getAbsolutePath());
            FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream);
            AndroidFrameConverter converterToBitmap = new AndroidFrameConverter();
            OpenCVFrameConverter.ToMat converterToMat = new OpenCVFrameConverter.ToMat();
            Log.i("Check","1");
            grabber.start();
            Log.i("Check","Grabber Started");
            Log.i("Check", String.valueOf(grabber.getLengthInTime()));

            for(int frameCount =0; frameCount<7;frameCount++){
                Frame nthFrame = grabber.grabKeyFrame();
                Bitmap bitmap = converterToBitmap.convert(nthFrame);
                Mat mat = converterToMat.convertToOrgOpenCvCoreMat(nthFrame);

                final Mat processed = processImage(mat);
                markOuterContour(processed, mat);
                Log.i("Check","2");

                nthFrame = converterToMat.convert(mat);
                Bitmap myBitmap = converterToBitmap.convert(nthFrame);
                Log.i("Check","3");

                SaveImage(myBitmap);
                Log.i("Check","Image Saved");
            }
            Log.i("Check","All Saved");


        } catch (FileNotFoundException | FrameGrabber.Exception e) {
            Log.i("Check",e.getMessage());
            e.printStackTrace();
        }

    }
    public static Mat processImage(final Mat mat) {
        final Mat processed = new Mat(mat.height(), mat.width(), mat.type());
        // Blur an image using a Gaussian filter
        Imgproc.GaussianBlur(mat, processed, new Size(7, 7), 1);

        // Switch from RGB to GRAY
        Imgproc.cvtColor(processed, processed, Imgproc.COLOR_RGB2GRAY);

        // Find edges in an image using the Canny algorithm
        Imgproc.Canny(processed, processed, 200, 25);

        // Dilate an image by using a specific structuring element
        // https://en.wikipedia.org/wiki/Dilation_(morphology)
        Imgproc.dilate(processed, processed, new Mat(), new Point(-1, -1), 1);

        return processed;
    }
    public static void markOuterContour(final Mat processedImage,
                                        final Mat originalImage) {
        // Find contours of an image
        final List<MatOfPoint> allContours = new ArrayList<>();
        Imgproc.findContours(
                processedImage,
                allContours,
                new Mat(processedImage.size(), processedImage.type()),
                Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_NONE
        );
        // Filter out noise and display contour area value
        final List<MatOfPoint> filteredContours = allContours.stream()
                .filter(contour -> {
                    final double value = Imgproc.contourArea(contour);
                    final Rect rect = Imgproc.boundingRect(contour);

                    final boolean isNotNoise = value > 5000;

                    if (isNotNoise) {
                        if(value>= 3000) {
                            Imgproc.putText(
                                    originalImage,
                                    "Area: " + (int) value,
                                    new Point(rect.x + rect.width, rect.y + rect.height),
                                    2,
                                    0.5,
                                    new Scalar(124, 252, 0),
                                    1
                            );
                        }

                        dst = new MatOfPoint2f();
                        contour.convertTo(dst, CvType.CV_32F);
                        Imgproc.approxPolyDP(dst, dst, 0.02 * Imgproc.arcLength(dst, true), true);
                        if(dst.toArray().length >= 8){
                            Imgproc.putText (
                                    originalImage,
                                    "Points: " + dst.toArray().length,
                                    new Point(rect.x + rect.width, rect.y + rect.height + 15),
                                    2,
                                    0.5,
                                    new Scalar(124, 252, 0),
                                    1
                            );
                        }

                    }

                    return isNotNoise;
                }).collect(Collectors.toList());
        // Mark contours
        if(dst.toArray().length >= 4){
            Imgproc.drawContours(
                    originalImage,
                    filteredContours,
                    -1, // Negative value indicates that we want to draw all of contours
                    new Scalar(124, 252, 0), // Green color
                    1
            );
        }

    }
    private void SaveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        Random generator = new Random();
        String fname = "Image-"+ n +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ())
            file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        n++;
    }

}
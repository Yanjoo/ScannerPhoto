package com.example.cvexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class PhotoActivity<dragSrc> extends AppCompatActivity {
    static {
        System.loadLibrary("opencv_java4");
        System.loadLibrary("native-lib");
    }

    private float mScaleFactor = 1.0f;
    private ImageView mImageView;
    private Mat img_input;
    private Mat img_output;

    int imageWidth, imageHeight;
    int[][] srcQuad = new int[4][2];    // 사각형 영역
    int[][] dstQuad = new int[4][2];    // 변환 후 사각형 영역
    boolean[] dragSrc = {false, false, false, false};   // 각 원의 드래그 상태
    int oldx, oldy;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        // MainActivity에서 이미지 건네 받기
        byte[] byteArray = getIntent().getByteArrayExtra("image");
        Bitmap image = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
        img_input = new Mat();
        Utils.bitmapToMat(image, img_input);
        imageWidth = image.getWidth();
        imageHeight = image.getHeight();

        // 이미지 뷰 설정
        mImageView = findViewById(R.id.loadImageView);
        mImageView.setImageBitmap(image);
        mImageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                setImageViewTouchListener(motionEvent);
                return true;
            }
        });

        // 초기 사각형 좌표
        srcQuad[0][0] = srcQuad[0][1] = 30;
        srcQuad[1][0] = 30; srcQuad[1][1] = imageHeight-30;
        srcQuad[2][0] = imageWidth-30; srcQuad[2][1] = imageHeight-30;
        srcQuad[3][0] = imageWidth-30; srcQuad[3][1] = 30;

        // 사각형 그리기
        img_output = drawROI(img_input, srcQuad);
        setMatToImageView(img_output);

        // 변환 후 사각형 정보
        final int dw = 500;
        final int dh = Math.round(dw * 297 / 210);
        dstQuad[0][0] = 0; dstQuad[0][1] = 0;
        dstQuad[1][0] = 0; dstQuad[1][1] = dh-1;
        dstQuad[2][0] = dw-1; dstQuad[2][1] = dh-1;
        dstQuad[3][0] = dw-1; dstQuad[3][1] = 0;

        // 버튼 설정
        button = findViewById(R.id.selectButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MatOfPoint2f src = new MatOfPoint2f(
                        new Point(srcQuad[0][0], srcQuad[0][1]),
                        new Point(srcQuad[1][0], srcQuad[1][1]),
                        new Point(srcQuad[2][0], srcQuad[2][1]),
                        new Point(srcQuad[3][0], srcQuad[3][1])
                        );

                MatOfPoint2f dst = new MatOfPoint2f(
                        new Point(dstQuad[0][0], dstQuad[0][1]),
                        new Point(dstQuad[1][0], dstQuad[1][1]),
                        new Point(dstQuad[2][0], dstQuad[2][1]),
                        new Point(dstQuad[3][0], dstQuad[3][1])
                        );
                Mat warpMat = Imgproc.getPerspectiveTransform(src, dst);
                Mat destImg = new Mat();
                Imgproc.warpPerspective(img_input, destImg, warpMat, new Size(dw, dh), Imgproc.INTER_CUBIC);

                Bitmap bitmapOutput = Bitmap.createBitmap(destImg.cols(), destImg.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(destImg, bitmapOutput);
                mImageView.setImageBitmap(bitmapOutput);
            }
        });
    }

    // Mat 객체를 imageView에 설정한다.
    private void setMatToImageView(Mat img) {
        Bitmap bitmapOutput = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bitmapOutput);
        mImageView.setImageBitmap(bitmapOutput);
    }

    // 원과 사각형을 그린다
    private Mat drawROI(Mat src, int[][] corners) {
        Mat cpy = new Mat();
        src.copyTo(cpy);

        for (int i=0; i<4; i++)
            Imgproc.circle(cpy, new Point(corners[i][0], corners[i][1]), 15, new Scalar(0, 192, 192, 255), -1, Imgproc.LINE_AA);

        Imgproc.line(cpy, new Point(corners[0][0], corners[0][1]), new Point(corners[1][0], corners[1][1]), new Scalar(0, 128, 128, 255), 2, Imgproc.LINE_AA);
        Imgproc.line(cpy, new Point(corners[1][0], corners[1][1]), new Point(corners[2][0], corners[2][1]), new Scalar(0, 128, 128, 255), 2, Imgproc.LINE_AA);
        Imgproc.line(cpy, new Point(corners[2][0], corners[2][1]), new Point(corners[3][0], corners[3][1]), new Scalar(0, 128, 128, 255), 2, Imgproc.LINE_AA);
        Imgproc.line(cpy, new Point(corners[3][0], corners[3][1]), new Point(corners[0][0], corners[0][1]), new Scalar(0, 128, 128, 255), 2, Imgproc.LINE_AA);

        return cpy;
    }

    private void setImageViewTouchListener(MotionEvent motionEvent) {
        int x = (int) (motionEvent.getX());
        int y = (int) (motionEvent.getY());

        Log.d("Photo", "원 좌표 : " + x + " " + y);

        // 터치한 x, y 좌표를 이미지 뷰에 맞게 변환한다.
        x = (int) (x * imageWidth / mImageView.getWidth());
        y = (int) (y * imageHeight / mImageView.getHeight());
        Log.d("Photo", "변환후 : " + x + " " + y);

        if (x > img_input.cols() || y > img_input.rows()) return;

        Log.d("image ", "width " + mImageView.getWidth() + " height " + mImageView.getHeight());
        Log.d("input ", "cols " + img_input.cols() + " rows " + img_input.rows());

        // 터치 했을 때
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
            for (int i=0; i<4; i++) {
                int cx = srcQuad[i][0];
                int cy = srcQuad[i][1];

                if ((cx-x)*(cx-x)+(cy-y)*(cy-y) < 15*15) {
                    dragSrc[i] = true;
                    oldx = x;
                    oldy = y;
                    break;
                }
            }
        }

        // 터치 뗏을 때
        if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
            for (int i=0; i<4; i++)
                dragSrc[i] = false;
            for (int i=0; i<4; i++)
                Log.d("Photo ", i + " 번째 " + srcQuad[i][0] + " " + srcQuad[i][1]);
        }

        // 터치하고 이동 할 때
        if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
            for (int i=0; i<4; i++) {
                if (dragSrc[i]) {
                    int dx = x - oldx;
                    int dy = y - oldy;
                    srcQuad[i][0] += dx;
                    srcQuad[i][1] += dy;
                    img_output = drawROI(img_input, srcQuad);
                    setMatToImageView(img_output);
                    oldx = x; oldy = y;
                    break;
                }
            }
        }
    }
}


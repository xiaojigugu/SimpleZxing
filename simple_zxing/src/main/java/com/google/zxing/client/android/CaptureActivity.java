/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.client.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static com.google.zxing.client.android.CaptureActivityHandler.msg_restart_preview;


/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback {

    private static final String TAG = CaptureActivity.class.getSimpleName();
    public static final String AUTO_FOCUS="AUTO_FOCUS_INTERVAL_MS";
    public static final String DOUBLE_TAP_ZOOM="DOUBLE_TAP_ZOOM";
    public static final String VIBRATE="VIBRATE";
    public static final String BEEP="BEEP";
    public static final String RESULT_TYPE = "result_type";
    public static final String RESULT_STRING = "result_string";
    private static final String[] ZXING_URLS = {"http://zxing.appspot.com/scan", "zxing://scan/"};
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private ImageView ivTorch;
    private TextView statusView;
    private Result lastResult;
    private boolean hasSurface;
    private IntentSource source;
    private Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, ?> decodeHints;
    private String characterSet;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;
    private long AUTO_FOCUS_INTERVAL_MS;
    private final int AUTO_FOCUS_INTERVAL_MS_DEFAULT=500;
    private boolean vibrate;
    private boolean playBeep;

    ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.capture);

        hasSurface = false;
        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        ambientLightManager = new AmbientLightManager(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);


    }

    @Override
    protected void onResume() {
        super.onResume();
        // CameraManager must be initialized here, not in onCreate(). This is necessary because we don't
        // want to open the camera driver and measure the screen size if we're going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the wrong size and partially
        // off screen.
        Intent intent=getIntent();
        AUTO_FOCUS_INTERVAL_MS = intent.getIntExtra(AUTO_FOCUS,AUTO_FOCUS_INTERVAL_MS_DEFAULT);
        cameraManager = new CameraManager(getApplication(),AUTO_FOCUS_INTERVAL_MS);

        viewfinderView = findViewById(R.id.viewfinder_view);
        viewfinderView.setCameraManager(cameraManager);

        if (intent.getBooleanExtra(DOUBLE_TAP_ZOOM,false)){
            viewfinderView.setOnDounbleTapListener(new ViewfinderView.OnDounbleTapListener() {
                @Override
                public void onDouble(MotionEvent e) {
                    cameraManager.zoom();
                }
            });
        }
        statusView = findViewById(R.id.status_view);
        ivTorch = findViewById(R.id.ivTorch);
        ivTorch.setTag(false);
        ivTorch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean tag= (boolean) ivTorch.getTag();
                if (!tag){
                    cameraManager.setTorch(true);
                    ivTorch.setImageResource(R.mipmap.sanguangdeng_off);
                    ivTorch.setTag(true);
                }else {
                    cameraManager.setTorch(false);
                    ivTorch.setImageResource(R.mipmap.shanguangdeng_on);
                    ivTorch.setTag(false);
                }
            }
        });

        ImageView ivBack=findViewById(R.id.ivBack);
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    setResult(RESULT_CANCELED);
                    finish();
            }
        });

        handler = null;
        lastResult = null;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        resetStatusView();

        vibrate=intent.getBooleanExtra(VIBRATE,true);
        playBeep = intent.getBooleanExtra(BEEP,true);
        beepManager.updatePrefs(vibrate,playBeep);

        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();

        source = IntentSource.NONE;
        decodeFormats = null;
        characterSet = null;

        if (intent != null) {
            String action = intent.getAction();
            String dataString = intent.getDataString();

            if (Intents.Scan.ACTION.equals(action)) {
                // Scan the formats the intent requested, and return the result to the calling activity.
                source = IntentSource.NATIVE_APP_INTENT;
                decodeFormats = DecodeFormatManager.parseDecodeFormats(intent);
                decodeHints = DecodeHintManager.parseDecodeHints(intent);

                if (intent.hasExtra(Intents.Scan.WIDTH) && intent.hasExtra(Intents.Scan.HEIGHT)) {
                    int width = intent.getIntExtra(Intents.Scan.WIDTH, 0);
                    int height = intent.getIntExtra(Intents.Scan.HEIGHT, 0);
                    if (width > 0 && height > 0) {
                        cameraManager.setManualFramingRect(width, height);
                    }
                }

                if (intent.hasExtra(Intents.Scan.CAMERA_ID)) {
                    int cameraId = intent.getIntExtra(Intents.Scan.CAMERA_ID, -1);
                    if (cameraId >= 0) {
                        cameraManager.setManualCameraId(cameraId);
                    }
                }

                String customPromptMessage = intent.getStringExtra(Intents.Scan.PROMPT_MESSAGE);
                if (customPromptMessage != null) {
                    statusView.setText(customPromptMessage);
                }

            } else if (dataString != null &&
                    dataString.contains("http://www.google") &&
                    dataString.contains("/m/products/scan")) {

                // Scan only products and send the result to mobile Product Search.
                source = IntentSource.PRODUCT_SEARCH_LINK;
                decodeFormats = DecodeFormatManager.PRODUCT_FORMATS;

            } else if (isZXingURL(dataString)) {
                // Scan formats requested in query string (all formats if none specified).
                // If a return URL is specified, send the results there. Otherwise, handle it ourselves.
                source = IntentSource.ZXING_LINK;
                Uri inputUri = Uri.parse(dataString);
                decodeFormats = DecodeFormatManager.parseDecodeFormats(inputUri);
                // Allow a sub-set of the hints to be specified by the caller.
                decodeHints = DecodeHintManager.parseDecodeHints(inputUri);
            }
            characterSet = intent.getStringExtra(Intents.Scan.CHARACTER_SET);
        }

        SurfaceView surfaceView = findViewById(R.id.preview_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (hasSurface) {
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceHolder.addCallback(this);
        }
    }

    /**
     * get current window orientation
     * @return
     */
    private int getCurrentOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            switch (rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
            }
        } else {
            switch (rotation) {
                case Surface.ROTATION_0:
                case Surface.ROTATION_270:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                default:
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
            }
        }
    }

    private static boolean isZXingURL(String dataString) {
        if (dataString == null) {
            return false;
        }
        for (String url : ZXING_URLS) {
            if (dataString.startsWith(url)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        cameraManager.closeDriver();
        //historyManager = null; // Keep for onActivityResult
        if (!hasSurface) {
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        beepManager.close();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (source == IntentSource.NATIVE_APP_INTENT) {
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                }
                if ((source == IntentSource.NONE || source == IntentSource.ZXING_LINK) && lastResult != null) {
                    restartPreviewAfterDelay(0L);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // Handle these events so they don't launch the Camera app
                return true;
            // Use volume up/down to turn on light
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                cameraManager.setTorch(false);
                return true;
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraManager.setTorch(true);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (holder == null) {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // do nothing
    }

    /**
     * A valid barcode has been found, so give an indication of success and show the results.
     *
     * @param rawResult   The contents of the barcode.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param barcode     A greyscale bitmap of the camera data which was decoded.
     */
    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        inactivityTimer.onActivity();
        lastResult = rawResult;

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            beepManager.playBeepSoundAndVibrate();
            Log.i(TAG, "handleDecode(),scanResult:" + rawResult.getText());
            Intent intent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putInt(RESULT_TYPE, RESULT_OK);
            bundle.putString(RESULT_STRING, rawResult.getText());
            intent.putExtras(bundle);
            setResult(-1, intent);
            finish();
        }
    }

//    /**
//     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
//     *
//     * @param barcode     A bitmap of the captured image.
//     * @param scaleFactor amount by which thumbnail was scaled
//     * @param rawResult   The decoded results which contains the points to draw.
//     */
//    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
//        ResultPoint[] points = rawResult.getResultPoints();
//        if (points != null && points.length > 0) {
//            Canvas canvas = new Canvas(barcode);
//            Paint paint = new Paint();
//            paint.setColor(getResources().getColor(R.color.result_points));
//            if (points.length == 2) {
//                paint.setStrokeWidth(4.0f);
//                drawLine(canvas, paint, points[0], points[1], scaleFactor);
//            } else if (points.length == 4 &&
//                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
//                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
//                // Hacky special case -- draw two lines, for the barcode and metadata
//                drawLine(canvas, paint, points[0], points[1], scaleFactor);
//                drawLine(canvas, paint, points[2], points[3], scaleFactor);
//            } else {
//                paint.setStrokeWidth(10.0f);
//                for (ResultPoint point : points) {
//                    if (point != null) {
//                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
//                    }
//                }
//            }
//        }
//    }

//    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
//        if (a != null && b != null) {
//            canvas.drawLine(scaleFactor * a.getX(),
//                    scaleFactor * a.getY(),
//                    scaleFactor * b.getX(),
//                    scaleFactor * b.getY(),
//                    paint);
//        }
//    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, decodeFormats, decodeHints, characterSet, cameraManager);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(msg_restart_preview, delayMS);
        }
        resetStatusView();
    }

    private void resetStatusView() {
        statusView.setText(R.string.msg_default_status);
        statusView.setVisibility(View.VISIBLE);
        viewfinderView.setVisibility(View.VISIBLE);
        lastResult = null;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }
}

package com.breadwallet.presenter.activities.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.qrcode.QRCodeReaderView;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.util.CryptoUriParser;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.platform.tools.BRBitId;

import java.security.NoSuchAlgorithmException;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/29/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class ScanQRActivity extends BRActivity implements ActivityCompat.OnRequestPermissionsResultCallback, QRCodeReaderView.OnQRCodeReadListener {
    private static final String TAG = ScanQRActivity.class.getName();
    private ImageView cameraGuide;
    private TextView descriptionText;
    private long lastUpdated;
    private UIUpdateTask task;
    private boolean handlingCode;
    public static boolean appVisible = false;
    private static ScanQRActivity app;
    private static final int MY_PERMISSION_REQUEST_CAMERA = 56432;

    private MultiPartQrcode[] mQrArray = null;
    private String mData;

    private ViewGroup mainLayout;

    private QRCodeReaderView qrCodeReaderView;

    public static ScanQRActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        cameraGuide = (ImageView) findViewById(R.id.scan_guide);
        descriptionText = (TextView) findViewById(R.id.description_text);

        task = new UIUpdateTask();
        task.start();

        cameraGuide.setImageResource(R.drawable.cameraguide);
        cameraGuide.setVisibility(View.GONE);

        if (android.support.v4.app.ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            initQRCodeReaderView();
        } else {
//            requestCameraPermission();
            Log.e(TAG, "onCreate: Permissions needed? HUH?");
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cameraGuide.setVisibility(View.VISIBLE);
                SpringAnimator.showExpandCameraGuide(cameraGuide);
            }
        }, 400);

    }


    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        if (qrCodeReaderView != null) {
            qrCodeReaderView.startCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
        if (qrCodeReaderView != null) {
            qrCodeReaderView.stopCamera();
        }
        task.stopTask();
    }

    @Override
    public void onBackPressed() {
        overridePendingTransition(R.anim.fade_down, 0);
        super.onBackPressed();
    }

    private class UIUpdateTask extends Thread {
        public boolean running = true;

        @Override
        public void run() {
            super.run();
            while (running) {
                if (System.currentTimeMillis() - lastUpdated > 300) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            cameraGuide.setImageResource(R.drawable.cameraguide);
                            descriptionText.setText("");
                        }
                    });
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void stopTask() {
            running = false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != MY_PERMISSION_REQUEST_CAMERA) {
            return;
        }

        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//            Snackbar.make(mainLayout, "Camera permission was granted.", Snackbar.LENGTH_SHORT).show();
            initQRCodeReaderView();
        } else {
//            Snackbar.make(mainLayout, "Camera permission request was denied.", Snackbar.LENGTH_SHORT)
//                    .show();
        }
    }

    @Override
    public void onQRCodeRead(final String text, PointF[] points) {
        lastUpdated = System.currentTimeMillis();
        if (handlingCode) return;
        handlingCode = true;
        if (CryptoUriParser.isCryptoUrl(this, text)
                || BRBitId.isBitId(text)
                || text.contains("redpacket")
                || text.contains("elaphant")
                || text.contains("elsphant")
                || text.contains("https")
                || text.contains("http")
                || text.contains("MultiQrContent")) {
            Log.e(TAG, "onQRCodeRead: isCrypto");
            handleData(text);
        } else {
            Log.e(TAG, "onQRCodeRead: not a crypto url");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        cameraGuide.setImageResource(R.drawable.cameraguide_red);
                        lastUpdated = System.currentTimeMillis();
                        descriptionText.setText("Not a valid address or scheme" );
                    } finally {
                        handlingCode = false;
                    }
                }
            });

        }

    }

    private void handleData(final String text) {
        Log.d(TAG, "multiqr data: " + text);

        try {
            MultiPartQrcode part = new Gson().fromJson(text, MultiPartQrcode.class);
            if (mQrArray == null || mQrArray.length != part.total) {
                mQrArray = new MultiPartQrcode[part.total];
            }

            if (part.index < part.total && mQrArray[part.index] == null) {
                mQrArray[part.index] = part;
            }

            for (MultiPartQrcode qr : mQrArray) {
                if (qr == null) {
                    handlingCode = false;
                    return;
                }
            }

            StringBuilder qrText = new StringBuilder();
            for (MultiPartQrcode qr : mQrArray) {
                qrText.append(qr.data);
            }

            Log.d(TAG, "multiqr text: " + qrText);

            mData = qrText.toString();
            String md5str = UiUtils.getStringMd5(mData);
            Log.d(TAG, "multiqr md5 checksum: " + md5str);

            if (!md5str.equals(part.md5)) {
                Log.e(TAG, "multiqr code data md5 verify failed");
                handlingCode = false;
                return;
            }

        } catch (JsonSyntaxException ignored) {
            Log.i(TAG, "qr code text is not a json object");
            mData = text;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            handlingCode = false;
            return;
        } catch (JsonParseException e) {
            Log.i(TAG, "qr code text is not a json object");
            mData = text;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mQrArray = null;
                    cameraGuide.setImageResource(R.drawable.cameraguide);
                    descriptionText.setText("");
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", mData);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                } finally {
                    handlingCode = false;
                }

            }
        });
    }

    private void initQRCodeReaderView() {
        qrCodeReaderView = findViewById(R.id.qrdecoderview);
        qrCodeReaderView.setAutofocusInterval(500L);
        qrCodeReaderView.setOnQRCodeReadListener(this);
        qrCodeReaderView.setBackCamera();
        qrCodeReaderView.startCamera();
    }

    public static class MultiPartQrcode {
        public String name;
        public int total;
        public int index;
        public String data;
        public String md5;
    }
}
package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.util.StringUtil;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import rufus.lzstring4java.LZString;

public class MultiSignQrActivity extends BRActivity {

    private final String TAG = "MultiSignQrActivity";
    private ImageView mQRCodeIv;
    private Bitmap mBitmap = null;

    private String mTransaction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_sign_qr);

        Intent intent = getIntent();
        mTransaction = intent.getStringExtra("tx");

        initView();
        if (!StringUtil.isNullOrEmpty(mTransaction)) {
            fixView();
        }
    }

    private void initView() {
        mQRCodeIv = findViewById(R.id.multisign_qr_iv);
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        TextView shareQr = findViewById(R.id.multisign_qr_share_qr);
        TextView shareJson = findViewById(R.id.multisign_qr_share_json);
        TextView passOrSent = findViewById(R.id.multisign_pass_or_sent);
        TextView or = findViewById(R.id.multisign_qr_share_or);

        if (StringUtil.isNullOrEmpty(mTransaction)) {
            mQRCodeIv.setVisibility(View.INVISIBLE);
            shareQr.setVisibility(View.INVISIBLE);
            shareJson.setVisibility(View.INVISIBLE);
            or.setVisibility(View.INVISIBLE);
            passOrSent.setText(R.string.multisign_send_succeeded);
        } else {
            shareJson.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareJsonFile();
                }
            });

            shareQr.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareQrcode();
                }
            });
            passOrSent.setText(R.string.multisign_pass_next);
        }
    }

    private void fixView() {

        try {
            String url = "elaphant://multitx?tx=" + URLEncoder.encode(mTransaction, "utf-8");
            String utf16 = LZString.compressToUTF16(url);
            Log.d(TAG, "=== utf16 length: " + utf16.length());
            mBitmap = generateQR(this, url, mQRCodeIv);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public Bitmap generateQR(Context ctx, String bitcoinURL, ImageView qrcode) {
        if (qrcode == null || bitcoinURL == null || bitcoinURL.isEmpty()) return null;
        WindowManager manager = (WindowManager) ctx.getSystemService(Activity.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = (int) (smallerDimension * 0.45f);
        Bitmap bitmap = null;
        bitmap = QRUtils.encodeAsBitmap(bitcoinURL, smallerDimension);
        if (bitmap == null) return null;
        qrcode.setImageBitmap(bitmap);
        return bitmap;

    }

    private void shareQrcode() {
        try {
            File imagePath = new File(getCacheDir(), "images");
            imagePath.mkdirs();
            FileOutputStream stream = new FileOutputStream(imagePath + "/image.png");
            mBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            share("image.png");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shareJsonFile() {
        try {
            File imagePath = new File(getCacheDir(), "images");
            imagePath.mkdirs();

            FileOutputStream fOut = new FileOutputStream(imagePath + "/tx.json");
            OutputStreamWriter outWriter = new OutputStreamWriter(fOut);
            outWriter.write(mTransaction);
            outWriter.close();
            fOut.flush();
            fOut.close();

            share("tx.json");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void share(final String fileName) {
        File cachePath = new File(getCacheDir(), "images");
        File newFile = new File(cachePath, fileName);
        Uri contentUri = FileProvider.getUriForFile(this, "com.elastos.wallet.imageprovider", newFile);
        if (contentUri == null) return;

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (fileName.contains(".json")) {
            shareIntent.setDataAndType(contentUri, "text/plain");
        } else {
            shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
        }

        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        startActivity(Intent.createChooser(shareIntent, "Share"));
    }
}

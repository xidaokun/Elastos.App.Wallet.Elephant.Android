package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.camera.ScanQRActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.google.gson.Gson;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class MultiSignQrActivity extends BRActivity {

    private final String TAG = "MultiSignQrActivity";
    private ImageView mQRCodeIv;
    private Bitmap mBitmap = null;
    private ArrayList<Bitmap> mBitmaps;
    private TextView mTxidCopyTv;
    private BRButton mTxidCopyBtn;

    private String mTransaction;
    private String mTxid;

    private Handler mHandler = null;
    private int mIndex = 0;
    private int mTotal = 0;
    private final static int INTERVAL = 500;
    private static final String FILE_SUFFIX = ".elasign";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_sign_qr);

        Intent intent = getIntent();
        mTransaction = intent.getStringExtra("tx");
        mTxid = intent.getStringExtra("txid");

        initView();
        if (!StringUtil.isNullOrEmpty(mTransaction)) {
            fixView();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mHandler != null && mBitmaps.size() != 0) {
            changeQrcode();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mHandler != null) {
            mHandler.removeCallbacks(runnable);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler = null;
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
        mTxidCopyTv = findViewById(R.id.txid_tv);
        mTxidCopyBtn = findViewById(R.id.txid_copy_btn);

        TextView shareJson = findViewById(R.id.multisign_qr_share_json);
        TextView passOrSent = findViewById(R.id.multisign_pass_or_sent);

        if (!StringUtil.isNullOrEmpty(mTxid)) {
            mQRCodeIv.setVisibility(View.INVISIBLE);
            shareJson.setVisibility(View.INVISIBLE);
            mTxidCopyTv.setVisibility(View.VISIBLE);
            mTxidCopyBtn.setVisibility(View.VISIBLE);
            mTxidCopyTv.setText(String.format(getString(R.string.txid_copy_hint), mTxid));
            passOrSent.setText(R.string.multisign_send_succeeded);
        } else if (StringUtil.isNullOrEmpty(mTransaction)) {
            mQRCodeIv.setVisibility(View.INVISIBLE);
            shareJson.setVisibility(View.INVISIBLE);
            mTxidCopyTv.setVisibility(View.GONE);
            mTxidCopyBtn.setVisibility(View.GONE);
            passOrSent.setVisibility(View.INVISIBLE);
        } else {
            shareJson.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareJsonFile();
                }
            });

            passOrSent.setText(R.string.multisign_pass_next);
        }

        mTxidCopyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(mTxid);
            }
        });
    }

    private void copyToClipboard(String content) {
        if(!StringUtil.isNullOrEmpty(content)) {
            BRClipboardManager.putClipboard(MultiSignQrActivity.this, content);
            Toast.makeText(MultiSignQrActivity.this, getString(R.string.Receive_copied), Toast.LENGTH_SHORT).show();
        }
    }

    private String getUrl() throws UnsupportedEncodingException {
        return "elaphant://multitx?AppName=" + BRConstants.ELAPHANT_APP_NAME +
                "&AppID=" + BRConstants.ELAPHANT_APP_ID +
                "&PublicKey=" + BRConstants.ELAPHANT_APP_PUBLICKEY +
                "&DID=" + BRConstants.ELAPHANT_APP_DID +
                "&Tx=" + URLEncoder.encode(mTransaction, "utf-8");
    }

    private void fixView() {
        try {
            String url = getUrl();
            Log.d(TAG, "url: " + url);

            mTotal = (int) Math.ceil((double) url.length() / (double) 500);
            Log.d(TAG, "qrcount: " + mTotal);

            if (mTotal > 1) {
                mBitmaps = new ArrayList<>(mTotal);
                String md5str = UiUtils.getStringMd5(url);
                Log.d(TAG, "md5: " + md5str);
                for (int i = 0; i < mTotal; i++) {
                    ScanQRActivity.MultiPartQrcode qr = new ScanQRActivity.MultiPartQrcode();
                    qr.name = "MultiQrContent";
                    qr.total = mTotal;
                    qr.index = i;

                    int start = i * 500;
                    if (start > url.length()) break;
                    int end = (i + 1) * 500;
                    if (end > url.length()) end = url.length();
                    qr.data = url.substring(start, end);
                    qr.md5 = md5str;

                    String qrstr = new Gson().toJson(qr);
                    Log.d(TAG, "qrstr: " + qrstr);
                    Bitmap bitmap = QRUtils.generateQRBitmap(this, qrstr);
                    mBitmaps.add(i, bitmap);
                }
                mHandler = new Handler();
                mIndex = 0;
            } else {
                mBitmap = QRUtils.generateQRBitmap(this, url);
                mQRCodeIv.setImageBitmap(mBitmap);
            }

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void shareJsonFile() {
        try {
            File imagePath = new File(getCacheDir(), "images");
            imagePath.mkdirs();
            deleteTempFile(imagePath.getAbsolutePath());

            String fileName = "tx" + System.currentTimeMillis() + FILE_SUFFIX;
            FileOutputStream fOut = new FileOutputStream(imagePath + "/" + fileName);
            OutputStreamWriter outWriter = new OutputStreamWriter(fOut);
            outWriter.write(getUrl());
            outWriter.close();
            fOut.flush();
            fOut.close();

            share(fileName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void share(final String fileName) {
        File cachePath = new File(getCacheDir(), "images");

        File newFile = new File(cachePath, fileName);

        Uri contentUri;
        if (Build.VERSION.SDK_INT >= 24) {
            contentUri = FileProvider.getUriForFile(this, "elaphant.app.imageprovider", newFile);
        } else {
            contentUri = Uri.fromFile(newFile);
        }
        if (contentUri == null) return;

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (fileName.contains(FILE_SUFFIX)) {
            shareIntent.setDataAndType(contentUri, "text/plain");
        } else {
            shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
        }

        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        startActivity(Intent.createChooser(shareIntent, "Share"));
    }

    private void deleteTempFile(String folder) {
        File file = new File(folder);
        File[] fileList = file.listFiles();

        for (File temp : fileList) {
            if (temp.getName().endsWith(FILE_SUFFIX)) {
                temp.delete();
                Log.d(TAG, "delete " + temp.getAbsolutePath());
            }
        }
    }

    private void changeQrcode() {
        mQRCodeIv.setImageBitmap(mBitmaps.get(mIndex));
        mIndex++;
        if (mIndex >= mTotal) mIndex = 0;
        mHandler.postDelayed(runnable, INTERVAL);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            changeQrcode();
        }
    };
}

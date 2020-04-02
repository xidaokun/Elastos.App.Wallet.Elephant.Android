package com.breadwallet.presenter.fragments;

import android.content.Context;

import com.breadwallet.tools.util.StringUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MiniAppHelper {


    public static void copyCapsuleToDownloadCache(Context context, String fileOutputPath, String capsuleName) {
        if (StringUtil.isNullOrEmpty(fileOutputPath) || StringUtil.isNullOrEmpty(capsuleName))
            return;

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            if (!new File(fileOutputPath).exists()) new File(fileOutputPath).mkdirs();
            File capsuleFile = new File(fileOutputPath, capsuleName);
            if (capsuleFile.exists()) {
                capsuleFile.delete();
            }
            outputStream = new FileOutputStream(capsuleFile);
            inputStream = context.getAssets().open("apps/" + capsuleName);
            byte[] buffer = new byte[1024];
            int length = inputStream.read(buffer);
            while (length > 0) {
                outputStream.write(buffer, 0, length);
                length = inputStream.read(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.flush();
                inputStream.close();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}

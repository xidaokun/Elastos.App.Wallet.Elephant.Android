package com.breadwallet.presenter.fragments;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.MyAppItem;
import com.breadwallet.tools.adapter.ExploreAppsAdapter;
import com.breadwallet.tools.animation.SimpleItemTouchHelperCallback;
import com.breadwallet.tools.listeners.OnStartDragListener;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.StringUtil;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FragmentExplore extends Fragment implements OnStartDragListener {

    private static final String TAG = FragmentExplore.class.getSimpleName()+"_log";

    public static FragmentExplore newInstance(String text) {

        FragmentExplore f = new FragmentExplore();
        Bundle b = new Bundle();
        b.putString("text", text);

        f.setArguments(b);

        return f;
    }

    private RecyclerView mMyAppsRv;
    private ExploreAppsAdapter mAdapter;
    private View mDisclaimLayout;
    private ItemTouchHelper mItemTouchHelper;
    private View mDoneBtn;
    private View mCancelBtn;
    private View mAddBtn;
    private View mEditBtn;
    private View mOkBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explore_layout, container, false);
        initView(rootView);
        initListener();
        if(BRSharedPrefs.getDisclaimShow(getContext())) mDisclaimLayout.setVisibility(View.VISIBLE);
        return rootView;
    }


    private void initView(View rootView){
        mDisclaimLayout = rootView.findViewById(R.id.disclaim_layout);
        mOkBtn = rootView.findViewById(R.id.disclaim_ok_btn);
        mDoneBtn = rootView.findViewById(R.id.explore_done_tv);
        mCancelBtn = rootView.findViewById(R.id.explore_cancel_tv);
        mMyAppsRv = rootView.findViewById(R.id.app_list_rv);

        mMyAppsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mMyAppsRv.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mMyAppsRv);
    }

    private void initListener(){
        mDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadCapsule("https://xidaokun.github.io/vote.capsule");
            }
        });

        mOkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDisclaimLayout.setVisibility(View.GONE);
                BRSharedPrefs.setDisclaimshow(getContext(), false);
            }
        });
        mDisclaimLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    private List<MyAppItem> getAppItems(){
        File capsule = new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule");
        File[] files = capsule.listFiles();
        for(File file : files){
            if(file.isDirectory()){
                MyAppItem myAppItem = new MyAppItem();
                File jsonFile = new File(file, "app.json");
                File logoFile = new File(file, "banner/en/bannar1x.png");
                String json = getJsonFromCapsule(jsonFile);
                String logoPath = logoFile.getAbsolutePath();
                new Gson().fromJson(json, MyAppItem.class);

            }
        }
        return null;
    }

    private DownloadManager manager;
    private long downloadCapsule(String url){
        Log.d(TAG, "capsule url:"+url);
        if(StringUtil.isNullOrEmpty(url)) return -1;
        DownloadManager.Request request;
        try{
            Uri uri = Uri.parse(url);
            String fileName = uri.getLastPathSegment();
            request = new DownloadManager.Request(uri);
            request.setDestinationInExternalFilesDir(getContext(), Environment.DIRECTORY_DOWNLOADS, fileName+".zip");
            manager = (DownloadManager) getContext().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId =  manager.enqueue(request);
            registerDownloadReceiver();
            return downloadId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void registerDownloadReceiver(){
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

                decompression();
                deleteDownloadCapsule();
//                //TODO test
//                File appJsonPath = new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule/vote.capsule/app.json");
//                File appIconPath = new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule/vote.capsule/banner/en/bannar1x.png");
//                String tmp = getJsonFromCapsule(appJsonPath);
//                Bitmap tmp1 = getIconFromCapsule(appIconPath);

                logFile("capsule", new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule"));
                logFile("download", getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsoluteFile());
                logFile("vote.capsule", new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule/vote.capsule"));

                context.unregisterReceiver(this);
            }
        };
        getContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    private String getJsonFromCapsule(File filePath){
        FileInputStream inputStream;
        StringBuilder sb = new StringBuilder();
        try {
            inputStream = new FileInputStream(filePath);
            byte buffer[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0){
                sb.append(new String(buffer, 0, len));
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private Bitmap getIconFromCapsule(File filePath){
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        try {
//            FileInputStream inputStream = new FileInputStream(filePath);
//            byte buffer[] = new byte[1024];
//            int len;
//            while ((len=inputStream.read(buffer)) > 0) {
//                outputStream.write(buffer, 0, len);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        byte[] data = outputStream.toByteArray();
//        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        return BitmapFactory.decodeFile(filePath.getAbsolutePath());
    }

    private void decompression(){
        try {
            File file = getZipFile();
            if(null == file) return;
            File cacheFile = getContext().getExternalCacheDir().getAbsoluteFile();
            File capsuleFile = new File(cacheFile, "capsule");

            unZipFolder(file.getAbsolutePath(), capsuleFile.getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteDownloadCapsule(){
        File file = getZipFile();
        if(null != file) file.delete();
    }

    private File getZipFile(){
        File downloadFile = getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsoluteFile();
        if(downloadFile.exists()){
            File[] files = downloadFile.listFiles();
            if(files==null || files.length==0) return null;
            for(File file : files){
                String name = file.getName();
                if(!StringUtil.isNullOrEmpty(name) && name.contains("capsule")){
                    return file;
                }
            }
        }

        return null;
    }

    private void logFile(String flag, File path){
        File[] files = path.listFiles();
        for(File file : files){
            String name = file.getName();
            Log.d(TAG, flag+" fileName:"+name);
        }
    }

    public static void unZipFolder(String zipFileString, String outPathString) throws Exception {
        ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
        ZipEntry zipEntry;
        String  szName = "";
        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();
            if (zipEntry.isDirectory()) {
                szName = szName.substring(0, szName.length() - 1);
                File folder = new File(outPathString + File.separator + szName);
                folder.mkdirs();
            } else {
                Log.d(TAG,outPathString + File.separator + szName);
                File file = new File(outPathString + File.separator + szName);
                if (!file.exists()){
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                FileOutputStream out = new FileOutputStream(file);
                int len;
                byte[] buffer = new byte[1024];
                while ((len = inZip.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    out.flush();
                }
                out.close();
            }
        }
        inZip.close();
    }
}

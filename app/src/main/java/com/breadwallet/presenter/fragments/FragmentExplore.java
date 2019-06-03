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
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.MyAppItem;
import com.breadwallet.tools.adapter.ExploreAppsAdapter;
import com.breadwallet.tools.animation.SimpleItemTouchHelperCallback;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.listeners.OnStartDragListener;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FragmentExplore extends Fragment implements OnStartDragListener {

    private static final String TAG = FragmentExplore.class.getSimpleName() + "_log";

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
    private View mPopLayout;
    private ItemTouchHelper mItemTouchHelper;
    private View mDoneBtn;
    private View mCancelBtn;
    private View mAddBtn;
    private View mEditBtn;
    private View mOkBtn;
    private View mEditPopView;
    private View mAddPopView;
    private View mAddUrlView;
    private View mAddScanView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explore_layout, container, false);
        initView(rootView);
        initAdapter();
        initListener();
        if (BRSharedPrefs.getDisclaimShow(getContext()))
            mDisclaimLayout.setVisibility(View.VISIBLE);
        return rootView;
    }

    private List<MyAppItem> mItems = new ArrayList<>();

    private void initView(View rootView) {
        mDisclaimLayout = rootView.findViewById(R.id.disclaim_layout);
        mPopLayout = rootView.findViewById(R.id.explore_pop_layout);
        mOkBtn = rootView.findViewById(R.id.disclaim_ok_btn);
        mDoneBtn = rootView.findViewById(R.id.explore_done_tv);
        mCancelBtn = rootView.findViewById(R.id.explore_cancel_tv);
        mAddBtn = rootView.findViewById(R.id.explore_add_tv);
        mEditBtn = rootView.findViewById(R.id.explore_edit_tv);
        mMyAppsRv = rootView.findViewById(R.id.app_list_rv);
        mEditPopView = rootView.findViewById(R.id.explore_edit_pop);
        mAddPopView = rootView.findViewById(R.id.explore_add_pop);
        mAddUrlView = rootView.findViewById(R.id.explore_url_pop);
        mAddScanView = rootView.findViewById(R.id.explore_scan_pop);
    }

    private void initAdapter(){
        mItems.clear();
        MyAppItem redPackageItem = new MyAppItem();
        redPackageItem.appId = BRConstants.REA_PACKAGE_ID;
        redPackageItem.name_zh_CN = "红包";
        redPackageItem.name_en = "red package";
        redPackageItem.url = "https://redpacket.elastos.org";
        redPackageItem.developer = "elastos";
        redPackageItem.shortDesc_zh_CN = "红包";
        redPackageItem.shortDesc_en = "red package";
        redPackageItem.longDesc_en = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        redPackageItem.longDesc_zh_CN = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        mItems.add(redPackageItem);

        MyAppItem dposVoteItem = new MyAppItem();
        dposVoteItem.appId = BRConstants.DPOS_VOTE_ID;
        dposVoteItem.name_zh_CN = "dpos投票";
        dposVoteItem.name_en = "dpos vote";
        dposVoteItem.url = "http://elaphant.net/";
        dposVoteItem.developer = "elastos";
        dposVoteItem.shortDesc_en = "dpos投票";
        dposVoteItem.shortDesc_zh_CN = "dpos vote";
        mItems.add(dposVoteItem);

        MyAppItem exchageItem = new MyAppItem();
        exchageItem.appId = BRConstants.EXCHANGE_ID;
        exchageItem.name_zh_CN = "兑换";
        exchageItem.name_en = "exchage";
        exchageItem.url = "http://swft.elabank.net";
        exchageItem.developer = "elastos";
        dposVoteItem.shortDesc_zh_CN = "兑换";
        dposVoteItem.shortDesc_en = "exchage";
        mItems.add(exchageItem);

        List<MyAppItem> items = getAppItems();
        if (items != null) {
            mItems.addAll(items);
        }

        mMyAppsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new ExploreAppsAdapter(getContext(), mItems);
        mAdapter.isDelete(false);
        mMyAppsRv.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mMyAppsRv);
    }

    private void initListener() {
        mAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopLayout.setVisibility(mAddPopView.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
                mAddPopView.setVisibility(mAddPopView.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
                mEditPopView.setVisibility(View.GONE);
            }
        });

        mEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopLayout.setVisibility(mEditPopView.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
                mEditPopView.setVisibility(mEditPopView.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
                mAddPopView.setVisibility(View.GONE);
            }
        });

        mEditPopView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditPopView.setVisibility(View.GONE);
                mPopLayout.setVisibility(View.GONE);
                changeView(true);
                mAdapter.isDelete(true);
                mAdapter.notifyDataSetChanged();
            }
        });

        mAddUrlView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopLayout.setVisibility(View.GONE);
                UiUtils.startAddAppsActivity(getActivity(), BRConstants.ADD_APP_URL_REQUEST);
            }
        });

        mAddScanView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopLayout.setVisibility(View.GONE);
                UiUtils.openScanner(getActivity(), BRConstants.ADD_APP_URL_REQUEST);
            }
        });

        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeView(false);
            }
        });

        mDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                downloadCapsule("https://xidaokun.github.io/vote.capsule");
                changeView(false);
                mAdapter.isDelete(false);
                mAdapter.notifyDataSetChanged();
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
        mPopLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mPopLayout.setVisibility(View.GONE);
                mAddPopView.setVisibility(View.GONE);
                mEditPopView.setVisibility(View.GONE);
                return true;
            }
        });
    }

    private void changeView(boolean isEdit){
        mAddBtn.setVisibility(isEdit?View.GONE:View.VISIBLE);
        mEditBtn.setVisibility(isEdit?View.GONE:View.VISIBLE);
        mCancelBtn.setVisibility(isEdit?View.VISIBLE:View.GONE);
        mDoneBtn.setVisibility(isEdit?View.VISIBLE:View.GONE);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }


    private List<MyAppItem> getAppItems() {
        List<MyAppItem> items = new ArrayList<>();
        File capsule = new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule");
        File[] files = capsule.listFiles();
        if(null == files) return null;
        for (File file : files) {
            if (file.isDirectory()) {
                File jsonFile = new File(file, "app.json");
                String json = getJsonFromCapsule(jsonFile);
                MyAppItem item = new Gson().fromJson(json, MyAppItem.class);
                if(item != null){
                    items.add(item);
                }
            }
        }
        return items;
    }

    private DownloadManager manager;
    private String mDoloadFileName;
    public long downloadCapsule(String url) {
        Log.d(TAG, "capsule url:" + url);
        if (StringUtil.isNullOrEmpty(url)) return -1;
        DownloadManager.Request request;
        try {
            Uri uri = Uri.parse(url);
            mDoloadFileName = uri.getLastPathSegment();
            if (StringUtil.isNullOrEmpty(mDoloadFileName)) return -1;
            request = new DownloadManager.Request(uri);
            request.setDestinationInExternalFilesDir(getContext(), Environment.DIRECTORY_DOWNLOADS, mDoloadFileName + ".zip");
            manager = (DownloadManager) getContext().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = manager.enqueue(request);
            registerDownloadReceiver();
            Toast.makeText(getContext(), "开始下载", Toast.LENGTH_SHORT).show();
            return downloadId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void registerDownloadReceiver() {
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
//                    logFile("download", getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsoluteFile());
                    File file = decompression();
                    Log.d(TAG, "unzip path:"+file.getAbsolutePath());
                    deleteDownloadCapsule();
                    if(file == null) return;
//                    logFile("capsule", file);

                    File appJsonPath = new File(file, "/app.json");
                    String json = getJsonFromCapsule(appJsonPath);
                    MyAppItem item = new Gson().fromJson(json, MyAppItem.class);
                    if (item != null) {
                        mItems.add(item);
                        mAdapter.notifyDataSetChanged();
                    }
                    Toast.makeText(getContext(), "下载完成", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                context.unregisterReceiver(this);
            }
        };
        getContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    private String getJsonFromCapsule(File filePath) {
        FileInputStream inputStream;
        StringBuilder sb = new StringBuilder();
        try {
            inputStream = new FileInputStream(filePath);
            byte buffer[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                sb.append(new String(buffer, 0, len));
            }
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private Bitmap getIconFromCapsule(File filePath) {
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

    private File decompression() {
        try {
            File file = getZipFile();
            if (null == file) return null;
            return unZipFolder(file.getAbsolutePath(), new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule").getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void deleteDownloadCapsule() {
        File file = getZipFile();
        if (null != file) file.delete();
    }

    private File getZipFile() {
        try {
            File downloadFile = getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsoluteFile();
            if (downloadFile.exists()) {
                File[] files = downloadFile.listFiles();
                if (files == null || files.length == 0) return null;
                for (File file : files) {
                    String name = file.getName();
                    if (!StringUtil.isNullOrEmpty(name) && name.contains(".capsule")) {
                        return file;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void logFile(String flag, File path) {
        Log.d(TAG, "<-----------------------------"+path.getAbsolutePath()+" start------------------------------>");
        File[] files = path.listFiles();
        if(files == null) return;
        for (File file : files) {
            String name = file.getName();
            Log.d(TAG, flag + " fileName:" + name);
        }
        Log.d(TAG, "<-----------------------------"+path.getAbsolutePath()+" end------------------------------>");
        Log.d(TAG, "\n\n");
    }

    public static File unZipFolder(String zipFileString, String outPathString) throws Exception {
        boolean isFirstName = true;
        ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
        ZipEntry zipEntry;
        String szName = "";
        String firstName = null;
        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();
            if(isFirstName){
                firstName = zipEntry.getName();
                isFirstName = false;
            }
            if (zipEntry.isDirectory()) {
                szName = szName.substring(0, szName.length() - 1);
                File folder = new File(outPathString + File.separator + szName);
                folder.mkdirs();
            } else {
                Log.d(TAG, outPathString + File.separator + szName);
                File file = new File(outPathString + File.separator + szName);
                if (!file.exists()) {
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

        return new File(outPathString + File.separator + firstName);
    }
}

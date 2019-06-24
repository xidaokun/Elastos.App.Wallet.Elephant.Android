package com.breadwallet.presenter.fragments;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.presenter.entities.MyAppItem;
import com.breadwallet.presenter.entities.RegisterChainData;
import com.breadwallet.presenter.entities.StringChainData;
import com.breadwallet.tools.adapter.ExploreAppsAdapter;
import com.breadwallet.tools.animation.ItemTouchHelperAdapter;
import com.breadwallet.tools.animation.SimpleItemTouchHelperCallback;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.listeners.OnStartDragListener;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.ProfileDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.Utility;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.elastos.sdk.wallet.BlockChainNode;
import org.elastos.sdk.wallet.Did;
import org.elastos.sdk.wallet.DidManager;
import org.elastos.sdk.wallet.Identity;
import org.elastos.sdk.wallet.IdentityManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FragmentExplore extends Fragment implements OnStartDragListener, ExploreAppsAdapter.OnDeleteClickListener,
        ExploreAppsAdapter.OnTouchMoveListener,
        ExploreAppsAdapter.OnAboutClickListener,
        ExploreAppsAdapter.OnItemClickListener {

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
    private View mAboutView;
    private View mAboutShareView;
    private View mRemoveDialogView;
    private View mCancelView;
    private View mRemoveView;
    private BaseTextView mAboutAboutView;
    private View mAboutCancelView;
    private View mLoadDialogView;
    private TextView mLoadHintTv;
    private View mLoadNoBtn;
    private View mLoadYesBtn;
    private LoadingDialog mLoadingDialog;
    private static final int INIT_APPS_MSG = 0x01;
    private static final int UPDATE_APPS_MSG = 0x02;
    private static final int UNREGISTER_RECEIVER = 0x03;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int flag = msg.what;
            switch (flag) {
                case INIT_APPS_MSG:
//                    List<MyAppItem> tmp = ProfileDataSource.getInstance(getContext()).getMyAppItems();
//                    mItems.addAll(tmp);
//                    mAdapter.notifyDataSetChanged();
                    break;
                case UPDATE_APPS_MSG:
                    ProfileDataSource.getInstance(getContext()).updateMyAppItem(mItems);
                    mAdapter.notifyDataSetChanged();
                    break;
                case UNREGISTER_RECEIVER:

                    break;
                default:
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explore_layout, container, false);
        initView(rootView);
        initAdapter();
        initListener();
        initDid();
        initApps();
        return rootView;
    }

    private List<String> mAppIds = new ArrayList<>();

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
        mAboutView = rootView.findViewById(R.id.explore_about_layout);
        mAboutShareView = rootView.findViewById(R.id.share_tv);
        mAboutAboutView = rootView.findViewById(R.id.about_tv);
        mAboutCancelView = rootView.findViewById(R.id.cancel_tv);
        mRemoveDialogView = rootView.findViewById(R.id.explore_remove_app_layout);
        mCancelView = rootView.findViewById(R.id.remove_mini_cancel);
        mRemoveView = rootView.findViewById(R.id.remove_mini_confirm);
        mLoadDialogView = rootView.findViewById(R.id.explore_load_app_layout);
        mLoadHintTv = rootView.findViewById(R.id.load_hint_tv);
        mLoadNoBtn = rootView.findViewById(R.id.load_mini_no);
        mLoadYesBtn = rootView.findViewById(R.id.load_mini_yes);
        if (BRSharedPrefs.getDisclaimShow(getContext()))
            mDisclaimLayout.setVisibility(View.VISIBLE);
        mLoadingDialog = new LoadingDialog(getContext(), R.style.progressDialog);
        mLoadingDialog.setCanceledOnTouchOutside(false);
    }

    public void initApps() {
        mAppIds.clear();
        mItems.clear();
        mRemoveApp.clear();
        List<MyAppItem> tmp = ProfileDataSource.getInstance(getContext()).getMyAppItems();
        if (tmp != null && tmp.size() > 0) { //database
            mItems.addAll(tmp);
            for (MyAppItem item : tmp) {
                mAppIds.add(item.appId);
            }
            mAdapter.notifyDataSetChanged();
        }
        resetAppsFromNet();
    }

    private void getInterApps() {
        String downloadFile = getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        StringChainData redPackageStatus = getAppStatus(BRConstants.REA_PACKAGE_ID);
        if (null == redPackageStatus ||
                StringUtil.isNullOrEmpty(redPackageStatus.value) ||
                redPackageStatus.value.equals("normal")) {
            copyCapsuleToDownloadCache(getContext(), downloadFile, "redpacket.capsule");
        }
        StringChainData exchangeStatus = getAppStatus(BRConstants.DPOS_VOTE_ID);
        if (null == exchangeStatus ||
                StringUtil.isNullOrEmpty(exchangeStatus.value) ||
                exchangeStatus.value.equals("normal")) {
            copyCapsuleToDownloadCache(getContext(), downloadFile, "vote.capsule");
        }
        StringChainData dposVoteStatus = getAppStatus(BRConstants.EXCHANGE_ID);
        if (null == dposVoteStatus ||
                StringUtil.isNullOrEmpty(dposVoteStatus.value) ||
                dposVoteStatus.value.equals("normal")) {
            copyCapsuleToDownloadCache(getContext(), downloadFile, "swft.capsule");
        }
    }

    private List<MyAppItem> mItems = new ArrayList<>();

    private void initAdapter() {
        mMyAppsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new ExploreAppsAdapter(getContext(), mItems);
        mAdapter.isDelete(false);
        mMyAppsRv.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new MySimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mMyAppsRv);
    }

    private static boolean mIsLongPressDragEnabled = false;

    private MyAppItem mAboutAppItem = null;

    @Override
    public void onAbout(MyAppItem item, int position) {
        mAboutView.setVisibility(View.VISIBLE);
        mAboutAppItem = item;
        mAboutAboutView.setText(String.format(getString(R.string.explore_pop_about), mAboutAppItem.name_en));
    }

    private String mLoadUrl = null;
    @Override
    public void onItemClick(MyAppItem item, int position) {
        String url = item.url;
        if (!StringUtil.isNullOrEmpty(url)) {
            mLoadDialogView.setVisibility(View.VISIBLE);
            mLoadUrl = url;
            mLoadHintTv.setText(Html.fromHtml(String.format(getString(R.string.esign_load_mini_app_hint), item.name_en)));
        }
    }

    public static class MySimpleItemTouchHelperCallback extends SimpleItemTouchHelperCallback {

        public MySimpleItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
            super(adapter);
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return mIsLongPressDragEnabled;
        }
    }

    private void initListener() {
        mAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopLayout.setVisibility(mAddPopView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                mAddPopView.setVisibility(mAddPopView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                mEditPopView.setVisibility(View.GONE);
            }
        });

        mEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPopLayout.setVisibility(mEditPopView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                mEditPopView.setVisibility(mEditPopView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
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
                mIsLongPressDragEnabled = true;
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
                mAdapter.isDelete(false);
                mIsLongPressDragEnabled = false;
                mAdapter.notifyDataSetChanged();
            }
        });

        mDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeView(false);
                mAdapter.isDelete(false);
                mIsLongPressDragEnabled = false;
                mHandler.sendEmptyMessage(UPDATE_APPS_MSG);
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

        mAboutShareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAboutView.setVisibility(View.GONE);
                if (null != mAboutAppItem) {
                    UiUtils.shareCapsule(getContext(), new File(mAboutAppItem.path));
                }
            }
        });

        mAboutAboutView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAboutView.setVisibility(View.GONE);
                UiUtils.startMiniAppAboutActivity(getContext(), mAboutAppItem.appId);
            }
        });

        mAboutCancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAboutView.setVisibility(View.GONE);
            }
        });

        mCancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRemoveDialogView.setVisibility(View.GONE);
            }
        });

        mRemoveView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRemoveDialogView.setVisibility(View.GONE);
                if (mRemoveApp.size() > 0) {
                    for (MyAppItem app : mRemoveApp) {
                        ProfileDataSource.getInstance(getContext()).deleteAppItem(app.appId);
                        upAppStatus(app.appId, "deleted");
                        deleteFile(new File(app.path));
                        mItems.remove(app);
                    }
                }
                mAdapter.notifyDataSetChanged();
                mRemoveApp.clear();
            }
        });

        mLoadNoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoadDialogView.setVisibility(View.GONE);
            }
        });

        mLoadYesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoadDialogView.setVisibility(View.GONE);
                UiUtils.startWebviewActivity(getActivity(), mLoadUrl);
                mLoadUrl = null;
            }
        });

        mAdapter.setOnDeleteClick(this);
        mAdapter.setOnMoveListener(this);
        mAdapter.setOnAboutClick(this);
        mAdapter.setOnItemClick(this);
    }

    private void changeView(boolean isEdit) {
        mAddBtn.setVisibility(isEdit ? View.GONE : View.VISIBLE);
        mEditBtn.setVisibility(isEdit ? View.GONE : View.VISIBLE);
        mCancelBtn.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        mDoneBtn.setVisibility(isEdit ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    private void resetAppsFromNet() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                //inter app
                getInterApps();

                //third app
                StringChainData miniApps = getMiniApps();
                if (null == miniApps || StringUtil.isNullOrEmpty(miniApps.value)) return;
                List<String> appIds = new Gson().fromJson(miniApps.value, new TypeToken<List<String>>() {
                }.getType());
                if (null == appIds) return;
                for (String appId : appIds) {
                    if (StringUtil.isNullOrEmpty(appId)) break;
                    if (appId.equals(BRConstants.REA_PACKAGE_ID) ||
                            appId.equals(BRConstants.DPOS_VOTE_ID) ||
                            appId.equals(BRConstants.EXCHANGE_ID)) break;
                    StringChainData appStatus = getAppStatus(appId);
                    if (null == appStatus ||
                            StringUtil.isNullOrEmpty(appStatus.value) ||
                            appStatus.value.equals("normal")) {
                        StringChainData appUrlEntity = getAppsUrl(appId);
                        if (null == appUrlEntity || StringUtil.isNullOrEmpty(appUrlEntity.value))
                            return;
                        downloadCapsule(appUrlEntity.value);
                    }
                }
            }
        });
    }

    private DownloadManager manager;
    private String mDoloadFileName;

    private void copyCapsuleToDownloadCache(Context context, String fileOutputPath, String capsuleName) {
        if (StringUtil.isNullOrEmpty(fileOutputPath) || StringUtil.isNullOrEmpty(capsuleName))
            return;

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            File capsuleFile = new File(fileOutputPath, capsuleName);
            if (capsuleFile.exists()) {
                capsuleFile.delete();
            }
            mDoloadFileName = capsuleName;
            outputStream = new FileOutputStream(capsuleFile);
            inputStream = context.getAssets().open("apps/" + capsuleName);
            byte[] buffer = new byte[1024];
            int length = inputStream.read(buffer);
            while (length > 0) {
                outputStream.write(buffer, 0, length);
                length = inputStream.read(buffer);
            }

            registerDownloadReceiver();
            Intent intent = new Intent();
            intent.setAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            getContext().sendBroadcast(intent);
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

    public void backupCapsule(File downloadFile, File fileOutputPath, String capsuleName) throws IOException {
        if(null==downloadFile
                || null==fileOutputPath
                || StringUtil.isNullOrEmpty(capsuleName)) return;
        if(!fileOutputPath.exists()) fileOutputPath.mkdirs();
        File backupFile = new File(fileOutputPath, capsuleName);
        if(backupFile.exists()) backupFile.delete();

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(downloadFile);
            outputStream = new FileOutputStream(backupFile);
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

    public long downloadCapsule(String url) {
        Log.d(TAG, "capsule url:" + url);
        if (StringUtil.isNullOrEmpty(url)) return -1;
        DownloadManager.Request request;
        try {
            Uri uri = Uri.parse(url);
            mDoloadFileName = uri.getLastPathSegment();
            if (StringUtil.isNullOrEmpty(mDoloadFileName)) return -1;
            //TODO
            File downloadFile = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsoluteFile(), mDoloadFileName);
            if (downloadFile.exists()) {
                downloadFile.delete();
            }

            request = new DownloadManager.Request(uri);
            request.setDestinationInExternalFilesDir(getContext(), Environment.DIRECTORY_DOWNLOADS, mDoloadFileName);
            manager = (DownloadManager) getContext().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = manager.enqueue(request);
            registerDownloadReceiver();
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
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            File downloadPath = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsoluteFile(), mDoloadFileName);
                            File outPath = new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule/" + mDoloadFileName);
                            File backupPath = new File(getContext().getExternalCacheDir().getAbsoluteFile(), "cbackup/");
                            decompression(downloadPath.getAbsolutePath(), outPath.getAbsolutePath());
                            backupCapsule(downloadPath, backupPath, mDoloadFileName);
//                            logFile("srcPath", getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsoluteFile());
//                            logFile("outPath", new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule").getAbsoluteFile());

                            File appJsonPath = new File(outPath, "/app.json");
                            String json = getJsonFromCapsule(appJsonPath);
                            MyAppItem item = new Gson().fromJson(json, MyAppItem.class);
                            item.path = new File(backupPath, mDoloadFileName).getAbsolutePath();

                            String hash = CryptoHelper.getShaChecksum(downloadPath.getAbsolutePath());
                            Log.d(TAG, "mDoloadFileName:"+mDoloadFileName+" hash:"+hash);

                            RegisterChainData appSetting = ProfileDataSource.getInstance(getContext()).getMiniAppSetting(item.did);
                            Log.d(TAG, "registerUrl:"+appSetting.url+" registerHash:"+appSetting.hash);

                            if (StringUtil.isNullOrEmpty(hash) ||
                                    null == appSetting ||
                                    StringUtil.isNullOrEmpty(appSetting.hash) ||
                                    !appSetting.hash.equals(hash)) {
                                Toast.makeText(getContext(), "Illegal capsule ", Toast.LENGTH_SHORT).show();
                                deleteFile(downloadPath);
                                deleteFile(outPath);
                                deleteFile(backupPath);
                                return;
                            }
                            Log.d(TAG, "capsule legitimacy");

                            if (item != null) {
                                for (String appId : mAppIds) {
                                    if (item.appId.equals(appId)) return;
                                }
                                mAppIds.add(item.appId);
                                mItems.add(item);
                                mHandler.sendEmptyMessage(UPDATE_APPS_MSG);

                                upAppStatus(item.appId, "normal");
                                upAppUrlData(item.appId, item.url);
                                upAppIds(mAppIds);
                            }
                            deleteFile(downloadPath);
                            deleteFile(outPath);
//                            logFile("srcPath", getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsoluteFile());
//                            logFile("outPath", new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule").getAbsoluteFile());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                context.unregisterReceiver(this);
            }
        };
        getContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    private void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                deleteFile(f);
            }
            file.delete();
        } else if (file.exists()) {
            file.delete();
        }
    }

    private List<MyAppItem> mRemoveApp = new ArrayList<>();

    @Override
    public void onDelete(MyAppItem item, int position) {
        String appId = item.appId;
        if (!StringUtil.isNullOrEmpty(appId)) {
            mRemoveApp.add(item);
            mRemoveDialogView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onMove(int from, int to) {
        Log.d(TAG, "from:" + from + " to:" + to);
        Collections.swap(mItems, from, to);
    }

    class KeyValue {
        public String Key;
        public String Value;
    }

    private String getKeyVale(String path, String value) {
        KeyValue key = new KeyValue();
        key.Key = path;
        key.Value = value;
        List<KeyValue> keys = new ArrayList<>();
        keys.add(key);

        return new Gson().toJson(keys, new TypeToken<List<KeyValue>>() {
        }.getType());
    }

    private void upAppUrlData(final String miniAppId, final String value) {
        if (StringUtil.isNullOrEmpty(miniAppId) || StringUtil.isNullOrEmpty(value)) return;
        String path = mDidStr + "/Apps/" + miniAppId;
        String data = getKeyVale(path, value);
        String info = mDid.signInfo(mSeed, data);
        ProfileDataSource.getInstance(getContext()).upchainSync(info);
    }

    private void upAppStatus(String miniAppId, String status) {
        if (StringUtil.isNullOrEmpty(mDidStr) || StringUtil.isNullOrEmpty(miniAppId)) return;
        String path =  mDidStr + "/Apps/" + BRConstants.ELAPHANT_APP_ID + "/MiniPrograms/" + miniAppId + "/Status";
//        String path = mDidStr + "/Apps/" + miniAppId + "/Status";
        String data = getKeyVale(path, status);
        String info = mDid.signInfo(mSeed, data);
        ProfileDataSource.getInstance(getContext()).upchainSync(info);
    }

    private void upAppIds(List<String> appIds) {
        String ids = new Gson().toJson(appIds);
        String path = mDidStr + "/Apps";
        String data = getKeyVale(path, ids);
        String info = mDid.signInfo(mSeed, data);
        ProfileDataSource.getInstance(getContext()).upchainSync(info);
    }

    private StringChainData getMiniApps() {
        String path = mDidStr + "/Apps";
        mDid.syncInfo();
        String appIds = mDid.getInfo(path);
        if (!StringUtil.isNullOrEmpty(appIds)) {
            return new Gson().fromJson(appIds, StringChainData.class);
        }
        return null;
    }

    private StringChainData getAppStatus(String miniAppId) {
        String path = mDidStr + "/Apps/" + BRConstants.ELAPHANT_APP_ID + "/MiniPrograms/" + miniAppId + "/Status";
//        String path = mDidStr + "/Apps/" + miniAppId + "/Status";
//        String path = "iXiqJ7brdiH18vrHiTo9WiAHh692xgXi5y/Apps/8816ed501878a9f4404f5926c4fb2df56239424e41da9c449b4db35e9a8b99d5f976a0537858d709511b5b41cf11c0e88be8778008eb5f918a6aa712ac20c421/MiniPrograms/317DD1D2188C459EB24EAEBC81932F6ADB305FF66F073AB1DC767869EF2B1A04273A8A875";
        mDid.syncInfo();
        String value = mDid.getInfo(path);
        if (!StringUtil.isNullOrEmpty(value)) {
            return new Gson().fromJson(value, StringChainData.class);
        }
        return null;
    }

    private StringChainData getAppsUrl(String miniAppId) {
        String path = mDidStr + "/Apps/" + miniAppId;
        mDid.syncInfo();
        String url = mDid.getInfo(path);
        if (!StringUtil.isNullOrEmpty(url)) {
            return new Gson().fromJson(url, StringChainData.class);
        }
        return null;
    }

    private String getMn() {
        byte[] phrase = null;
        try {
            phrase = BRKeyStore.getPhrase(getContext(), 0);
            if (phrase != null) {
                return new String(phrase);
            }
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Did mDid;
    private String mSeed;
    private String mPublicKey;
    private String mDidStr;

    private void initDid() {
        if (null == mDid || null == mPublicKey) {
            String mnemonic = getMn();
            if (StringUtil.isNullOrEmpty(mnemonic)) return;
            String language = Utility.detectLang(getContext(), mnemonic);
            if (StringUtil.isNullOrEmpty(language)) return;
            String words = Utility.getWords(getContext(), language + "-BIP39Words.txt");
            if (StringUtil.isNullOrEmpty(words)) return;
            mSeed = IdentityManager.getSeed(mnemonic, Utility.getLanguage(language), words, "");
            if (StringUtil.isNullOrEmpty(mSeed)) return;
            Identity identity = IdentityManager.createIdentity(getContext().getFilesDir().getAbsolutePath());
            DidManager didManager = identity.createDidManager(mSeed);
            BlockChainNode node = new BlockChainNode(ProfileDataSource.DID_URL);
            mDid = didManager.createDid(0);
            mDid.setNode(node);
            mPublicKey = Utility.getInstance(getContext()).getSinglePublicKey(mnemonic);
            mDidStr = Utility.getInstance(getContext()).getDid(mPublicKey);
        }
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

    private void decompression(String srcPath, String outPath) {
        try {
            unZipFolder(srcPath, outPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        Log.d(TAG, "<-----------------------------" + path.getAbsolutePath() + " start------------------------------>");
        File[] files = path.listFiles();
        if (files == null) return;
        for (File file : files) {
            String name = file.getName();
            Log.d(TAG, flag + " fileName:" + name);
        }
        Log.d(TAG, "<-----------------------------" + path.getAbsolutePath() + " end------------------------------>");
        Log.d(TAG, "\n\n");
    }

    public static void unZipFolder(String zipFileString, String outPathString) throws Exception {
        boolean isFirstName = true;
        ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
        ZipEntry zipEntry;
        String szName = "";
        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();
            if (isFirstName) {
                isFirstName = false;
            }
            if (zipEntry.isDirectory()) {
                szName = szName.substring(0, szName.length() - 1);
                File folder = new File(outPathString + File.separator + szName);
                folder.mkdirs();
            } else {
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
    }

    private void showDialog() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mLoadingDialog.isShowing()) {
                    mLoadingDialog.show();
                }
            }
        });
    }

    private void dialogDismiss() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mLoadingDialog.dismiss();
            }
        });
    }
}

package com.breadwallet.presenter.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
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
import org.wlf.filedownloader.DownloadFileInfo;
import org.wlf.filedownloader.FileDownloadConfiguration;
import org.wlf.filedownloader.FileDownloader;
import org.wlf.filedownloader.listener.OnDeleteDownloadFileListener;
import org.wlf.filedownloader.listener.OnFileDownloadStatusListener;
import org.wlf.filedownloader.listener.simple.OnSimpleFileDownloadStatusListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private View mMenuPopLayout;
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
    private View mAboutPopLayout;
    private View mAboutShareView;
    private View mRemoveAppLayout;
    private View mCancelView;
    private View mRemoveView;
    private View mLoadAppView;
    private BaseTextView mAboutAboutView;
    private View mAboutCancelView;
    private View mLoadAppLayout;
    private TextView mLoadHintTv;
    private View mLoadNoBtn;
    private View mLoadYesBtn;
    private Activity mParent;
    private LoadingDialog mLoadingDialog;
    private AboutShowListener mAboutShowListener;
    private static final int INIT_APPS_MSG = 0x01;
    private static final int UPDATE_APPS_MSG = 0x02;
    private static final int SHOW_LOADING = 0x03;
    private static final int DISMISS_LOADING = 0x04;
    private static final int TOAST_MESSAGE = 0x05;

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
                    Log.d(TAG, "handler UPDATE_APPS_MSG items size:" + mItems.size());
                    ProfileDataSource.getInstance(getContext()).updateMyAppItem(mItems);
                    mAdapter.notifyDataSetChanged();
                    break;
                case SHOW_LOADING:
                    if (!mLoadingDialog.isShowing()) {
                        mLoadingDialog.show();
                    }
                    break;
                case DISMISS_LOADING:
                    if (mLoadingDialog.isShowing()) {
                        mLoadingDialog.dismiss();
                    }
                    break;
                case TOAST_MESSAGE:
                    String message = (String) msg.obj;
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
        initDownloader();
        return rootView;
    }

    public static class UserAppInfo {
        public String appId;
        public String url;
    }

    private List<String> mAppIds = new ArrayList<>();

    private void initView(View rootView) {
        mDisclaimLayout = rootView.findViewById(R.id.disclaim_layout);
        mRemoveAppLayout = rootView.findViewById(R.id.explore_remove_app_layout);
        mLoadAppLayout = rootView.findViewById(R.id.explore_load_app_layout);
        mMenuPopLayout = rootView.findViewById(R.id.explore_menu_pop_layout);
        mAboutPopLayout = rootView.findViewById(R.id.explore_about_layout);

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
        mAboutShareView = rootView.findViewById(R.id.share_tv);
        mAboutAboutView = rootView.findViewById(R.id.about_tv);
        mAboutCancelView = rootView.findViewById(R.id.cancel_tv);
        mCancelView = rootView.findViewById(R.id.remove_mini_cancel);
        mRemoveView = rootView.findViewById(R.id.remove_mini_confirm);
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

//        getInterApps();

        BRSharedPrefs.putAddedAppId(getContext(), new Gson().toJson(mAppIds));
        List<MyAppItem> tmp = ProfileDataSource.getInstance(getContext()).getMyAppItems();
        if (tmp != null && tmp.size() > 0) { //database
            Log.d(TAG, "MyAppItems size:" + tmp.size());
            mItems.addAll(tmp);
            for (MyAppItem item : tmp) {
                mAppIds.add(item.appId);
                BRSharedPrefs.putAddedAppId(getContext(), new Gson().toJson(mAppIds));
            }
            mAdapter.notifyDataSetChanged();
        } else {
            boolean has = BRSharedPrefs.hasReset(getContext());
            if (has) return;
            Log.d(TAG, "MyAppItems size:0");
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    BRSharedPrefs.setHasReset(getContext(), true);
                    getInterApps();
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        initApps();
    }

    private synchronized void getInterApps() {
        try {
            showDialog();
            StringChainData redPackageStatus = getAppStatus(BRConstants.REA_PACKAGE_ID);
            if (null == redPackageStatus ||
                    StringUtil.isNullOrEmpty(redPackageStatus.value) ||
                    redPackageStatus.value.equals("normal")) {
                Log.d(TAG, "copy redpackage");
                mDoloadFileName = "redpacket.capsule";
                mDoloadUrl = "https://redpacket.elastos.org/redpacket.capsule";
                copyCapsuleToDownloadCache(getContext(), mDownloadDir, mDoloadFileName);
                refreshApps();
            }
            showDialog();
            StringChainData dposVoteStatus = getAppStatus(BRConstants.DPOS_VOTE_ID);
            if (null == dposVoteStatus ||
                    StringUtil.isNullOrEmpty(dposVoteStatus.value) ||
                    dposVoteStatus.value.equals("normal")) {
                Log.d(TAG, "copy dposvote");
                mDoloadFileName = "vote.capsule";
                mDoloadUrl = "http://elaphant.net/vote.capsule";
                copyCapsuleToDownloadCache(getContext(), mDownloadDir, mDoloadFileName);
                refreshApps();
            }
            showDialog();
            StringChainData elaNewsStatus = getAppStatus(BRConstants.ELA_NEWS_ID);
            if (null == elaNewsStatus ||
                    StringUtil.isNullOrEmpty(elaNewsStatus.value) ||
                    elaNewsStatus.value.equals("normal")) {
                Log.d(TAG, "copy elaNews");
                mDoloadFileName = "ELANews01.capsule";
                mDoloadUrl = "https://elanews.net/ELANews01.capsule";
                copyCapsuleToDownloadCache(getContext(), mDownloadDir, mDoloadFileName);
                refreshApps();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            dialogDismiss();
        }

//        StringChainData swftStatus = getAppStatus(BRConstants.EXCHANGE_ID);
//        if (null == swftStatus ||
//                StringUtil.isNullOrEmpty(swftStatus.value) ||
//                swftStatus.value.equals("normal")) {
//            mDoloadFileName = "swft.capsule";
//            mDoloadUrl = "http://swft.elabank.net/swft.capsule";
//            copyCapsuleToDownloadCache(getContext(), downloadFile, mDoloadFileName);
//        }
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
        mAboutPopLayout.setVisibility(View.VISIBLE);
        if (null != mAboutShowListener) mAboutShowListener.hide();
        mAboutAppItem = item;
        mAboutAboutView.setText(String.format(getString(R.string.explore_pop_about), mAboutAppItem.name_en));
    }

    private String mLoadUrl = null;
    private String mLoadAppId = null;

    @Override
    public void onItemClick(MyAppItem item, int position) {
        String url = item.url;
        if (!StringUtil.isNullOrEmpty(url)) {
            mLoadAppLayout.setVisibility(View.VISIBLE);
            mLoadUrl = url;
            mLoadAppId = item.appId;

            String languageCode = Locale.getDefault().getLanguage();
            if (!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")) {
                mLoadHintTv.setText(Html.fromHtml(String.format(getString(R.string.esign_load_mini_app_hint), item.name_zh_CN)));
            } else {
                mLoadHintTv.setText(Html.fromHtml(String.format(getString(R.string.esign_load_mini_app_hint), item.name_en)));
            }
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
                mMenuPopLayout.setVisibility(mAddPopView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                mAddPopView.setVisibility(mAddPopView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                mEditPopView.setVisibility(View.GONE);
            }
        });

        mEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuPopLayout.setVisibility(mEditPopView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                mEditPopView.setVisibility(mEditPopView.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                mAddPopView.setVisibility(View.GONE);
            }
        });

        mEditPopView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditPopView.setVisibility(View.GONE);
                mMenuPopLayout.setVisibility(View.GONE);
                changeView(true);
                mAdapter.isDelete(true);
                mIsLongPressDragEnabled = true;
                mAdapter.notifyDataSetChanged();
            }
        });

        mAddUrlView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuPopLayout.setVisibility(View.GONE);
                UiUtils.startAddAppsActivity(getActivity(), BRConstants.ADD_APP_URL_REQUEST);
            }
        });

        mAddScanView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuPopLayout.setVisibility(View.GONE);
                UiUtils.openScanner(getActivity(), BRConstants.ADD_APP_URL_REQUEST);
            }
        });

        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeView(false);
                mAdapter.isDelete(false);
                mIsLongPressDragEnabled = false;

                List<MyAppItem> tmp = ProfileDataSource.getInstance(getContext()).getMyAppItems();
                if (null != tmp && mItems.size() > 0) {
                    mItems.clear();
                    mItems.addAll(tmp);
                    mAdapter.notifyDataSetChanged();
                }
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
        mLoadAppLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mRemoveAppLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mAboutPopLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });
        mMenuPopLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mMenuPopLayout.setVisibility(View.GONE);
                mAddPopView.setVisibility(View.GONE);
                mEditPopView.setVisibility(View.GONE);
                return true;
            }
        });


        mAboutShareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAboutPopLayout.setVisibility(View.GONE);
                if (null != mAboutShowListener) mAboutShowListener.show();
                if (null != mAboutAppItem) {
                    UiUtils.shareCapsule(getContext(), mAboutAppItem.path);
                }
            }
        });

        mAboutAboutView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAboutPopLayout.setVisibility(View.GONE);
                if (null != mAboutShowListener) mAboutShowListener.show();
                UiUtils.startMiniAppAboutActivity(getContext(), mAboutAppItem.appId);
            }
        });

        mAboutCancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAboutPopLayout.setVisibility(View.GONE);
                if (null != mAboutShowListener) mAboutShowListener.show();
            }
        });

        mCancelView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRemoveAppLayout.setVisibility(View.GONE);
            }
        });

        mRemoveView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRemoveAppLayout.setVisibility(View.GONE);
                if (mRemoveApp.size() > 0) {
                    for (MyAppItem app : mRemoveApp) {
                        ProfileDataSource.getInstance(getContext()).deleteAppItem(app.appId);
                        upAppStatus(app.appId, "deleted");
                        deleteFile(new File(app.path));
                        mItems.remove(app);
                        mAppIds.remove(app.appId);
                        BRSharedPrefs.putAddedAppId(getContext(), new Gson().toJson(mAppIds));
                    }
                }
                mAdapter.notifyDataSetChanged();
                mRemoveApp.clear();
            }
        });

        mLoadNoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoadAppLayout.setVisibility(View.GONE);
            }
        });

        mLoadYesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoadAppLayout.setVisibility(View.GONE);
                if (!StringUtil.isNullOrEmpty(mLoadUrl)) {
                    if (mLoadUrl.contains("?")) {
                        mLoadUrl = mLoadUrl + "&browser=elaphant";
                    } else {
                        mLoadUrl = mLoadUrl + "?browser=elaphant";
                    }
                    UiUtils.startWebviewActivity(getActivity(), mLoadUrl, mLoadAppId);
                }
                mLoadUrl = null;
                mLoadAppId = null;
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

    private String mDoloadFileName;
    private String mDoloadUrl;

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

    public void handleExternalCapsule(String fileInputPath, String fileOutputPath) {
        if (StringUtil.isNullOrEmpty(fileOutputPath)) return;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            File capsuleFile = new File(fileOutputPath, "share.capsule");
            if (capsuleFile.exists()) {
                capsuleFile.delete();
            }
            outputStream = new FileOutputStream(capsuleFile);
            inputStream = new FileInputStream(new File(fileInputPath));
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

    public void backupCapsule(File downloadFile, File fileOutputPath, String capsuleName) throws IOException {
        if (null == downloadFile
                || null == fileOutputPath
                || StringUtil.isNullOrEmpty(capsuleName)) return;
        if (!fileOutputPath.exists()) fileOutputPath.mkdirs();
        File backupFile = new File(fileOutputPath, capsuleName);
        if (backupFile.exists()) backupFile.delete();

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

    private String mDownloadDir = null;

    private void initDownloader() {
        try {
            mDownloadDir = new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule_download").getAbsolutePath();
            FileDownloadConfiguration.Builder builder = new FileDownloadConfiguration.Builder(getContext());
            builder.configFileDownloadDir(mDownloadDir);
            builder.configDownloadTaskSize(3);
            builder.configRetryDownloadTimes(3);
            builder.configDebugMode(false);
            builder.configConnectTimeout(25000);
            FileDownloadConfiguration configuration = builder.build();
            FileDownloader.init(configuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private OnFileDownloadStatusListener mOnFileDownloadStatusListener = null;

    private void registerDownloadListener() {
        if (null == mOnFileDownloadStatusListener) {
            mOnFileDownloadStatusListener = new OnSimpleFileDownloadStatusListener() {
                @Override
                public void onFileDownloadStatusRetrying(DownloadFileInfo downloadFileInfo, int retryTimes) {
                    super.onFileDownloadStatusRetrying(downloadFileInfo, retryTimes);
                }

                @Override
                public void onFileDownloadStatusWaiting(DownloadFileInfo downloadFileInfo) {
                    super.onFileDownloadStatusWaiting(downloadFileInfo);
                }

                @Override
                public void onFileDownloadStatusPreparing(DownloadFileInfo downloadFileInfo) {
                    super.onFileDownloadStatusPreparing(downloadFileInfo);
                }

                @Override
                public void onFileDownloadStatusPrepared(DownloadFileInfo downloadFileInfo) {
                    super.onFileDownloadStatusPrepared(downloadFileInfo);
                }

                @Override
                public void onFileDownloadStatusDownloading(DownloadFileInfo downloadFileInfo, float downloadSpeed, long remainingTime) {
                    super.onFileDownloadStatusDownloading(downloadFileInfo, downloadSpeed, remainingTime);
                }

                @Override
                public void onFileDownloadStatusPaused(DownloadFileInfo downloadFileInfo) {
                    super.onFileDownloadStatusPaused(downloadFileInfo);
                }

                @Override
                public void onFileDownloadStatusCompleted(final DownloadFileInfo downloadFileInfo) {
                    super.onFileDownloadStatusCompleted(downloadFileInfo);
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            refreshApps();
                            deleteDownloadCapsule(downloadFileInfo.getUrl());
                            dialogDismiss();
                        }
                    });
                }

                @Override
                public void onFileDownloadStatusFailed(String url, DownloadFileInfo downloadFileInfo, FileDownloadStatusFailReason failReason) {
                    super.onFileDownloadStatusFailed(url, downloadFileInfo, failReason);
                    dialogDismiss();
                }
            };
            FileDownloader.registerDownloadStatusListener(mOnFileDownloadStatusListener);
        }
    }

    private void deleteDownloadCapsule(String url){
        if(StringUtil.isNullOrEmpty(url)) return;
        FileDownloader.delete(url, true, new OnDeleteDownloadFileListener() {
            @Override
            public void onDeleteDownloadFilePrepared(DownloadFileInfo downloadFileNeedDelete) {
                Log.d("test", "test");
            }

            @Override
            public void onDeleteDownloadFileSuccess(DownloadFileInfo downloadFileDeleted) {
                Log.d("test", "test");
            }

            @Override
            public void onDeleteDownloadFileFailed(DownloadFileInfo downloadFileInfo, DeleteDownloadFileFailReason failReason) {
                Log.d("test", "test");
            }
        });
    }

    private void unregisterDownloadListener() {
        FileDownloader.unregisterDownloadStatusListener(mOnFileDownloadStatusListener);
        mOnFileDownloadStatusListener = null;
    }

//    public void downloadCapsule(String url) {
//        Log.d(TAG, "capsule url:" + url);
//        if (StringUtil.isNullOrEmpty(url)) return;
//        Uri uri = Uri.parse(url);
//        mDoloadFileName = uri.getLastPathSegment();
//        mDoloadUrl = url;
//        if (StringUtil.isNullOrEmpty(mDoloadFileName)) return;
//        FileDownloader.start(url);
//        registerDownloadListener();
//        showDialog();
//    }

    public void downloadCapsule(final String url){
        if(StringUtil.isNullOrEmpty(url)) return;
        showDialog();
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try{
//            String path = Environment.getExternalStorageDirectory().getAbsolutePath();

                    final long startTime = System.currentTimeMillis();
                    Log.i("DOWNLOAD","startTime="+startTime);
                    String filename=url.substring(url.lastIndexOf("/") + 1);
                    mDoloadFileName = filename;
                    mDoloadUrl = url;
                    URL myURL = new URL(url);
                    URLConnection conn = myURL.openConnection();
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    int fileSize = conn.getContentLength();//根据响应获取文件大小
                    if (fileSize <= 0) throw new RuntimeException("无法获知文件大小 ");
                    if (is == null) throw new RuntimeException("stream is null");
                    File file1 = new File(mDownloadDir);
                    if(!file1.exists()){
                        file1.mkdirs();
                    }
                    File outputFile = new File(mDownloadDir+"/"+filename);
                    if(outputFile.exists()){
                        outputFile.delete();
                    }
                    //把数据存入路径+文件名
                    FileOutputStream fos = new FileOutputStream(outputFile.getAbsoluteFile());
                    byte buf[] = new byte[1024];
                    int downLoadFileSize = 0;
                    do{
                        //循环读取
                        int numread = is.read(buf);
                        if (numread == -1)
                        {
                            break;
                        }
                        fos.write(buf, 0, numread);
                        downLoadFileSize += numread;
                        //更新进度条
                    } while (true);

                    logFile("downloadPath", new File(mDownloadDir));
                    refreshApps();

                    Log.i("DOWNLOAD","download success");
                    Log.i("DOWNLOAD","totalTime="+ (System.currentTimeMillis() - startTime));

                    is.close();
                } catch (Exception ex) {
                    Log.e("DOWNLOAD", "error: " + ex.getMessage(), ex);
                } finally {
                    dialogDismiss();
                }
            }
        });
    }

    private void refreshApps() {
        try {
            File downloadPath = new File(mDownloadDir, mDoloadFileName);
            File outPath = new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule/" + mDoloadFileName);
            decompression(downloadPath.getAbsolutePath(), outPath.getAbsolutePath());

//            logFile(mDoloadFileName, outPath);

            File appJsonPath = findAppJsonPath(outPath);
            Log.d(TAG, "appJsonPath:" + appJsonPath);
            if (appJsonPath == null) return;
            String json = getJsonFromCapsule(appJsonPath);
            MyAppItem item = new Gson().fromJson(json, MyAppItem.class);
            if (item == null) return;

            item.path = /*new File(backupPath, mDoloadFileName).getAbsolutePath()*/mDoloadUrl;
            item.icon = new File(outPath, item.icon).getAbsolutePath();

            String hash = CryptoHelper.getShaChecksum(downloadPath.getAbsolutePath());
            Log.d(TAG, "mDoloadFileName:" + mDoloadFileName + " hash:" + hash);

//                            String key = "key=Dev/dopsvote.h5.app/Release/Web/1.0.0";
            String key = "Dev/" + item.name + "/Release/" + item.platform + "/" + item.version;
            RegisterChainData appSetting = ProfileDataSource.getInstance(getContext()).getMiniAppSetting(item.did, key);

            if (StringUtil.isNullOrEmpty(hash) ||
                    null == appSetting ||
                    StringUtil.isNullOrEmpty(appSetting.hash) ||
                    !appSetting.hash.equals(hash)) {
                messageToast("illegal file");
                deleteFile(downloadPath);
                deleteFile(outPath);
                deleteDownloadCapsule(mDoloadUrl);
//                                deleteFile(backupPath);
                return;
            }
            Log.d(TAG, "capsule legitimacy");

            if (item != null) {
                for (String appId : mAppIds) {
                    if (item.appId.equals(appId)) {
                        deleteFile(downloadPath);
                        deleteFile(outPath);
                        return;
                    }
                }
                mAppIds.add(item.appId);
                mItems.add(item);
                mHandler.sendEmptyMessage(UPDATE_APPS_MSG);

                BRSharedPrefs.putAddedAppId(getContext(), new Gson().toJson(mAppIds));
                upAppStatus(item.appId, "normal");
//                                upUserAppInfo(mAppIds);
            }
            deleteFile(downloadPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            mRemoveAppLayout.setVisibility(View.VISIBLE);
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

    private void upAppStatus(String miniAppId, String status) {
        if (StringUtil.isNullOrEmpty(mDidStr) || StringUtil.isNullOrEmpty(miniAppId)) return;
        String path = "/Apps/" + BRConstants.ELAPHANT_APP_ID + "/MiniPrograms/" + miniAppId + "/Status";
        String data = getKeyVale(path, status);
        String info = mDid.signInfo(mSeed, data);
        ProfileDataSource.getInstance(getContext()).upchainSync(info);
    }

    private StringChainData getAppStatus(String miniAppId) {
        String path = "/Apps/" + BRConstants.ELAPHANT_APP_ID + "/MiniPrograms/" + miniAppId + "/Status";
        if (mDid == null) return null;
        mDid.syncInfo();
        String value = mDid.getInfo(path);
        if (!StringUtil.isNullOrEmpty(value)) {
            return new Gson().fromJson(value, StringChainData.class);
        }
        return null;
    }

    private void upUserAppInfo(List<UserAppInfo> appIds) {
        String ids = new Gson().toJson(appIds);
        String path = mDidStr + "/Apps";
        String data = getKeyVale(path, ids);
        String info = mDid.signInfo(mSeed, data);
        ProfileDataSource.getInstance(getContext()).upchainSync(info);
    }

    private UserAppInfo getUserAppInfo() {
        String path = mDidStr + "/Apps";
        if (null == mDid) return null;
        mDid.syncInfo();
        String appIds = mDid.getInfo(path);
        if (!StringUtil.isNullOrEmpty(appIds)) {
            return new Gson().fromJson(appIds, UserAppInfo.class);
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

    private void decompression(String srcPath, String outPath) throws Exception {
        unZipFolder(srcPath, outPath);
    }

    private File findAppJsonPath(File path) {
        if (null == path) return null;
        File[] files = path.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                findAppJsonPath(file);
                Log.d(TAG, " findAppJsonPath directory name:" + file.getName());
            } else {
                String name = file.getName();
                if (name.equals("app.json")) {
                    Log.d(TAG, " findAppJsonPath file name:" + file.getAbsolutePath());
                    return file;
                }
            }
        }

        return null;
    }

    private void logFile(String flag, File path) {
        Log.d(TAG, "<-----------------------------" + path.getAbsolutePath() + " start------------------------------>");
        File[] files = path.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                logFile(file.getName(), file);
                Log.d(TAG, flag + " fileName:" + file.getAbsolutePath());
            } else {
                String name = file.getName();
                Log.d(TAG, flag + " fileName:" + file.getAbsolutePath());
            }
        }
        Log.d(TAG, "<-----------------------------" + path.getAbsolutePath() + " end------------------------------>");
        Log.d(TAG, "\n\n");
    }

    public static void unZipFolder(String zipFileString, String outPathString) throws Exception {
        ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
        ZipEntry zipEntry;
        String szName = "";
        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterDownloadListener();
    }

    private void showDialog() {
        mHandler.sendEmptyMessageDelayed(SHOW_LOADING, 50);
    }

    private void dialogDismiss() {
        mHandler.sendEmptyMessage(DISMISS_LOADING);
    }

    private void messageToast(final String message) {
        if (StringUtil.isNullOrEmpty(message)) return;
        Message msg = new Message();
        msg.what = TOAST_MESSAGE;
        msg.obj = message;
        mHandler.sendMessage(msg);
    }

    public void hideAboutView() {
        mAboutPopLayout.setVisibility(View.GONE);
    }

    public void setAboutShowListener(AboutShowListener listener) {
        this.mAboutShowListener = listener;
    }

    public interface AboutShowListener {
        void show();

        void hide();
    }
}

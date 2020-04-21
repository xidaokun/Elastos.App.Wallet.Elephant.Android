package com.breadwallet.presenter.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
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
import com.breadwallet.presenter.entities.StringChainData;
import com.breadwallet.tools.adapter.MiniAppsAdapter;
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
import com.breadwallet.tools.util.FileHelper;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.Utility;
import com.elastos.jni.utils.SchemeStringUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.eclipse.jetty.util.UrlEncoded;
import org.elastos.sdk.wallet.BlockChainNode;
import org.elastos.sdk.wallet.Did;
import org.elastos.sdk.wallet.DidManager;
import org.elastos.sdk.wallet.Identity;
import org.elastos.sdk.wallet.IdentityManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FragmentExplore extends Fragment implements OnStartDragListener, MiniAppsAdapter.OnDeleteClickListener,
        MiniAppsAdapter.OnTouchMoveListener,
        MiniAppsAdapter.OnAboutClickListener,
        MiniAppsAdapter.OnItemClickListener {

    private static final String TAG = FragmentExplore.class.getSimpleName() + "_log";

    public static FragmentExplore newInstance(String text) {

        FragmentExplore f = new FragmentExplore();
        Bundle b = new Bundle();
        b.putString("text", text);

        f.setArguments(b);

        return f;
    }

    private RecyclerView mMyAppsRv;
    private MiniAppsAdapter mAdapter;
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
    private TextView mRemoveHint;
    private View mCancelView;
    private View mRemoveView;
    private BaseTextView mAboutAboutView;
    private View mAboutCancelView;
    private LoadingDialog mLoadingDialog;
    private AboutShowListener mAboutShowListener;
    private static final int INIT_APPS_MSG = 0x01;
    private static final int UPDATE_APPS_MSG = 0x02;
    private static final int SHOW_LOADING = 0x03;
    private static final int DISMISS_LOADING = 0x04;
    private static final int TOAST_MESSAGE = 0x05;
    private static final int DOWNLOAD_FAILED = 0x06;
    private static final int REMOVE_MINI_APP = 0x07;

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
                    if (mActivity != null && !mActivity.isFinishing() && !mLoadingDialog.isShowing()) {
                        mLoadingDialog.show();
                    }
                    break;
                case DISMISS_LOADING:
                    if (mActivity != null && !mActivity.isFinishing() && mLoadingDialog.isShowing()) {
                        mLoadingDialog.dismiss();
                    }
                    break;
                case TOAST_MESSAGE:
                    String message = (String) msg.obj;
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    break;
                case DOWNLOAD_FAILED:
                    if (mLoadingDialog.isShowing()) {
                        mLoadingDialog.dismiss();
                    }
                    break;
                case REMOVE_MINI_APP:
                    for (MyAppItem app : mRemoveApp) {
                        ProfileDataSource.getInstance(getContext()).deleteAppItem(app.appId);
                        upAppStatus(app.appId, "deleted");
                        FileHelper.deleteFile(new File(app.path));
                    }
                    BRSharedPrefs.putAddedAppId(getContext(), new Gson().toJson(mAppIds));
                    break;
                default:
                    break;
            }
        }
    };

    private Activity mActivity;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mActivity = activity;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_explore_layout, container, false);
        initView(rootView);
        initAdapter();
        initListener();
        initDid();
        initDownloader();
        initApps();
        return rootView;
    }

    private List<String> mAppIds = new ArrayList<>();

    private void initView(View rootView) {
        mDisclaimLayout = rootView.findViewById(R.id.disclaim_layout);
        mRemoveAppLayout = rootView.findViewById(R.id.explore_remove_app_layout);
        mMenuPopLayout = rootView.findViewById(R.id.explore_menu_pop_layout);
        mAboutPopLayout = rootView.findViewById(R.id.explore_about_layout);

        mRemoveHint = rootView.findViewById(R.id.remove_hint_tv);
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
        if (BRSharedPrefs.getDisclaimShow(getContext()))
            mDisclaimLayout.setVisibility(View.VISIBLE);
        mLoadingDialog = new LoadingDialog(getContext(), R.style.progressDialog);
        mLoadingDialog.setCanceledOnTouchOutside(false);
    }

    public void initApps() {
        try {
            mAppIds.clear();
            mItems.clear();
            mRemoveApp.clear();

//            AssetManager assetManager = getContext().getApplicationContext().getAssets();
//            String[] apps = assetManager.list("apps");
            BRSharedPrefs.putAddedAppId(getContext(), new Gson().toJson(mAppIds));
            final List<MyAppItem> tmp = ProfileDataSource.getInstance(getContext()).getMyAppItems();
            if (tmp != null && tmp.size()>0) {
                mItems.addAll(tmp);
                for (MyAppItem item : tmp) {
                    mAppIds.add(item.appId);
                    BRSharedPrefs.putAddedAppId(getContext(), new Gson().toJson(mAppIds));
                }
                mAdapter.notifyDataSetChanged();

                boolean need = BRSharedPrefs.needAddApps(getContext());
                if(need) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                addVotemeApp();
                                addMiniAppsApp();
                                BRSharedPrefs.putNeedAddApps(getContext(), false);
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                dialogDismiss();
                            }
                        }
                    });
                }
            } else {
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        getInterApps(tmp);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void addVotemeApp() {
        showDialog();
        StringChainData votemeStatus = getAppStatus(BRConstants.VOTE_ME_ID);
        if (null == votemeStatus ||
                StringUtil.isNullOrEmpty(votemeStatus.value) ||
                votemeStatus.value.equals("normal")) {
            mDoloadFileName = "voteforme.capsule";
            mDoloadUrl = "https://voteforme.elaphant.net/voteforme.capsule ";
            MiniAppHelper.copyCapsuleToDownloadCache(getContext(), mDownloadDir.getAbsolutePath(), mDoloadFileName);
            refreshApps();
        }
    }

    private void addMiniAppsApp() {
        showDialog();
        StringChainData miniAppStatus = getAppStatus(BRConstants.MINI_APPS_ID);
        if (null == miniAppStatus ||
                StringUtil.isNullOrEmpty(miniAppStatus.value) ||
                miniAppStatus.value.equals("normal")) {
            mDoloadFileName = "miniapps.capsule";
            mDoloadUrl = "https://miniapps.elaphant.app/miniapps.capsule";
            MiniAppHelper.copyCapsuleToDownloadCache(getContext(), mDownloadDir.getAbsolutePath(), mDoloadFileName);
            refreshApps();
        }
    }

    private void addRedPackageApp() {
        showDialog();
        StringChainData redPackageStatus = getAppStatus(BRConstants.REA_PACKAGE_ID);
        if (null == redPackageStatus ||
                StringUtil.isNullOrEmpty(redPackageStatus.value) ||
                redPackageStatus.value.equals("normal")) {
            Log.d(TAG, "copy redpackage");
            mDoloadFileName = "redpacket.capsule";
            mDoloadUrl = "https://redpacket.elastos.org/redpacket.capsule";
            MiniAppHelper.copyCapsuleToDownloadCache(getContext(), mDownloadDir.getAbsolutePath(), mDoloadFileName);
            refreshApps();
        }
    }

    private void addDposVoteApp() {
        showDialog();
        StringChainData dposVoteStatus = getAppStatus(BRConstants.DPOS_VOTE_ID);
        if (null == dposVoteStatus ||
                StringUtil.isNullOrEmpty(dposVoteStatus.value) ||
                dposVoteStatus.value.equals("normal")) {
            Log.d(TAG, "copy dposvote");
            mDoloadFileName = "vote.capsule";
            mDoloadUrl = "https://dposvote.elaphant.app/vote.capsule";
            MiniAppHelper.copyCapsuleToDownloadCache(getContext(), mDownloadDir.getAbsolutePath(), mDoloadFileName);
            refreshApps();
        }
    }

    private void addElaNewsApp() {
        showDialog();
        StringChainData elaNewsStatus = getAppStatus(BRConstants.ELA_NEWS_ID);
        if (null == elaNewsStatus ||
                StringUtil.isNullOrEmpty(elaNewsStatus.value) ||
                elaNewsStatus.value.equals("normal")) {
            Log.d(TAG, "copy elaNews");
            mDoloadFileName = "ELANews01.capsule";
            mDoloadUrl = "https://elanews.net/ELANews01.capsule";
            MiniAppHelper.copyCapsuleToDownloadCache(getContext(), mDownloadDir.getAbsolutePath(), mDoloadFileName);
            refreshApps();
        }
    }

    private void addElappApp() {
        showDialog();
        StringChainData elaAppsStatus = getAppStatus(BRConstants.ELA_APPS_ID);
        if (null == elaAppsStatus ||
                StringUtil.isNullOrEmpty(elaAppsStatus.value) ||
                elaAppsStatus.value.equals("normal")) {
            Log.d(TAG, "copy elaApps");
            mDoloadFileName = "elapp.capsule";
            mDoloadUrl = "https://elaphant.app/elapp.capsule";
            MiniAppHelper.copyCapsuleToDownloadCache(getContext(), mDownloadDir.getAbsolutePath(), mDoloadFileName);
            refreshApps();
        }
    }

    private synchronized void getInterApps(List<MyAppItem> apps) {
        try {
            boolean hasVotemeApps = false;
            boolean hasMiniApps = false;
            boolean hasRedPackage = false;
            boolean hasDposVote = false;
            boolean hasElaNews = false;
            boolean hasElapp = false;

            for (MyAppItem item : apps) {
                if(item.appId.equals(BRConstants.VOTE_ME_ID)) hasVotemeApps = true;
                if(item.appId.equals(BRConstants.MINI_APPS_ID)) hasMiniApps = true;
                if (item.appId.equals(BRConstants.REA_PACKAGE_ID)) hasRedPackage = true;
                if (item.appId.equals(BRConstants.DPOS_VOTE_ID)) hasDposVote = true;
                if (item.appId.equals(BRConstants.ELA_NEWS_ID)) hasElaNews = true;
                if (item.appId.equals(BRConstants.ELA_APPS_ID)) hasElapp = true;
            }

            boolean isVotemeAppDelete = BRSharedPrefs.isVotemeDelete(getContext());
            boolean isMiniAppDelete = BRSharedPrefs.isMiniAppsDelete(getContext());
            boolean isRedPackageDelete = BRSharedPrefs.isRedPacketDelete(getContext());
            boolean isDposVoteDelete = BRSharedPrefs.isVoteDelete(getContext());
            boolean isElaNewsDelete = BRSharedPrefs.isElaNewsDelete(getContext());
            boolean isElappDelete = BRSharedPrefs.isElappDelete(getContext());

            if(!hasVotemeApps && !isVotemeAppDelete) addVotemeApp();

            if(!hasMiniApps && !isMiniAppDelete) addMiniAppsApp();

            if (!hasRedPackage && !isRedPackageDelete) addRedPackageApp();

            if (!hasDposVote && !isDposVoteDelete) addDposVoteApp();

            if (!hasElaNews && !isElaNewsDelete) addElaNewsApp();

            if (!hasElapp && !isElappDelete) addElappApp();

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
        mAdapter = new MiniAppsAdapter(getContext(), mItems);
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

        String languageCode = Locale.getDefault().getLanguage();
        if(!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")){
            mAboutAboutView.setText(String.format(getString(R.string.explore_pop_about), mAboutAppItem.name_zh_CN));
        } else {
            mAboutAboutView.setText(String.format(getString(R.string.explore_pop_about), mAboutAppItem.name_en));
        }
        if(StringUtil.isNullOrEmpty(mAboutAppItem.name_zh_CN) && StringUtil.isNullOrEmpty(mAboutAppItem.name_en))
            mAboutAboutView.setText(mAboutAppItem.name);
    }

    @Override
    public void onItemClick(MyAppItem item, int position) {
        String url = item.url;
        if (!StringUtil.isNullOrEmpty(url)) {
            if (url.contains("?")) {
                url = url + "&browser=elaphant";
            } else {
                url = url + "?browser=elaphant";
            }
            UiUtils.startWebviewActivity(getActivity(), url, item.appId);
//
//            String languageCode = Locale.getDefault().getLanguage();
//            if (!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")) {
//                mLoadHintTv.setText(Html.fromHtml(String.format(getString(R.string.esign_load_mini_app_hint), item.name_zh_CN)));
//            } else {
//                mLoadHintTv.setText(Html.fromHtml(String.format(getString(R.string.esign_load_mini_app_hint), item.name_en)));
//            }
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
                UiUtils.startAddAppsActivity(getActivity(), BRConstants.SCANNER_DID_OR_ADD_REQUEST);
            }
        });

        mAddScanView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMenuPopLayout.setVisibility(View.GONE);
                UiUtils.openScanner(getActivity(), BRConstants.SCANNER_DID_OR_ADD_REQUEST);
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
                mHandler.sendEmptyMessage(REMOVE_MINI_APP);
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
                    StringBuilder sb = new StringBuilder();

                    String languageCode = Locale.getDefault().getLanguage();
                    if(!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")){
                        sb.append(mAboutAppItem.name_zh_CN);
                    } else {
                        sb.append(mAboutAppItem.name_en);
                    }
                    sb.append(" ");
                    sb.append("https://launch.elaphant.app/?");
                    if(!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")){
                        sb.append("appName=").append(Uri.encode(mAboutAppItem.name_zh_CN)).append("&");
                        sb.append("appTitle=").append(Uri.encode(mAboutAppItem.name_zh_CN)).append("&");
                    } else {
                        sb.append("appName=").append(Uri.encode(mAboutAppItem.name_en)).append("&");
                        sb.append("appTitle=").append(Uri.encode(mAboutAppItem.name_en)).append("&");
                    }
                    sb.append("autoRedirect=").append("True").append("&");
                    sb.append("redirectURL=").append(Uri.encode(mAboutAppItem.path.trim().
                            replace("http:", "elaphant:")
                            .replace("https:", "elaphant:")));
                    UiUtils.shareCapsule(getContext(), sb.toString());
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
                        mItems.remove(app);
                        mAppIds.remove(app.appId);
                        mAdapter.notifyDataSetChanged();
                    }
                }
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



    private File mDownloadDir = null;
    private File mDownloadCacheDir = null;

    private void initDownloader() {
        try {
            mDownloadDir = new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule_download");
            if (!mDownloadDir.exists()) mDownloadDir.mkdirs();

            mDownloadCacheDir = new File(getContext().getExternalCacheDir().getAbsoluteFile(), "capsule");
            if (!mDownloadCacheDir.exists()) mDownloadCacheDir.mkdirs();
//            FileDownloadConfiguration.Builder builder = new FileDownloadConfiguration.Builder(getContext());
//            builder.configFileDownloadDir(mDownloadDir);
//            builder.configDownloadTaskSize(3);
//            builder.configRetryDownloadTimes(3);
//            builder.configDebugMode(false);
//            builder.configConnectTimeout(25000);
//            FileDownloadConfiguration configuration = builder.build();
//            FileDownloader.init(configuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void downloadCapsule(String url) {
        if (StringUtil.isNullOrEmpty(url)) {
            if (isAdded())
                Toast.makeText(getContext(), getString(R.string.mini_app_invalid_url), Toast.LENGTH_SHORT).show();
            return;
        }

        if (url.contains("elapp:")) {
            String[] tmp = url.split("elapp:");
            if (tmp != null && tmp.length == 2) {
                url = tmp[1].trim();
            }
        }

        if (SchemeStringUtils.isElaphantPrefix(url)) {
            downloadCapsule(SchemeStringUtils.replaceElsProtocol(url, "http"));
            downloadCapsule(SchemeStringUtils.replaceElsProtocol(url, "https"));
            return;
        }

        //open url if exit
        for(MyAppItem item : mItems) {
            if(url.trim().equals(item.path)) {
                UiUtils.startWebviewActivity(getActivity(), item.url, item.appId);
                return;
            }
        }

        boolean isValid = SchemeStringUtils.isHttpPrefix(url);
        if (!isValid) {
            if(isAdded())Toast.makeText(getContext(), getString(R.string.mini_app_invalid_url), Toast.LENGTH_SHORT).show();
            return;
        }

        String capsuleUrl = url;
        if (url.contains("elapp:")) {
            capsuleUrl = url.split("elapp:").length > 1 ? url.split("elapp:")[1] : "";
        }
        if (StringUtil.isNullOrEmpty(capsuleUrl)) {
            Toast.makeText(getContext(), getString(R.string.mini_app_invalid_url), Toast.LENGTH_SHORT).show();
            return;
        }

        mDoloadFileName = capsuleUrl.substring(capsuleUrl.lastIndexOf("/") + 1).trim();
        mDoloadUrl = capsuleUrl;

        mHandler.sendEmptyMessage(SHOW_LOADING);
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder().url(capsuleUrl).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("capsule_download", "download failed");
                mHandler.sendEmptyMessage(DOWNLOAD_FAILED);
            }

            @Override
            public void onResponse(Call call, final Response response) {
                Log.d("capsule_download", "download onResponse");
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        InputStream is = null;
                        byte[] buf = new byte[2048];
                        int len = 0;
                        FileOutputStream fos = null;

                        try {
                            is = response.body().byteStream();
                            long total = response.body().contentLength();
                            File file = new File(mDownloadDir, mDoloadFileName);
                            fos = new FileOutputStream(file);
                            long sum = 0;
                            while ((len = is.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                                sum += len;
                                int progress = (int) (sum * 1.0f / total * 100);
                                // 下载中
//                        listener.onDownloading(progress);
                            }
                            fos.flush();
                            Log.d("capsule_download", "download success");
                            refreshApps();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.d("capsule_download", "download failed");
                        } finally {
                            try {
                                if (is != null)
                                    is.close();
                            } catch (IOException e) {
                            }
                            try {
                                if (fos != null)
                                    fos.close();
                            } catch (IOException e) {
                            }
                            mHandler.sendEmptyMessage(DISMISS_LOADING);
                        }
                    }
                });
            }
        });
    }


    private void refreshApps() {
        try {
            File downloadPath = new File(mDownloadDir, mDoloadFileName);
            File outPath = new File(mDownloadCacheDir, mDoloadFileName);
            FileHelper.deleteFile(outPath);
            FileHelper.decompression(downloadPath.getAbsolutePath(), outPath.getAbsolutePath());

            List<String> ret = new ArrayList();
            FileHelper.findAppJsonPath(outPath, ret);
            if (ret.size() <= 0) return;
            Log.d("capsule_download", "parse capsule success");
            String json = FileHelper.getJsonFromCapsule(new File(ret.get(0), "app.json"));
            MyAppItem item = new Gson().fromJson(json, MyAppItem.class);
            if (item == null) return;

            item.path = /*new File(backupPath, mDoloadFileName).getAbsolutePath()*/mDoloadUrl;
            item.icon = new File(ret.get(0), item.icon).getAbsolutePath();

            String hash = CryptoHelper.getShaChecksum(downloadPath.getAbsolutePath());
            Log.d(TAG, "mDoloadFileName:" + mDoloadFileName + " hash:" + hash);

//                            String key = "key=Dev/dopsvote.h5.app/Release/Web/1.0.0";
//            String key = "Dev/" + item.name + "/Release/" + item.platform + "/" + item.version;
//            RegisterChainData appSetting = ProfileDataSource.getInstance(getContext()).getMiniAppSetting(item.did, key);

//            if (StringUtil.isNullOrEmpty(hash) ||
//                    null == appSetting ||
//                    StringUtil.isNullOrEmpty(appSetting.hash) ||
//                    !appSetting.hash.equals(hash)) {
//                messageToast("illegal file");
//                deleteFile(downloadPath);
//                deleteFile(outPath);
////              deleteFile(backupPath);
//                return;
//            }
            Log.d(TAG, "capsule legitimacy");

            if (item != null) {
                for (int i = 0; i < mItems.size(); i++) {
                    if (item.appId.equals(mItems.get(i).appId)) {
                        mItems.add(i, item);
                        mItems.remove(i + 1);
                        mHandler.sendEmptyMessage(UPDATE_APPS_MSG);
                        return;
                    }
                }

                mAppIds.add(item.appId);
                mItems.add(item);
                mHandler.sendEmptyMessage(UPDATE_APPS_MSG);
                Log.d("capsule_download", "UPDATE_APPS_MSG");

                BRSharedPrefs.putAddedAppId(getContext(), new Gson().toJson(mAppIds));
                upAppStatus(item.appId, "normal");
//                                upUserAppInfo(mAppIds);
            }
            FileHelper.deleteFile(downloadPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<MyAppItem> mRemoveApp = new ArrayList<>();

    @Override
    public void onDelete(MyAppItem item, int position) {
        String appId = item.appId;
        if (!StringUtil.isNullOrEmpty(appId)) {
            mRemoveApp.add(item);
            mRemoveAppLayout.setVisibility(View.VISIBLE);
            String languageCode = Locale.getDefault().getLanguage();
            if (!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")) {
                mRemoveHint.setText(Html.fromHtml(String.format(getString(R.string.esign_remove_nini_app_hint), item.name_zh_CN)));
            } else {
                mRemoveHint.setText(Html.fromHtml(String.format(getString(R.string.esign_remove_nini_app_hint), item.name_en)));
            }
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

        switch (miniAppId) {
            case BRConstants.VOTE_ME_ID:
                BRSharedPrefs.putVotemeDeleteStatue(getContext(), StringUtil.isNullOrEmpty(status) || status.equals("deleted"));
                break;
            case BRConstants.MINI_APPS_ID:
                BRSharedPrefs.putMiniAppsDeleteStatue(getContext(), StringUtil.isNullOrEmpty(status) || status.equals("deleted"));
                break;
            case BRConstants.REA_PACKAGE_ID:
                BRSharedPrefs.putRedPacketDeleteStatue(getContext(), StringUtil.isNullOrEmpty(status) || status.equals("deleted"));
                break;
            case BRConstants.DPOS_VOTE_ID:
                BRSharedPrefs.putVoteDeleteStatue(getContext(), StringUtil.isNullOrEmpty(status) || status.equals("deleted"));
                break;
            case BRConstants.ELA_NEWS_ID:
                BRSharedPrefs.putElaNewsDeleteStatue(getContext(), StringUtil.isNullOrEmpty(status) || status.equals("deleted"));
                break;
            case BRConstants.ELA_APPS_ID:
                BRSharedPrefs.putElappDeleteStatue(getContext(), StringUtil.isNullOrEmpty(status) || status.equals("deleted"));
                break;

        }

        String path = "/Apps/" + BRConstants.ELAPHANT_APP_ID + "/MiniPrograms/" + miniAppId + "/Status";
        String data = getKeyVale(path, status);
        String info = mDid.signInfo(mSeed, data, false);
        if (!StringUtil.isNullOrEmpty(info)) {
            ProfileDataSource.getInstance(getContext()).upchainSync(info);
        }
    }


    private StringChainData getAppStatus(String miniAppId) {
        String path = "/Apps/" + BRConstants.ELAPHANT_APP_ID + "/MiniPrograms/" + miniAppId + "/Status";
        if (mDid == null) return null;
        mDid.syncInfo();
        String value = mDid.getInfo(path, false, mSeed);
        if (!StringUtil.isNullOrEmpty(value)) {
            return new Gson().fromJson(value, StringChainData.class);
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
            mSeed = IdentityManager.getSeed(mnemonic, "");
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

    private void showDialog() {
        mHandler.sendEmptyMessageDelayed(SHOW_LOADING, 50);
    }

    private void dialogDismiss() {
        mHandler.sendEmptyMessage(DISMISS_LOADING);
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

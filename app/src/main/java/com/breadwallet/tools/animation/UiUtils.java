package com.breadwallet.tools.animation;

import android.Manifest;
import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.Toast;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.did.CallbackEntity;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.presenter.activities.AddAppsActivity;
import com.breadwallet.presenter.activities.AppAboutActivity;
import com.breadwallet.presenter.activities.DisabledActivity;
import com.breadwallet.presenter.activities.EsignHistoryActivity;
import com.breadwallet.presenter.activities.ExploreWebActivity;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.activities.LoginActivity;
import com.breadwallet.presenter.activities.MultiSignCreateActivity;
import com.breadwallet.presenter.activities.MultiSignTxActivity;
import com.breadwallet.presenter.activities.VoteActivity;
import com.breadwallet.presenter.activities.WalletActivity;
import com.breadwallet.presenter.activities.WalletNameActivity;
import com.breadwallet.presenter.activities.camera.ScanQRActivity;
import com.breadwallet.presenter.activities.crc.CrcMembersActivity;
import com.breadwallet.presenter.activities.crc.CrcVoteActivity;
import com.breadwallet.presenter.activities.did.DidAuthorizeActivity;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.activities.settings.WebViewActivity;
import com.breadwallet.presenter.activities.sign.SignDetailActivity;
import com.breadwallet.presenter.activities.sign.SignSuccessActivity;
import com.breadwallet.presenter.activities.sign.SignaureActivity;
import com.breadwallet.presenter.activities.sign.SignaureEditActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.entities.ElapayEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.presenter.fragments.FragmentReceive;
import com.breadwallet.presenter.fragments.FragmentRequestAmount;
import com.breadwallet.presenter.fragments.FragmentSend;
import com.breadwallet.presenter.fragments.FragmentSignal;
import com.breadwallet.presenter.fragments.FragmentSupport;
import com.breadwallet.presenter.fragments.FragmentTxDetails;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.manager.BRPublicSharedPrefs;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.FileHelper;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.elastos.jni.Constants;
import com.google.gson.Gson;
import com.platform.APIClient;
import com.platform.sqlite.PlatformSqliteHelper;

import org.chat.lib.presenter.ChatDetailActivity;
import org.chat.lib.presenter.ChatGroupSelectActivity;
import org.chat.lib.presenter.ChatScanActivity;
import org.chat.lib.presenter.FriendProfileEditActivity;
import org.chat.lib.presenter.GroupNameActivity;
import org.chat.lib.presenter.MyQrActivity;
import org.chat.lib.presenter.NewFriendListActivity;

import java.io.File;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import static android.content.Context.ACTIVITY_SERVICE;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/13/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class UiUtils {
    private static final String TAG = UiUtils.class.getName();
    public static final int CLICK_PERIOD_ALLOWANCE = 1000;
    private static long mLastClickTime = 0;
    private static boolean mSupportIsShowing;

    public static void showBreadSignal(Activity activity, String title, String iconDescription, int drawableId, BROnSignalCompletion completion) {
        FragmentSignal mFragmentSignal = new FragmentSignal();
        Bundle bundle = new Bundle();
        bundle.putString(FragmentSignal.TITLE, title);
        bundle.putString(FragmentSignal.ICON_DESCRIPTION, iconDescription);
        mFragmentSignal.setCompletion(completion);
        bundle.putInt(FragmentSignal.RES_ID, drawableId);
        mFragmentSignal.setArguments(bundle);
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_bottom, R.animator.from_bottom, R.animator.to_bottom);
        transaction.add(android.R.id.content, mFragmentSignal, mFragmentSignal.getClass().getName());
        transaction.addToBackStack(null);
        if (!activity.isDestroyed()) {
            transaction.commitAllowingStateLoss();
        }
    }

    public static void setIsSupportFragmentShown(boolean isSupportFragmentShown) {
        mSupportIsShowing = isSupportFragmentShown;
    }

    public static void showSendFragment(FragmentActivity app, final CryptoRequest request) {
        if (app == null) {
            Log.e(TAG, "showSendFragment: app is null");
            return;
        }

        FragmentSend fragmentSend = (FragmentSend) app.getSupportFragmentManager().findFragmentByTag(FragmentSend.class.getName());
        if (fragmentSend == null) {
            fragmentSend = new FragmentSend();
        }
        fragmentSend.saveViewModelData(request);
        if (!fragmentSend.isAdded()) {
            app.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                    .add(android.R.id.content, fragmentSend, FragmentSend.class.getName())
                    .addToBackStack(FragmentSend.class.getName()).commitAllowingStateLoss();
        }

    }

    public static void showSupportFragment(FragmentActivity app, String articleId, BaseWalletManager wm) {
        if (mSupportIsShowing) {
            return;
        }
        try {
            mSupportIsShowing = true;
            if (app == null) {
                Log.e(TAG, "showSupportFragment: app is null");
                return;
            }

            FragmentSupport fragmentSupport = (FragmentSupport) app.getSupportFragmentManager().findFragmentByTag(FragmentSupport.class.getName());
            if (fragmentSupport != null && fragmentSupport.isAdded()) {
                app.getFragmentManager().popBackStack();
                return;
            }
            String iso = BaseBitcoinWalletManager.BITCOIN_SYMBOL;
            if (wm != null) wm.getIso();
            fragmentSupport = new FragmentSupport();
            Bundle bundle = new Bundle();
            bundle.putString("walletIso", iso);
            if (!Utils.isNullOrEmpty(articleId))
                bundle.putString("articleId", articleId);

            fragmentSupport.setArguments(bundle);
            app.getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                    .add(android.R.id.content, fragmentSupport, FragmentSend.class.getName())
                    .addToBackStack(FragmentSend.class.getName()).commitAllowingStateLoss();
        } finally {
            mSupportIsShowing = false;
        }

    }


    public static void showTransactionDetails(Activity app, TxUiHolder item, int position) {

        FragmentTxDetails txDetails = (FragmentTxDetails) app.getFragmentManager().findFragmentByTag(FragmentTxDetails.class.getName());

        if (txDetails != null && txDetails.isAdded()) {
            Log.e(TAG, "showTransactionDetails: Already showing");

            return;
        }

        txDetails = new FragmentTxDetails();
        txDetails.setTransaction(item);
        txDetails.show(app.getFragmentManager(), "txDetails");

    }

    public static void startMultiTxActivity(Context context, Uri uri) {
        Intent intent = new Intent();
        intent.setClass(context, MultiSignTxActivity.class);
        intent.setData(uri);
        context.startActivity(intent);
    }

    public static void startWaitAcceptActivity(Context context) {
        Intent intent = new Intent(context, NewFriendListActivity.class);
        context.startActivity(intent);
    }

    public static void startMultiCreateActivity(Context context, Uri uri) {
        Intent intent = new Intent();
        intent.setClass(context, MultiSignCreateActivity.class);
        intent.setData(uri);
        context.startActivity(intent);
    }

    public static void startVoteActivity(Context context, String url) {
        Intent intent = new Intent(context, VoteActivity.class);
        intent.putExtra("vote_scheme_uri", url);
        context.startActivity(intent);
    }

    public static void startCrcMembersActivity(Context context, String from, String candidates, String votes) {
        Intent intent = new Intent(context, CrcMembersActivity.class);
        intent.putExtra("from", from);
        intent.putExtra("candidates", candidates);
        intent.putExtra("votes", votes);
        context.startActivity(intent);
    }

    public static void startCrcActivity(final Context context, String url) {
        Intent intent = new Intent(context, CrcVoteActivity.class);
        intent.putExtra("vote_scheme_uri", url);
        context.startActivity(intent);
    }

    public static void startWebviewActivity(Context context, String url) {
        Intent intent = new Intent(context, ExploreWebActivity.class);
        intent.putExtra("explore_url", url);
        context.startActivity(intent);
    }

    public static void startWebviewActivity(Context context, String url, String appId) {
        Intent intent = new Intent(context, ExploreWebActivity.class);
        intent.putExtra("explore_url", url);
        intent.putExtra("app_id", appId);
        context.startActivity(intent);
    }

    public static void startWalletActivity(Context context, String url) {
        Intent intent = new Intent(context, WalletActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_KEY.META_EXTRA, url);
        context.startActivity(intent);
    }

    public static void startAuthorActivity(Context context, String url) {
        Intent intent = new Intent(context, DidAuthorizeActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_KEY.META_EXTRA, url);
        context.startActivity(intent);
    }

    public static void startSignActivity(Context context, String url) {
        Intent intent = new Intent(context, SignaureActivity.class);
        intent.putExtra(Constants.INTENT_EXTRA_KEY.META_EXTRA, url);
        context.startActivity(intent);
    }

    public static void startSignDetailActivity(Context context, String signed, String sign) {
        Intent intent = new Intent(context, SignDetailActivity.class);
        intent.putExtra("signed", signed);
        intent.putExtra("sign", sign);
        context.startActivity(intent);
    }

    public static void startSignSuccessActivity(Context context, String signed) {
        Intent intent = new Intent(context, SignSuccessActivity.class);
        intent.putExtra("signed", signed);
        context.startActivity(intent);
    }

    public static void startSignHistoryActivity(Context context) {
        Intent intent = new Intent(context, EsignHistoryActivity.class);
        context.startActivity(intent);
    }

    public static void openUrlByBrowser(Context context, String url) {
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        context.startActivity(intent);
    }

    public static void openScanner(Activity app, int requestID) {
        try {
            if (app == null) {
                return;
            }

            // Check if the camera permission is granted
            if (ContextCompat.checkSelfPermission(app,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(app,
                        Manifest.permission.CAMERA)) {
                    BRDialog.showCustomDialog(app, app.getString(R.string.Send_cameraUnavailabeTitle_android),
                            app.getString(R.string.Send_cameraUnavailabeMessage_android), app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismiss();
                                }
                            }, null, null, 0);
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(app,
                            new String[]{Manifest.permission.CAMERA},
                            BRConstants.CAMERA_REQUEST_ID);
                }
            } else {
                // Permission is granted, open camera
                Intent intent = new Intent(app, ScanQRActivity.class);
                app.startActivityForResult(intent, requestID);
                app.overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static LayoutTransition getDefaultTransition() {
        LayoutTransition itemLayoutTransition = new LayoutTransition();
        itemLayoutTransition.setStartDelay(LayoutTransition.APPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.DISAPPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.CHANGE_APPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.CHANGE_DISAPPEARING, 0);
        itemLayoutTransition.setStartDelay(LayoutTransition.CHANGING, 0);
        itemLayoutTransition.setDuration(100);
        itemLayoutTransition.setInterpolator(LayoutTransition.CHANGING, new OvershootInterpolator(2f));
        Animator scaleUp = ObjectAnimator.ofPropertyValuesHolder((Object) null, PropertyValuesHolder.ofFloat(View.SCALE_X, 1, 1), PropertyValuesHolder.ofFloat(View.SCALE_Y, 0, 1));
        scaleUp.setDuration(50);
        scaleUp.setStartDelay(50);
        Animator scaleDown = ObjectAnimator.ofPropertyValuesHolder((Object) null, PropertyValuesHolder.ofFloat(View.SCALE_X, 1, 1), PropertyValuesHolder.ofFloat(View.SCALE_Y, 1, 0));
        scaleDown.setDuration(2);
        itemLayoutTransition.setAnimator(LayoutTransition.APPEARING, scaleUp);
        itemLayoutTransition.setAnimator(LayoutTransition.DISAPPEARING, null);
        itemLayoutTransition.enableTransitionType(LayoutTransition.CHANGING);
        return itemLayoutTransition;
    }

    public static void showRequestFragment(FragmentActivity app) {
        if (app == null) {
            Log.e(TAG, "showRequestFragment: app is null");
            return;
        }

        FragmentRequestAmount fragmentRequestAmount = (FragmentRequestAmount) app.getSupportFragmentManager().findFragmentByTag(FragmentRequestAmount.class.getName());
        if (fragmentRequestAmount != null && fragmentRequestAmount.isAdded())
            return;

        fragmentRequestAmount = new FragmentRequestAmount();
        Bundle bundle = new Bundle();
        fragmentRequestAmount.setArguments(bundle);
        app.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                .add(android.R.id.content, fragmentRequestAmount, FragmentRequestAmount.class.getName())
                .addToBackStack(FragmentRequestAmount.class.getName()).commitAllowingStateLoss();

    }

    //isReceive tells the Animator that the Receive fragment is requested, not My Address
    public static void showReceiveFragment(FragmentActivity app, boolean isReceive) {
        if (app == null) {
            Log.e(TAG, "showReceiveFragment: app is null");
            return;
        }
        FragmentReceive fragmentReceive = (FragmentReceive) app.getSupportFragmentManager().findFragmentByTag(FragmentReceive.class.getName());
        if (fragmentReceive != null && fragmentReceive.isAdded())
            return;
        fragmentReceive = new FragmentReceive();
        Bundle args = new Bundle();
        args.putBoolean("receive", isReceive);
        fragmentReceive.setArguments(args);

        app.getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(0, 0, 0, R.animator.plain_300)
                .add(android.R.id.content, fragmentReceive, FragmentReceive.class.getName())
                .addToBackStack(FragmentReceive.class.getName()).commitAllowingStateLoss();

    }

    public static boolean isClickAllowed() {
        boolean allow = false;
        if (System.currentTimeMillis() - mLastClickTime > CLICK_PERIOD_ALLOWANCE) {
            allow = true;
        }
        mLastClickTime = System.currentTimeMillis();
        return allow;
    }

    public static void killAllFragments(Activity app) {
        if (app != null && !app.isDestroyed() && !app.isFinishing())
            app.getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public static void startBreadActivity(Activity from, boolean auth) {
        if (from == null) {
            return;
        }
        Class toStart = auth ? LoginActivity.class : HomeActivity.class;

        Intent intent = new Intent(from, toStart);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        from.startActivity(intent);
    }

    public static void startBreadActivity(Activity from, boolean auth, String url) {
        if (from == null) {
            return;
        }
        Class toStart = auth ? LoginActivity.class : HomeActivity.class;

        Intent intent = new Intent(from, toStart);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("url", url);
        from.startActivity(intent);
    }

    private static void setStatusBarColor(Activity app, int color) {
        if (app == null) return;
        Window window = app.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(app.getColor(color));
    }

    public static void showWalletDisabled(Activity app) {
        Intent intent = new Intent(app, DisabledActivity.class);
        app.startActivity(intent);
        app.overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
        Log.e(TAG, "showWalletDisabled: " + app.getClass().getName());

    }

    public static boolean isLast(Activity app) {
        ActivityManager mngr = (ActivityManager) app.getSystemService(ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> taskList = mngr.getRunningTasks(10);

        if (taskList.get(0).numActivities == 1 &&
                taskList.get(0).topActivity.getClassName().equals(app.getClass().getName())) {
            return true;
        }
        return false;
    }

    public static void startWebActivity(Activity activity, String url) {
        Intent intent = new Intent(activity, WebViewActivity.class);
        intent.putExtra(BRConstants.EXTRA_URL, url);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
    }

    public static boolean isMainThread() {
        boolean isMain = Looper.myLooper() == Looper.getMainLooper();
        if (isMain) {
            Log.e(TAG, "IS MAIN UI THREAD!");
        }
        return isMain;
    }

    public static int getThemeId(Activity activity) {
        try {
            return activity.getPackageManager().getActivityInfo(activity.getComponentName(), 0).getThemeResource();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error finding theme for this Activity -> " + activity.getLocalClassName());
        }

        return 0;
    }

    public static void startAddAppsActivity(Activity activity, int requestCode) {
        Intent intent = new Intent(activity, AddAppsActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startAddFriendActivity(Activity app, String type) {
        try {
            if (app == null) {
                return;
            }

            // Check if the camera permission is granted
            if (ContextCompat.checkSelfPermission(app,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(app,
                        Manifest.permission.CAMERA)) {
                    BRDialog.showCustomDialog(app, app.getString(R.string.Send_cameraUnavailabeTitle_android),
                            app.getString(R.string.Send_cameraUnavailabeMessage_android), app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismiss();
                                }
                            }, null, null, 0);
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(app,
                            new String[]{Manifest.permission.CAMERA},
                            BRConstants.CHAT_CAMERA_REQUST_ID);
                }
            } else {

                Intent intent = new Intent(app, ChatScanActivity.class);
                intent.putExtra("type", type);
                app.startActivityForResult(intent, BRConstants.SCANNER_REQUEST);
                app.overridePendingTransition(R.anim.fade_up, R.anim.fade_down);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startProfileEditActivity(Context context, String friendCode, String nickname) {
        Intent intent = new Intent(context, FriendProfileEditActivity.class);
        intent.putExtra("did", friendCode);
        intent.putExtra("nickname", nickname);
        context.startActivity(intent);
    }

    public static void startChatDetailActivity(Context context, String friendCode, String type, String chatName) {
        Intent intent = new Intent(context, ChatDetailActivity.class);
        intent.putExtra("did", friendCode);
        intent.putExtra("type", type);
        intent.putExtra("chatName", chatName);
        context.startActivity(intent);
    }

    public static void startMyQrActivity(Context context) {
        Intent intent = new Intent(context, MyQrActivity.class);
        context.startActivity(intent);
    }

    public static void startGroupNameActivity(Context context, String friendCode) {
        Intent intent = new Intent(context, GroupNameActivity.class);
        intent.putExtra("did", friendCode);
        context.startActivity(intent);
    }

    public static void startChatGroupSelectActivity(Activity activity, List<String> friendCodes) {
        Intent intent = new Intent(activity, ChatGroupSelectActivity.class);
        intent.putExtra("did", friendCodes.toString());
        activity.startActivityForResult(intent, BRConstants.CHAT_GROUP_SELECT_FRIENDS);
    }

    public static void startSignEditActivity(Activity activity, String from, String value, int requestCode) {
        Intent intent = new Intent(activity, SignaureEditActivity.class);
        intent.putExtra("from", from);
        intent.putExtra("value", value);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startMiniAppAboutActivity(Context context, String appId) {
        Intent intent = new Intent(context, AppAboutActivity.class);
        intent.putExtra("appId", appId);
        context.startActivity(intent);
    }

    public static void returnDataNeedSign(Activity activity, String returnUrl, String Data, String Sign, String appId, String targe) {
        if (!StringUtil.isNullOrEmpty(returnUrl)) {
            String url;
            if (returnUrl.contains("?")) {
                url = returnUrl + "&Data=" + Uri.encode(Data) + "&Sign=" + Uri.encode(Sign) /*+ "&browser=elaphant"*/;
            } else {
                url = returnUrl + "?Data=" + Uri.encode(Data) + "&Sign=" + Uri.encode(Sign) /*+ "&browser=elaphant"*/;
            }

            String addAppIds = BRSharedPrefs.getAddedAppId(activity);
            if (!StringUtil.isNullOrEmpty(addAppIds) && addAppIds.contains(appId)
                /*|| (!StringUtil.isNullOrEmpty(targe) && targe.equals("internal"))*/) {
                UiUtils.startWebviewActivity(activity, url, appId);
            } else {
                UiUtils.openUrlByBrowser(activity, url);
            }
        }
    }

    public static void callbackDataNeedSign(Activity activity, String backurl, CallbackEntity entity) {
        try {
            if (entity == null || StringUtil.isNullOrEmpty(backurl)) return;
            String params = new Gson().toJson(entity);
            String ret = DidDataSource.getInstance(activity).urlPost(backurl, params);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    public static void toast(final Activity activity, final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(null!=activity && !activity.isFinishing()) Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void payReturnData(final Context context, String txid) {
        payReturn(context, txid);
        try {
            payCallback(context, txid);
        } catch (Exception e) {
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BreadApp.getBreadContext(), "callback error", Toast.LENGTH_SHORT).show();
                }
            });
        }
        WalletActivity.mCallbackUrl = null;
        WalletActivity.mReturnUrl = null;
        WalletActivity.mAppId = null;
        WalletActivity.mOrderId = null;
    }

    private static void payReturn(Context context, String txid) {
        String url = null;
        if (!StringUtil.isNullOrEmpty(WalletActivity.mReturnUrl)) { //call return url
            if (WalletActivity.mReturnUrl.contains("?")) {
                url = WalletActivity.mReturnUrl + "&TXID=" + Uri.encode(txid) + "&OrderID=" + Uri.encode(WalletActivity.mOrderId);
            } else {
                url = WalletActivity.mReturnUrl + "?TXID=" + Uri.encode(txid) + "&OrderID=" + Uri.encode(WalletActivity.mOrderId);
            }

            String addAppIds = BRSharedPrefs.getAddedAppId(context);
            if (!StringUtil.isNullOrEmpty(addAppIds) && addAppIds.contains(WalletActivity.mAppId)) {
                UiUtils.startWebviewActivity(context, url, WalletActivity.mAppId);
            } else {
                UiUtils.openUrlByBrowser(context, url);
            }
        }
    }

    private static void payCallback(Context context, String txid) throws Exception{
        if (!StringUtil.isNullOrEmpty(WalletActivity.mCallbackUrl)) { //call back url
            ElapayEntity elapayEntity = new ElapayEntity();
            elapayEntity.OrderID = WalletActivity.mOrderId;
            elapayEntity.TXID = txid;
            APIClient.urlPost(WalletActivity.mCallbackUrl, new Gson().toJson(elapayEntity));
        }
    }

    public static String getStringMd5(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data.getBytes());
        byte[] digest = md.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) hexString.append(String.format("%02x", b));

        return hexString.toString();
    }

    public static void toast(Context context, int resId) {
        Toast toast = Toast.makeText(context, resId, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    public static void toastMessage(Activity activity, int layout) {
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(layout, null);
        Toast toast = new Toast(activity);
        toast.setView(view);
        toast.setGravity(Gravity.FILL, 0, 0);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void shareCapsule(Context context, String url) {
//        Uri contentUri = FileProvider.getUriForFile(context, "elaphant.app.capsuleProvider", file);
//        if (contentUri == null) return;
//        Intent shareIntent = new Intent();
//        shareIntent.setAction(Intent.ACTION_SEND);
//        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        shareIntent.setDataAndType(contentUri, /*context.getContentResolver().getType(contentUri)*/"text/plain");
//
//        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
//        context.startActivity(Intent.createChooser(shareIntent, "Share"));

        if(StringUtil.isNullOrEmpty(url)) return;
        Intent textIntent = new Intent(Intent.ACTION_SEND);
        textIntent.setType("text/plain");
        textIntent.putExtra(Intent.EXTRA_TEXT, url);
        context.startActivity(Intent.createChooser(textIntent, "Share"));
    }

    public static void restartApp(Context context) {
        // sleep 500ms for saving phrase list
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        assert intent != null;
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    public static void switchPhrase(Activity context, String phrase, boolean restart, boolean recover, String walletName) {
        PostAuth.getInstance().setCachedPaperKey(phrase);

        //Disallow BTC and BCH sending.
        BRSharedPrefs.putAllowSpend(context, BaseBitcoinWalletManager.BITCASH_SYMBOL, false);
        BRSharedPrefs.putAllowSpend(context, BaseBitcoinWalletManager.BITCOIN_SYMBOL, false);

        PostAuth.getInstance().onRecoverWalletAuth(context, false, restart, recover, walletName);
    }

    public static void startWalletNameActivity(Activity context, int type, boolean reenter) {
        Intent intent = new Intent(context, WalletNameActivity.class);
        intent.putExtra(WalletNameActivity.WALLET_NAME_PAGE_TYPE, type);
        intent.putExtra(WalletNameActivity.WALLET_NAME, getDefaultWalletName(context));
        intent.putExtra(IntroActivity.INTRO_REENTER, reenter);
        context.startActivity(intent);
    }

    public static String getDefaultWalletName(Activity context) {
        int count = BRKeyStore.getPhraseCount(context) + 1;
        return "Wallet #" + count;
    }

    public static String getSha256(byte[] data) {
        byte[] digest = CryptoHelper.sha256(data);
        if (digest == null) return "";
        StringBuilder hexString = new StringBuilder();
        for (byte b : digest) hexString.append(String.format("%02x", b));

        return hexString.toString();
    }

    public static void setStorageName(byte[] phrase) {
        String hash = getSha256(phrase);
        if (StringUtil.isNullOrEmpty(hash)) return;

        BRSQLiteHelper.DATABASE_NAME = hash + ".db";
        PlatformSqliteHelper.DATABASE_NAME = hash + "_platform.db";
        BRSharedPrefs.PREFS_NAME = "profile_" + hash;
    }

    public static boolean isSingleWallet(Context context, byte[] phrase){
        File databasePath = new File(context.getFilesDir().getParent(), "databases");
        File sharedPrefsPath = new File(context.getFilesDir().getParent(), "shared_prefs");

        String hash = UiUtils.getSha256(phrase);

        if(!new File(databasePath, hash + ".db").exists() && !new File(sharedPrefsPath, "profile_" + hash + ".xml").exists()) {
            return true;
        }

        return false;
    }

    public static String getCacheProviderName(Context context, String name) {
        if(StringUtil.isNullOrEmpty(name)) return null;
        String hash = BRSharedPrefs.getSingleWalletHash(context);
        if(StringUtil.isNullOrEmpty(hash)) return name;
        if(name.equals(hash+".db")) {
            return "breadwallet.db";
        } else if(name.equals(hash + "_platform.db")){
            return "platform.db";
        } else if(name.equals("profile_" + hash)){
            return "MyPrefsFile";
        }
        return name;
    }

    public static void clearCache(Context context) {
        boolean hasWroteDown = BRSharedPrefs.getPhraseWroteDown(context);

        File webviewPath = new File(context.getFilesDir().getParent(), "app_webview");
        FileHelper.deleteFile(webviewPath);

        File appInternalPath = new File(context.getFilesDir().getParent(), "app_download_internal");
        FileHelper.deleteFile(appInternalPath);

        File codeCache = new File(context.getFilesDir().getParent(), "code_cache");
        FileHelper.deleteFile(codeCache);

        File cachePath = new File(context.getFilesDir().getParent(), "cache");
        FileHelper.deleteFile(cachePath);

        File databasePath = new File(context.getFilesDir().getParent(), "databases");
//        File sharedPrefsPath = new File(context.getFilesDir().getParent(), "shared_prefs");
//
//        FileHelper.deleteFile(databasePath);
//        File[] files = sharedPrefsPath.listFiles();
//        for (int i = 0; i < files.length; i++) {
//            File f = files[i];
//            String name = f.getName();
//            if(!StringUtil.isNullOrEmpty(name) && !name.equals("keyStorePrefs.xml")) {
//                BRSharedPrefs.clearAllPrefs(context, name.replace(".xml", ""));
//            }
//        }
        String databaseName = getCacheProviderName(context, BRSQLiteHelper.DATABASE_NAME);
        String platformDbName = getCacheProviderName(context, PlatformSqliteHelper.DATABASE_NAME);
        FileHelper.deleteFile(new File(databasePath, databaseName));
        FileHelper.deleteFile(new File(databasePath, platformDbName));
        BRSharedPrefs.clearAllPrefs(context);
        BRPublicSharedPrefs.putUseFingerprint(context, false);
        BRKeyStore.putSpendLimit(context, new BigDecimal(1000000.00000000), "BTC");

        BRSharedPrefs.putPhraseWroteDown(context, hasWroteDown);

        restartApp(context);
    }
}

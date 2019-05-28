package com.breadwallet.presenter.fragments;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.breadwallet.tools.adapter.ExploreAppsAdapter;
import com.breadwallet.tools.animation.SimpleItemTouchHelperCallback;
import com.breadwallet.tools.listeners.OnStartDragListener;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.StringUtil;

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
        mMyAppsRv = rootView.findViewById(R.id.explore_my_apps_lv);

        mMyAppsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mMyAppsRv.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mMyAppsRv);
    }

    private void initListener(){
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

    private long downloadCapsule(String url){
        Log.d(TAG, "capsule url:"+url);
        if(StringUtil.isNullOrEmpty(url)) return -1;
        DownloadManager.Request request;
        try{
            Uri uri = Uri.parse(url);
            String fileName = uri.getLastPathSegment();
            request = new DownloadManager.Request(uri);
            request.setDestinationInExternalFilesDir(getContext(), Environment.DIRECTORY_DOWNLOADS, fileName+".zip");
            DownloadManager manager = (DownloadManager) getContext().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId =  manager.enqueue(request);
            Uri downloadUri = manager.getUriForDownloadedFile(downloadId);
            Log.d(TAG, "cache capsule path:"+downloadUri.getPath());
            registerDownloadReceiver(downloadId, downloadUri);
            return downloadId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void registerDownloadReceiver(final long downloadId, final Uri uri){
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                Log.d(TAG, "downloadId:"+downloadId);
                Log.d(TAG, "uri:"+uri.getPath());
                Log.d(TAG, "id:"+id);
                if(id == downloadId){
                    decompression(uri);
                }
                context.unregisterReceiver(this);
            }
        };
        getContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    private void decompression(Uri uri){

    }

}

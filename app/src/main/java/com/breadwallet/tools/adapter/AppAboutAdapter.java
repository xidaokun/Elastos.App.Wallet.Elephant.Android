package com.breadwallet.tools.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.AppAboutItem;

import java.util.List;

public class AppAboutAdapter extends RecyclerView.Adapter<AppAboutAdapter.AppAboutViewHoler> {

    private Context mContext;
    private List<AppAboutItem> mData;

    public AppAboutAdapter(Context context, List<AppAboutItem> data){
        this.mContext = context;
        this.mData = data;
    }

    @Override
    public AppAboutViewHoler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View contentView = LayoutInflater.from(mContext).inflate(R.layout.app_about_item_layout, null);
        return new AppAboutViewHoler(contentView);
    }

    @Override
    public void onBindViewHolder(@NonNull AppAboutViewHoler holder, int position) {
        AppAboutItem appAboutItem = mData.get(position);
        holder.mTitle.setText(appAboutItem.getTitle());
        holder.mContent.setText(appAboutItem.getContent());
        if(appAboutItem.isCanShare()){
            holder.mShare.setVisibility(View.VISIBLE);
        } else {
            holder.mShare.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return (mData==null) ? 0: mData.size();
    }

    public class AppAboutViewHoler extends RecyclerView.ViewHolder{

        private BaseTextView mTitle;
        private BaseTextView mContent;
        private BaseTextView mShare;

        public AppAboutViewHoler(View itemView) {
            super(itemView);

            mTitle = itemView.findViewById(R.id.app_about_item_title);
            mContent = itemView.findViewById(R.id.app_about_item_content);
            mShare = itemView.findViewById(R.id.app_about_item_share);
        }


    }
}

package com.breadwallet.tools.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.RoundImageView;
import com.breadwallet.presenter.entities.MyAppItem;
import com.breadwallet.tools.animation.ItemTouchHelperAdapter;

import java.util.Collections;
import java.util.List;

public class ExploreAppsAdapter extends RecyclerView.Adapter<ExploreAppsAdapter.MyAppsViewHolder> implements ItemTouchHelperAdapter{

    private Context mContext;
    private List<MyAppItem> mData;

    public ExploreAppsAdapter(Context context, List<MyAppItem> data){
        this.mContext = context;
        this.mData = data;
    }

    @NonNull
    @Override
    public MyAppsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View contentView = LayoutInflater.from(mContext).inflate(R.layout.explore_my_apps_layout, null);
        return new MyAppsViewHolder(contentView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyAppsViewHolder holder, int position) {
//        final MyAppItem item = mData.get(position);
//        holder.mTitle.setText(item.getName());
//        holder.mDeveloper.setText(item.getDeveloper());
//        //TODO daokun.xi
////        holder.mLogo.setImageResource();
//        boolean delete = item.isDelete();
//        if(delete){
//            holder.mAbout.setVisibility(View.GONE);
//            holder.mDelete.setVisibility(View.VISIBLE);
//            holder.mTouch.setVisibility(View.VISIBLE);
//        } else {
//            holder.mAbout.setVisibility(View.VISIBLE);
//            holder.mDelete.setVisibility(View.GONE);
//            holder.mTouch.setVisibility(View.GONE);
//        }
    }

    @Override
    public int getItemCount() {
        return (mData==null) ? 0: mData.size();
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {
        notifyItemMoved(fromPosition, toPosition);
        int bound = mData.size();
        if(fromPosition>=bound || toPosition>=bound) return;
        Collections.swap(mData, fromPosition, toPosition);

        //TODO 交换数据库中数据的位置
    }

    @Override
    public void onItemDismiss(int position) {

    }

    public class MyAppsViewHolder extends RecyclerView.ViewHolder  {
        private RoundImageView mLogo;
        private BaseTextView mTitle;
        private BaseTextView mDeveloper;
        private BaseTextView mDelete;
        private BaseTextView mTouch;
        private BaseTextView mAbout;

        public MyAppsViewHolder(View itemView) {
            super(itemView);

            mLogo = itemView.findViewById(R.id.explore_item_logo_tv);
            mTitle = itemView.findViewById(R.id.explore_item_title_tv);
            mDeveloper = itemView.findViewById(R.id.explore_item_developer_tv);
            mDelete = itemView.findViewById(R.id.explore_item_delete_tv);
            mTouch = itemView.findViewById(R.id.explore_item_touch_tv);
            mAbout = itemView.findViewById(R.id.explore_about_tv);
        }
    }
}

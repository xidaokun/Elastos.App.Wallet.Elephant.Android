package com.breadwallet.tools.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.RoundImageView;
import com.breadwallet.presenter.entities.MyAppItem;
import com.breadwallet.tools.animation.ItemTouchHelperAdapter;
import com.breadwallet.tools.animation.ItemTouchHelperViewHolder;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class MiniAppsAdapter extends RecyclerView.Adapter<MiniAppsAdapter.MyAppsViewHolder> implements ItemTouchHelperAdapter{

    private Context mContext;
    private List<MyAppItem> mData;
    private boolean mIsDelete;
    private OnDeleteClickListener mDeleteListener;
    private OnAboutClickListener mAboutListener;
    private OnTouchMoveListener mMoveListener;
    private OnItemClickListener mItemClickListener;

    public MiniAppsAdapter(Context context, List<MyAppItem> data){
        this.mContext = context;
        this.mData = data;
    }

    public void isDelete(boolean isDelete){
        this.mIsDelete = isDelete;
    }

    public void setOnMoveListener(OnTouchMoveListener listener){
        this.mMoveListener = listener;
    }

    public void setOnDeleteClick(OnDeleteClickListener listener ){
        this.mDeleteListener = listener;
    }

    public void setOnItemClick(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    public void setOnAboutClick(OnAboutClickListener listener){
        this.mAboutListener = listener;
    }

    @NonNull
    @Override
    public MyAppsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View contentView = LayoutInflater.from(mContext).inflate(R.layout.explore_my_apps_layout, null);
        return new MyAppsViewHolder(contentView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyAppsViewHolder holder, final int position) {
        final MyAppItem item = mData.get(position);

        String languageCode = Locale.getDefault().getLanguage();
        if(!StringUtil.isNullOrEmpty(languageCode) && languageCode.contains("zh")){
            holder.mTitle.setText(item.name_zh_CN);
        } else {
            holder.mTitle.setText(item.name_en);
        }

        if(StringUtil.isNullOrEmpty(item.name_zh_CN) && StringUtil.isNullOrEmpty(item.name_en)) {
            holder.mTitle.setText(item.name);
        }

        holder.mDeveloper.setText(item.developer);
        Bitmap bitmap = null;
        if(!StringUtil.isNullOrEmpty(item.icon)){
            bitmap = Utils.getIconFromPath(new File(item.icon));
        }
        if(null != bitmap){
            holder.mLogo.setImageBitmap(bitmap);
        } else {
            holder.mLogo.setImageResource(R.drawable.unknow);
        }

        if(null == bitmap) Log.d("bitmap_log", "iconPath:"+new File(item.icon).getAbsolutePath());

        //TODO daokun.xi
        if(mIsDelete){
            holder.mAbout.setVisibility(View.GONE);
            holder.mAboutTv.setVisibility(View.GONE);
            holder.mDelete.setVisibility(View.VISIBLE);
            holder.mTouch.setVisibility(View.VISIBLE);
        } else {
            holder.mAbout.setVisibility(View.VISIBLE);
            holder.mAboutTv.setVisibility(View.VISIBLE);
            holder.mDelete.setVisibility(View.GONE);
            holder.mTouch.setVisibility(View.GONE);
        }

        holder.mAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mAboutListener != null) mAboutListener.onAbout(item, position);
            }
        });

        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mDeleteListener != null) mDeleteListener.onDelete(item, position);
            }
        });

        holder.mItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mItemClickListener!=null && !mIsDelete) mItemClickListener.onItemClick(item, position);
            }
        });
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
        if(mMoveListener != null){
            mMoveListener.onMove(fromPosition, toPosition);
        }
    }

    @Override
    public void onItemDismiss(int position) {

    }

    public class MyAppsViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
        private RoundImageView mLogo;
        private BaseTextView mTitle;
        private BaseTextView mDeveloper;
        private RoundImageView mDelete;
        private BaseTextView mTouch;
        private BaseTextView mAboutTv;
        private View mAbout;
        private View mItemView;

        public MyAppsViewHolder(View itemView) {
            super(itemView);

            mLogo = itemView.findViewById(R.id.explore_item_logo_tv);
            mTitle = itemView.findViewById(R.id.explore_item_title_tv);
            mDeveloper = itemView.findViewById(R.id.explore_item_developer_tv);
            mDelete = itemView.findViewById(R.id.explore_item_delete_tv);
            mTouch = itemView.findViewById(R.id.explore_item_touch_tv);
            mAbout = itemView.findViewById(R.id.explore_item_about_shadow);
            mAboutTv = itemView.findViewById(R.id.explore_item_about_tv);
            mItemView = itemView;
        }

        @Override
        public void onItemSelected() {

        }

        @Override
        public void onItemClear() {

        }
    }

    public interface OnTouchMoveListener {
        void onMove(int from, int to);
    }

    public interface OnDeleteClickListener {
        void onDelete(MyAppItem item, int position);
    }

    public interface OnAboutClickListener {
        void onAbout(MyAppItem item, int position);
    }

    public interface OnItemClickListener {
        void onItemClick(MyAppItem item, int position);
    }
}

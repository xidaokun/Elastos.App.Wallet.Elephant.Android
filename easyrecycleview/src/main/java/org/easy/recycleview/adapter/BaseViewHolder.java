package org.easy.recycleview.adapter;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Field;


abstract public class BaseViewHolder<M> extends RecyclerView.ViewHolder {
    public BaseViewHolder(View itemView) {
        super(itemView);
    }

    public BaseViewHolder(ViewGroup parent, @LayoutRes int res) {
        super(LayoutInflater.from(parent.getContext()).inflate(res, parent, false));
    }

    public void setData(M data) {
    }

    protected <T extends View> T $(@IdRes int id) {
        return (T) itemView.findViewById(id);
    }

    protected Context getContext(){
        return itemView.getContext();
    }

    protected int getDataPosition(){
        RecyclerView.Adapter adapter = getOwnerAdapter();
        if (adapter!=null && adapter instanceof RecyclerArrayAdapter){
            return getAdapterPosition() - ((RecyclerArrayAdapter) adapter).getHeaderCount();
        }
        return getAdapterPosition();
    }


    @Nullable
    protected <T extends RecyclerView.Adapter> T getOwnerAdapter(){
        RecyclerView recyclerView = getOwnerRecyclerView();
        return recyclerView == null?null: (T) recyclerView.getAdapter();
    }

    @Nullable
    protected RecyclerView getOwnerRecyclerView(){
        try {
            Field field = RecyclerView.ViewHolder.class.getDeclaredField("mOwnerRecyclerView");
            field.setAccessible(true);
            return (RecyclerView) field.get(this);
        } catch (NoSuchFieldException e) {
        } catch (IllegalAccessException e) {
        }
        return null;
    }

}
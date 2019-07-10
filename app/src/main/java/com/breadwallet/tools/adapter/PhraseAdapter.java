package com.breadwallet.tools.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.security.PhraseInfo;

import java.util.Arrays;
import java.util.List;

public class PhraseAdapter extends ArrayAdapter<PhraseInfo> {
    private int mResourceId;

    public PhraseAdapter(@NonNull Context context, int resource, @NonNull List<PhraseInfo> objects) {
        super(context, resource, objects);
        mResourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        PhraseInfo info = getItem(position);

        View view;
        ViewHolder viewHolder;

        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(mResourceId, null);

            viewHolder = new ViewHolder();
            viewHolder.alias = view.findViewById(R.id.text_phrase_alias);
            viewHolder.creationTime = view.findViewById(R.id.text_phrase_creation_time);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        if (info != null) {
            viewHolder.alias.setText(info.alias.isEmpty() ? "mnemonic" : info.alias);
            viewHolder.creationTime.setText(Integer.toString(info.creationTime));
        }

        return view;
    }

    class ViewHolder{
        TextView alias;
        TextView creationTime;
    }
}

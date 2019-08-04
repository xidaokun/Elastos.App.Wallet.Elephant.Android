package com.breadwallet.tools.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.security.PhraseInfo;

import java.util.Date;
import java.util.List;

public class PhraseAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int FOOTER_VIEW = 1;

    private List<PhraseInfo> mList;
    private List<Boolean> mBackupList;
    private WalletCardListener mListener;
    private Context mContext;

    public PhraseAdapter(@NonNull Context context, @NonNull List<PhraseInfo> objects, @NonNull List<Boolean> backupList, WalletCardListener listener) {
        mContext = context;
        mList = objects;
        mBackupList = backupList;
        mListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == FOOTER_VIEW) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.phrase_footer_view, null);
            return new FooterViewHolder(view);

        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.phrase_item, null);
            return new ViewHolder(view);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof FooterViewHolder) {
            FooterViewHolder footer = (FooterViewHolder)holder;
            footer.card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.OnNewClick(v);
                }
            });
        } else {
            ViewHolder viewHolder = (ViewHolder)holder;
            PhraseInfo info = mList.get(position);

            Boolean backup = mBackupList.get(position);
            viewHolder.alias.setText(info.alias.isEmpty() ? "mnemonic" : info.alias);
            viewHolder.creationTime.setText(getTimeString(info.creationTime));

            if (info.selected) {
                viewHolder.card.setBackground(mContext.getDrawable(R.drawable.cardbgseleced));
                viewHolder.selected.setVisibility(View.VISIBLE);

                viewHolder.root.setCardBackgroundColor(mContext.getColor(R.color.pin_pad_delete));
            } else {
                viewHolder.card.setBackground(mContext.getDrawable(R.drawable.cardbgnoraml));
                viewHolder.selected.setVisibility(View.INVISIBLE);
                viewHolder.root.setCardBackgroundColor(mContext.getColor(R.color.wallet_normal_color));
            }

            viewHolder.walletBackup.setVisibility(backup ? View.GONE : View.VISIBLE);

            viewHolder.card.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.OnItemClick(v, position);
                }
            });
            viewHolder.aliasLable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.OnEditNameClick(v, position);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mList.size()) {
            // This is where we'll add footer.
            return FOOTER_VIEW;
        }

        return super.getItemViewType(position);
    }

    public PhraseInfo getItem(int position) {
        if (position < 0 || position >= mList.size()) return null;

        return mList.get(position);
    }

    private String getTimeString(long time) {
        if (time == 0) return "0";
        String dateFormat = mContext.getString(R.string.multi_wallet_format_date);
        Date date = new Date(time * 1000);
        return DateFormat.format(dateFormat, date).toString();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        CardView root;
        RelativeLayout card;
        TextView alias;
        TextView creationTime;
        LinearLayout aliasLable;
        ImageView selected;
        LinearLayout walletBackup;

        public ViewHolder(View itemView) {
            super(itemView);
            root = (CardView) itemView;
            card = itemView.findViewById(R.id.wallet_card);
            aliasLable = itemView.findViewById(R.id.wallet_name_label);
            alias = itemView.findViewById(R.id.wallet_name);
            creationTime = itemView.findViewById(R.id.wallet_time);
            selected = itemView.findViewById(R.id.wallet_selected);
            walletBackup = itemView.findViewById(R.id.wallet_backup);
        }

    }

    class FooterViewHolder extends RecyclerView.ViewHolder {
        LinearLayout card;

        public FooterViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.new_wallet_card);
        }

    }

    public interface WalletCardListener {
        void OnItemClick(View v, int position);
        void OnEditNameClick(View v, int position);
        void OnNewClick(View v);
    }
}

package com.breadwallet.tools.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRDateUtil;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.ela.ElaDataUtils;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/27/15.
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

public class TransactionListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final String TAG = TransactionListAdapter.class.getName();

    private final Context mContext;
    private final int mTxResourceId;
    private List<TxUiHolder> backUpFeed;
    private List<TxUiHolder> itemFeed;
    private Map<Integer, TxMetaData> mMetaDatas;
    private int mLastVisibleItemPosition;

    private final int TYPE_ITEM = 0;
    private final int TYPE_FOOTER = 1;

    public static final int LOADING = 1;
    public static final int LOAD_COMPLETE = 2;
    public static final int LOAD_NO_MORE = 3;
    private int mLoadState = LOADING;

    private boolean mIsUpdatingData;

    public TransactionListAdapter(Context context, RecyclerView recyclerView, List<TxUiHolder> items) {
        this.mTxResourceId = R.layout.tx_item;
        this.mContext = context;
        backUpFeed = items;
        itemFeed = items;
        mMetaDatas = new HashMap<>();
        items = new ArrayList<>();
        init(items);
        if(null != recyclerView){
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                private boolean canLoadMore = false;
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if(newState == RecyclerView.SCROLL_STATE_IDLE){
                        LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                        int lastItemPosition = manager.findLastCompletelyVisibleItemPosition();
                        int itemCount = manager.getItemCount();
                        if(lastItemPosition==(itemCount-1) && canLoadMore){
                            if(mLoadMoreListener != null) mLoadMoreListener.loadMore();
                        }
                    }
                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    BaseWalletManager walletManager = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);
                    String iso = walletManager.getIso();
                    if(!StringUtil.isNullOrEmpty(iso) && !iso.equalsIgnoreCase("ELA")) {
                        canLoadMore = false;
                    } else {
                        canLoadMore = dy > 0;
                    }
                }
            });
        }
    }

    public void setItems(List<TxUiHolder> items) {
        init(items);
    }

    private void init(List<TxUiHolder> items) {
        if (items == null) {
            items = new ArrayList<>();
        }
        if (itemFeed == null) {
            itemFeed = new ArrayList<>();
        }
        if (backUpFeed == null) {
            backUpFeed = new ArrayList<>();
        }
        this.itemFeed = items;
        this.backUpFeed = items;

    }

    public void clearData() {
        if (null != itemFeed) itemFeed.clear();
        if (null != backUpFeed) backUpFeed.clear();
    }

    public void updateData() {
        if (mIsUpdatingData) {
            return;
        }
        Map<Integer, TxMetaData> localMDs = new HashMap<>();
        BaseWalletManager wm = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);
        for (int i = 0; i < backUpFeed.size(); i++) {
            TxUiHolder item = backUpFeed.get(i);
            TxMetaData md = KVStoreManager.getInstance().getTxMetaData(mContext, item.getTxHash());
            if (System.currentTimeMillis() - item.getTimeStamp() < DateUtils.HOUR_IN_MILLIS) {
                if (md == null) {
                    md = KVStoreManager.getInstance().createMetadata(mContext, wm,
                            new CryptoTransaction(item.getTransaction()));
                    KVStoreManager.getInstance().putTxMetaData(mContext, md, item.getTxHash());
                } else if (md.exchangeRate == 0) {
                    md.exchangeRate = wm.getFiatExchangeRate(mContext).doubleValue();
                    md.exchangeCurrency = BRSharedPrefs.getPreferredFiatIso(mContext);
                    Log.d(TAG, "MetaData not null");
                    KVStoreManager.getInstance().putTxMetaData(mContext, md, item.getTxHash());
                }
            }
            localMDs.put(i, md);

        }
        mMetaDatas.clear();
        mMetaDatas.putAll(localMDs);
        mIsUpdatingData = false;
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });

    }

    public List<TxUiHolder> getItems() {
        return itemFeed;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_FOOTER) {
            return new FooterViewHolder(inflater.inflate(R.layout.swipe_refresh_layout, parent, false));
        }
        // inflate the layout
        return new TxHolder(inflater.inflate(mTxResourceId, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
//        if (holder.getItemViewType() == TYPE_ITEM) {
//            holder.setIsRecyclable(false);
//            setTexts((TxHolder) holder, position);
//        }

        if (holder instanceof TxHolder) {
            holder.setIsRecyclable(false);
            setTexts((TxHolder) holder, position);
        } else if (holder instanceof FooterViewHolder) {
            FooterViewHolder footViewHolder = (FooterViewHolder) holder;
            switch (mLoadState) {
                case LOADING:
                    footViewHolder.mProgress.setVisibility(View.VISIBLE);
                    footViewHolder.mHint.setText(mContext.getString(R.string.history_loading_hint));
                    break;
                case LOAD_COMPLETE:
                    footViewHolder.mProgress.setVisibility(View.INVISIBLE);
                    footViewHolder.mHint.setText(mContext.getString(R.string.history_pull_refresh_hint));
                    break;
                case LOAD_NO_MORE:
                    footViewHolder.mProgress.setVisibility(View.INVISIBLE);
                    footViewHolder.mHint.setText(mContext.getString(R.string.history_load_no_data));
                    break;
                default:
                    break;

            }
        }

    }

    public void setLoadState(int loadState){
        this.mLoadState = loadState;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        BaseWalletManager wm = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);
        if (wm == null) return TYPE_ITEM;
        String iso = wm.getIso();
        if (!StringUtil.isNullOrEmpty(iso) && iso.equalsIgnoreCase("ELA")) {
            if (position + 1 == getItemCount()) {
                return TYPE_FOOTER;
            }
        }

        return TYPE_ITEM;
    }

    @Override
    public int getItemCount() {
        BaseWalletManager wm = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);
        if (wm == null) itemFeed.size();
        String iso = wm.getIso();
        return (!StringUtil.isNullOrEmpty(iso) && iso.equalsIgnoreCase("ELA")) ? itemFeed.size() + 1 : itemFeed.size();
    }

    private void setTexts(final TxHolder convertView, int position) {
        BaseWalletManager wm = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);
        TxUiHolder item = itemFeed.get(position);

        String commentString = "";
        TxMetaData md = mMetaDatas.size() > position ? mMetaDatas.get(position) : null;
        if (md != null) {
            if (md.comment != null) {
                commentString = md.comment;
            }
        }

        boolean received = item.isReceived();
        int amountColor = received ? R.color.transaction_amount_received_color : R.color.total_assets_usd_color;

        convertView.transactionAmount.setTextColor(mContext.getResources().getColor(amountColor, null));

        // If this transaction failed, show the "FAILED" indicator in the cell
        if (!item.isValid())
            showTransactionFailed(convertView, item, received);

        BigDecimal cryptoAmount = item.getAmount()/*.abs()*/;

        BREthereumToken tkn = null;
        if (wm.getIso().equalsIgnoreCase("ETH"))
            tkn = WalletEthManager.getInstance(mContext).node.lookupToken(item.getTo());
        // it's a token transfer ETH tx
        if (tkn != null) {
            cryptoAmount = item.getFee();
        }
        boolean isCryptoPreferred = BRSharedPrefs.isCryptoPreferred(mContext);
        String preferredIso = isCryptoPreferred ? wm.getIso() : BRSharedPrefs.getPreferredFiatIso(mContext);
        BigDecimal amount = isCryptoPreferred ? cryptoAmount : wm.getFiatForSmallestCrypto(mContext, cryptoAmount, null);
        if (!received && amount != null) {
            amount = amount.negate();
        }
        String formattedAmount = CurrencyUtils.getFormattedAmount(mContext, preferredIso, amount, wm.getUiConfiguration().getMaxDecimalPlacesForUi());
        convertView.transactionAmount.setText(formattedAmount);

        String type = item.getType();
        String txType = item.getTxType();
        convertView.transactionDposFlag.setVisibility(View.GONE);
        convertView.transactionCrcFlag.setVisibility(View.GONE);
        if(!StringUtil.isNullOrEmpty(type) && !StringUtil.isNullOrEmpty(txType)) {
            if(type.equals("spend")) {
                if(ElaDataUtils.getVoteType(type, txType) == 1) {
                    convertView.transactionDposFlag.setVisibility(View.VISIBLE);
                    convertView.transactionCrcFlag.setVisibility(View.GONE);
                } else if(ElaDataUtils.getVoteType(type, txType) == 2) {
                    convertView.transactionDposFlag.setVisibility(View.GONE);
                    convertView.transactionCrcFlag.setVisibility(View.VISIBLE);
                } else if(ElaDataUtils.getVoteType(type, txType) == 3) {
                    convertView.transactionDposFlag.setVisibility(View.VISIBLE);
                    convertView.transactionCrcFlag.setVisibility(View.VISIBLE);
                }
            }
        }

        int blockHeight = item.getBlockHeight();
        int lastBlockHeight = BRSharedPrefs.getLastBlockHeight(mContext, wm.getIso());
        int confirms = blockHeight == Integer.MAX_VALUE ? 0 : lastBlockHeight - blockHeight + 1;
        int level;
        if (confirms <= 0) {
            long relayCount = wm.getRelayCount(item.getTxHash());
            if (relayCount <= 0)
                level = 0;
            else if (relayCount == 1)
                level = 1;
            else
                level = 2;
        } else {
            if (confirms >= 4) {
                level = 6;
            } else {
                level = confirms + 2;
            }
        }

        //已发送至
        String sentTo = mContext.getResources().getString(R.string.Transaction_sentTo);
        //已通过
        String receivedVia = mContext.getResources().getString(R.string.TransactionDetails_receivedVia);
        //正在发送至
        String sendingTo = mContext.getResources().getString(R.string.Transaction_sendingTo);
        //正在通过 接收
        String receivingVia = mContext.getResources().getString(R.string.TransactionDetails_receivingVia);

        if (item.isReceived()) {
            convertView.transactionIcon.setBackgroundResource(R.drawable.ellipse_receive);
            String from = item.getFrom();
            convertView.transactionDetail.setText(StringUtil.isNullOrEmpty(from) ? item.getTo() : from);
        } else {
            convertView.transactionIcon.setBackgroundResource(R.drawable.ellipse_send);
            convertView.transactionDetail.setText(item.getTo());
        }

        if (level > 4) {
            convertView.transactionStatus.setText(!received ? sentTo : receivedVia);
            convertView.transactionStatus.setTextColor(mContext.getColor(!received ? R.color.tx_send_color : R.color.transaction_amount_received_color));
        } else {
            convertView.transactionStatus.setText(!received ? sendingTo : receivingVia);
            convertView.transactionStatus.setTextColor(mContext.getColor(!received ? R.color.total_assets_usd_color : R.color.transaction_amount_received_color));
        }
        if (wm.getIso().equalsIgnoreCase("ELA") || wm.getIso().equalsIgnoreCase("IOEX")) {
            String status = item.getStatus();
            if (level==0 && (status==null || status.equalsIgnoreCase("confirmed"))) {
                convertView.transactionStatus.setText(!received ? sentTo : receivedVia);
                convertView.transactionStatus.setTextColor(mContext.getColor(!received ? R.color.tx_send_color : R.color.transaction_amount_received_color));
            } else {
                convertView.transactionStatus.setText(!received ? sendingTo : receivingVia);
                convertView.transactionIcon.setBackgroundResource(!received ? R.drawable.ellipse_sending : R.drawable.ellipse_receive);
                convertView.transactionStatus.setTextColor(mContext.getColor(!received ? R.color.total_assets_usd_color : R.color.transaction_amount_received_color));
            }
        }
        if (tkn != null) // it's a token transfer ETH tx
            convertView.transactionDetail.setText(String.format(mContext.getString(R.string.Transaction_tokenTransfer), tkn.getSymbol()));

        //if it's 0 we use the current time.
        long timeStamp = item.getTimeStamp() == 0 ? System.currentTimeMillis() : item.getTimeStamp() * DateUtils.SECOND_IN_MILLIS;

        String shortDate = BRDateUtil.getFullDate(timeStamp);

        convertView.transactionDate.setText(shortDate);
    }

    private void showTransactionProgress(TxHolder holder, int progress) {
        if (progress < 100) {
            holder.transactionProgress.setVisibility(View.VISIBLE);
            holder.transactionDate.setVisibility(View.GONE);
            holder.transactionProgress.setProgress(progress);
            RelativeLayout.LayoutParams detailParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            detailParams.addRule(RelativeLayout.RIGHT_OF, holder.transactionProgress.getId());
            detailParams.addRule(RelativeLayout.CENTER_VERTICAL);
            detailParams.setMargins(Utils.getPixelsFromDps(mContext, 16), Utils.getPixelsFromDps(mContext, 36), 0, 0);
            holder.transactionDetail.setLayoutParams(detailParams);
            holder.transactionDetail.setMaxWidth(Utils.getPixelsFromDps(mContext, 120));
        } else {
            holder.transactionProgress.setVisibility(View.INVISIBLE);
            holder.transactionDate.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams startingParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            startingParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            startingParams.addRule(RelativeLayout.CENTER_VERTICAL);
            startingParams.setMargins(Utils.getPixelsFromDps(mContext, 16), 0, 0, 0);
            holder.transactionDetail.setLayoutParams(startingParams);
            holder.setIsRecyclable(true);
        }
    }

    private void showTransactionFailed(TxHolder holder, TxUiHolder tx, boolean received) {
        holder.transactionDate.setVisibility(View.INVISIBLE);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.RIGHT_OF, holder.transactionFailed.getId());
        params.setMargins(16, 0, 0, 0);
        params.addRule(RelativeLayout.CENTER_VERTICAL, holder.transactionFailed.getId());
        holder.transactionDetail.setLayoutParams(params);
        BaseWalletManager wm = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);

        if (!received) {
            holder.transactionDetail.setText(String.format(mContext.getString(R.string.Transaction_sendingTo), wm.decorateAddress(tx.getTo())));
        }

    }

    public void filterBy(String query, boolean[] switches) {
        filter(query, switches);
    }

    public void resetFilter() {
        itemFeed = backUpFeed;
        notifyDataSetChanged();
    }

    private void filter(final String query, final boolean[] switches) {
        String lowerQuery = query.toLowerCase().trim();
        if (Utils.isNullOrEmpty(lowerQuery) && !switches[0] && !switches[1] && !switches[2] && !switches[3]) {
            return;
        }
        int switchesON = 0;
        for (boolean i : switches) {
            if (i) {
                switchesON++;
            }
        }

        final List<TxUiHolder> filteredList = new ArrayList<>();
        TxUiHolder item;
        for (int i = 0; i < backUpFeed.size(); i++) {
            item = backUpFeed.get(i);
            boolean matchesHash = item.getHashReversed() != null && item.getHashReversed().contains(lowerQuery);
            boolean matchesAddress = item.getFrom().contains(lowerQuery) || item.getTo().contains(lowerQuery);
            boolean matchesMemo = item.metaData != null && item.metaData.comment != null && item.metaData.comment.toLowerCase().contains(lowerQuery);
            if (matchesHash || matchesAddress || matchesMemo) {
                if (switchesON == 0) {
                    filteredList.add(item);
                } else {
                    boolean willAdd = true;
                    //filter by sent and this is received
                    if (switches[0] && item.isReceived()) {
                        willAdd = false;
                    }
                    //filter by received and this is sent
                    if (switches[1] && !item.isReceived()) {
                        willAdd = false;
                    }
                    BaseWalletManager wallet = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);

                    int confirms = item.getBlockHeight() == Integer.MAX_VALUE ? 0
                            : BRSharedPrefs.getLastBlockHeight(mContext, wallet.getIso()) - item.getBlockHeight() + 1;
                    //complete
                    if (switches[2] && confirms >= 6) {
                        willAdd = false;
                    }

                    //pending
                    if (switches[3] && confirms < 6) {
                        willAdd = false;
                    }

                    if (willAdd) filteredList.add(item);
                }

            }

        }
        itemFeed = filteredList;
        notifyDataSetChanged();
    }

    private class FooterViewHolder extends RecyclerView.ViewHolder {
        private View mProgress;
        private BaseTextView mHint;

        public FooterViewHolder(View itemView) {
            super(itemView);
            mProgress = itemView.findViewById(R.id.loading_progress);
            mHint = itemView.findViewById(R.id.load_state_tv);
        }
    }

    private class TxHolder extends RecyclerView.ViewHolder {
        public ConstraintLayout constraintLayout;
        public TextView amount;
        public TextView account;
        public TextView status;
        public TextView timestamp;
        public TextView comment;

        public BaseTextView transactionIcon;
        public BaseTextView transactionDate;
        public BaseTextView transactionAmount;
        public BaseTextView transactionDetail;
        public BaseTextView transactionStatus;
        public BaseTextView transactionDposFlag;
        public BaseTextView transactionCrcFlag;
        public Button transactionFailed;
        public ProgressBar transactionProgress;


        public TxHolder(View view) {
            super(view);

            transactionDate = view.findViewById(R.id.tx_date);
            transactionAmount = view.findViewById(R.id.tx_amount);
            transactionDetail = view.findViewById(R.id.tx_description);
            transactionStatus = view.findViewById(R.id.tx_status);
            transactionFailed = view.findViewById(R.id.tx_failed_button);
            transactionProgress = view.findViewById(R.id.tx_progress);
            transactionIcon = view.findViewById(R.id.tx_status_icon);
            transactionDposFlag = view.findViewById(R.id.dpos_vote_flag);
            transactionCrcFlag = view.findViewById(R.id.crc_vote_flag);
        }
    }

    private LoadMoreListener mLoadMoreListener = null;

    public void setLoadMoreListener(LoadMoreListener listener) {
        this.mLoadMoreListener = listener;
    }

    public interface LoadMoreListener {
        void loadMore();
    }

}
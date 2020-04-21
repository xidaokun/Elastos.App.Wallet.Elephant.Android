package com.breadwallet.wallet.wallets.ioex;

import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.core.BRCoreChainParams;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreWalletManager;
import com.breadwallet.core.ethereum.BREthereumAmount;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.TxManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.IoexDataSource;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.SettingsUtil;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.breadwallet.wallet.wallets.CryptoAddress;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.WalletManagerHelper;
import com.breadwallet.wallet.wallets.ela.BRElaTransaction;
import com.breadwallet.wallet.wallets.ela.ElaDataSource;
import com.breadwallet.wallet.wallets.ela.data.HistoryTransactionEntity;
import com.elastos.jni.Utility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class WalletIoexManager extends BRCoreWalletManager implements BaseWalletManager {

    private static final String TAG = WalletIoexManager.class.getSimpleName();

    public static final String ONE_IOEX_IN_SATOSHI = "100000000"; // 1 ela in sala, 100 millions
    public static final String MAX_IOEX = "10000000000"; //Max amount in ela
    public static final String ELA_SYMBOL = "IOEX";
    private static final String SCHEME = "ioex";
    private static final String NAME = "Ioex";
    private static final String IOEX_ADDRESS_PREFIX = "E";


    public static final BigDecimal ONE_IOEX_TO_SATOSHI = new BigDecimal(ONE_IOEX_IN_SATOSHI);

    private static WalletIoexManager mInstance;

    private final BigDecimal MAX_SALA = new BigDecimal(MAX_IOEX);

    private final BigDecimal MIN_SALA = new BigDecimal("0");

    private WalletSettingsConfiguration mSettingsConfig;
    private WalletUiConfiguration mUiConfig;

    private WalletManagerHelper mWalletManagerHelper;

    protected String mPrivateKey;

    private static Context mContext;

    public static WalletIoexManager getInstance(Context context) {

        if (mInstance == null) {
            mInstance = new WalletIoexManager(context, null, null, 0);
        }

        return mInstance;
    }

    private WalletIoexManager(Context context, BRCoreMasterPubKey masterPubKey,
                             BRCoreChainParams chainParams,
                             double earliestPeerTime) {
        super(masterPubKey, chainParams, 0);
        mContext = context;
        mUiConfig = new WalletUiConfiguration("#003d79", null,
                true, WalletManagerHelper.MAX_DECIMAL_PLACES);

        mSettingsConfig = new WalletSettingsConfiguration(context, getIso(), SettingsUtil.getElastosSettings(mContext), new ArrayList<BigDecimal>(0));

        mWalletManagerHelper = new WalletManagerHelper();
    }


    @Override
    public int getForkId() {
        Log.i(TAG, "getForkId");
        return -1;
    }

    @Override
    public BREthereumAmount.Unit getUnit() {
        Log.i(TAG, "getUnit");
        return BREthereumAmount.Unit.TOKEN_DECIMAL;
    }

    public String getPrivateKey() {
        if (mPrivateKey == null) {
            try {
                byte[] phrase = BRKeyStore.getPhrase(mContext, 0);
                mPrivateKey = Utility.getInstance(mContext).getSinglePrivateKey(new String(phrase));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mPrivateKey;
    }

    public String getPublicKey(){
        try {
            byte[] phrase = BRKeyStore.getPhrase(mContext, 0);
            return Utility.getInstance(mContext).getSinglePublicKey(new String(phrase));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String mAddress;
    @Override
    public String getAddress() {
        if (StringUtil.isNullOrEmpty(mAddress)) {
            String publickey = getPublicKey();
            if(publickey != null) {
                mAddress = Utility.getInstance(mContext).getAddress(publickey);
            }
        }

        return mAddress;
    }

    @Override
    public boolean isAddressValid(String address) {
        return !Utils.isNullOrEmpty(address) && (address.startsWith(IOEX_ADDRESS_PREFIX)
                || address.startsWith("8"));
    }

    @Override
    public byte[] signAndPublishTransaction(CryptoTransaction tx, byte[] seed) {
        Log.i(TAG, "signAndPublishTransaction");
        try {
            if(tx == null) return new byte[1];
            BRIoexTransaction raw = tx.getIoexTx();
            if(raw == null) return new byte[1];
            String rawTxTxid = IoexDataSource.getInstance(mContext).sendIoexRawTx(raw.getTx());

            if(StringUtil.isNullOrEmpty(rawTxTxid)) return new byte[1];
            TxManager.getInstance().updateTxList(mContext);
            return rawTxTxid.getBytes();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new byte[1];
    }

    @Override
    public void addBalanceChangedListener(OnBalanceChangedListener list) {
        mWalletManagerHelper.addBalanceChangedListener(list);
    }

    @Override
    public void onBalanceChanged(BigDecimal balance) {
        Log.i(TAG, "onBalanceChanged");
        mWalletManagerHelper.onBalanceChanged(balance);
    }

    @Override
    public void addSyncListener(SyncListener listener) {
        Log.i(TAG, "addSyncListener");
    }

    @Override
    public void removeSyncListener(SyncListener listener) {
        Log.i(TAG, "removeSyncListener");
    }

    @Override
    public void addTxListModifiedListener(OnTxListModified list) {
        Log.i(TAG, "addTxListModifiedListener");
        mWalletManagerHelper.addTxListModifiedListener(list);
    }

    @Override
    public void watchTransactionForHash(CryptoTransaction tx, OnHashUpdated listener) {
        Log.i(TAG, "watchTransactionForHash");
    }

    @Override
    public long getRelayCount(byte[] txHash) {
        Log.i(TAG, "getRelayCount");
        return 0;
    }

    private static double time = 0;

    @Override
    public double getSyncProgress(long startHeight) {
        Log.i(TAG, "getSyncProgress");
//        time += 0.1;
//        if(time >= 1.0) time = 1.0;
        return /*time*/1.0;
    }

    @Override
    public double getConnectStatus() {
        Log.i(TAG, "getConnectStatus");
        return 2;
    }

    @Override
    public void connect(Context app) {
        Log.i(TAG, "connect");
    }

    @Override
    public void disconnect(Context app) {
        Log.i(TAG, "disconnect");
    }

    @Override
    public boolean useFixedNode(String node, int port) {
        Log.i(TAG, "useFixedNode");
        return false;
    }

    @Override
    public void rescan(Context app) {
        Log.i(TAG, "rescan");
    }

    @Override
    public CryptoTransaction[] getTxs(Context app) {
        Log.i(TAG, "getTxs");
        return new CryptoTransaction[0];
    }

    private static final BigDecimal ELA_FEE = new BigDecimal(100).divide(ONE_IOEX_TO_SATOSHI, 8, BRConstants.ROUNDING_MODE);

    @Override
    public BigDecimal getTxFee(CryptoTransaction tx) {
        Log.i(TAG, "getTxFee");
        return ELA_FEE;
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        return ELA_FEE;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return ELA_FEE;
    }

    @Override
    public String getTxAddress(CryptoTransaction tx) {
        return getAddress();
    }

    @Override
    public BigDecimal getMaxOutputAmount(Context app) {
        BigDecimal balance = getCachedBalance(app);
        if (balance.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if (ELA_FEE.compareTo(balance) > 0) return BigDecimal.ZERO;
        return balance.subtract(ELA_FEE);
    }

    @Override
    public BigDecimal getMinOutputAmount(Context app) {
        return MIN_SALA;
    }

    @Override
    public BigDecimal getTransactionAmount(CryptoTransaction tx) {
        Log.i(TAG, "getTransactionAmount");
        return new BigDecimal(getWallet().getTransactionAmount(tx.getCoreTx()));
    }

    @Override
    public BigDecimal getMinOutputAmountPossible() {
        return new BigDecimal(BRCoreTransaction.getMinOutputAmount());
    }

    @Override
    public void updateFee(Context app) {

    }

    @Override
    public void refreshAddress(Context app) {

    }

    @Override
    public void refreshCachedBalance(final Context app) {
        Log.i(TAG, "refreshCachedBalance");
        try {
            String address = getAddress();
            Log.i("balance_test", "address:"+address);
            if(address == null) return;
            String balance = IoexDataSource.getInstance(mContext).getBalance(address);
            if(balance == null) return;
            final BigDecimal tmp = new BigDecimal((balance == null || balance.equals("")) ? "0" : balance);
            BRSharedPrefs.putCachedBalance(app, getIso(), tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTxHistory() {
        String address = getAddress();
        if(StringUtil.isNullOrEmpty(address)) return;
        IoexDataSource.getInstance(mContext).getHistory(address);
        TxManager.getInstance().updateTxList(mContext);
    }

    @Override
    public List<TxUiHolder> getTxUiHolders(Context app) {
        List<TxUiHolder> uiTxs = new ArrayList<>();
        List<HistoryTransactionEntity> transactionEntities = IoexDataSource.getInstance(mContext).getHistoryTransactions();
        try {
            for (HistoryTransactionEntity entity : transactionEntities) {
                BigDecimal fee = new BigDecimal(entity.fee).divide(ONE_IOEX_TO_SATOSHI, 8, BRConstants.ROUNDING_MODE);
                BigDecimal amount = new BigDecimal(entity.amount).divide(ONE_IOEX_TO_SATOSHI, 8, BRConstants.ROUNDING_MODE);
                TxUiHolder txUiHolder = new TxUiHolder(null,
                        entity.isReceived,
                        entity.timeStamp,
                        entity.blockHeight,
                        entity.hash,
                        entity.txReversed,
                        fee,
                        entity.toAddress,
                        entity.fromAddress,
                        new BigDecimal(String.valueOf(entity.balanceAfterTx))
                        ,entity.txSize
                        ,amount
                        , entity.isValid
                        ,entity.status);
                txUiHolder.memo = entity.memo;
                uiTxs.add(txUiHolder);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uiTxs;
    }

    @Override
    public boolean containsAddress(String address) {
        String addr = getAddress();
        return addr.equals(address);
    }

    @Override
    public boolean addressIsUsed(String address) {
        return false;
    }

    @Override
    public boolean generateWallet(Context app) {
        //no need, one key for all wallets so far
        return true;
    }

    @Override
    public String getSymbol(Context app) {
        return "IOEX";
    }

    @Override
    public String getIso() {
        return "IOEX";
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDenominator() {
        return String.valueOf(ONE_IOEX_IN_SATOSHI);
    }

    @Override
    public CryptoAddress getReceiveAddress(Context app) {
        return new CryptoAddress(getAddress(), null);
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address) {
        return null;
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address, String meno) {
        Log.i(TAG, "createTransaction");
        BRIoexTransaction brElaTransaction = IoexDataSource.getInstance(mContext).createTx(getAddress(), address, amount.multiply(ONE_IOEX_TO_SATOSHI).longValue(), meno);

        return new CryptoTransaction(brElaTransaction);
    }

    @Override
    public String decorateAddress(String addr) {
        return addr;
    }

    @Override
    public String undecorateAddress(String addr) {
        return addr;
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        return WalletManagerHelper.MAX_DECIMAL_PLACES;
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        Log.i(TAG, "getCachedBalance");
        if(app == null) return new BigDecimal(0);
        return BRSharedPrefs.getCachedBalance(app, getIso());
    }

    //TODO wait
    @Override
    public BigDecimal getTotalSent(Context app) {
        Log.i(TAG, "getTotalSent");
        return BigDecimal.ZERO;
    }

    @Override
    public void wipeData(Context app) {
        BRSharedPrefs.putCachedBalance(app, getIso(),  new BigDecimal(0));
        IoexDataSource.getInstance(app).deleteAllTransactions();
        mPrivateKey = null;
        mAddress = null;
        mInstance = null;
    }

    @Override
    public void syncStarted() {
        Log.i(TAG, "syncStarted");
        mWalletManagerHelper.onSyncStarted();
    }

    @Override
    public void syncStopped(String error) {
        Log.i(TAG, "syncStopped");
        mWalletManagerHelper.onSyncStopped(error);
    }

    @Override
    public boolean networkIsReachable() {
        Log.i(TAG, "networkIsReachable");
        Context app = BreadApp.getBreadContext();
        return InternetManager.getInstance().isConnected(app);
    }

    @Override
    public void setCachedBalance(Context app, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(), balance);
    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        return MAX_SALA;
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return mUiConfig;
    }

    @Override
    public WalletSettingsConfiguration getSettingsConfiguration() {
        return mSettingsConfig;
    }

    private BigDecimal getFiatForEla(Context app, BigDecimal elaAmount, String code) {//总资产
        if (elaAmount == null || elaAmount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        //fiat rate for btc
        CurrencyEntity btcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, "BTC", code);
        //Btc rate for ela
        CurrencyEntity elaBtcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), "BTC");
        if (btcRate == null) {
            return null;
        }
        if (elaBtcRate == null) {
            return null;
        }

        return elaAmount.multiply(new BigDecimal(elaBtcRate.rate)).multiply(new BigDecimal(btcRate.rate));
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {//行情价格
        BigDecimal fiatData = getFiatForEla(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        if (fiatData == null) new BigDecimal(0);
        return fiatData; //dollars
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        if(app == null) return new BigDecimal(0);
        BigDecimal amount = BRSharedPrefs.getCachedBalance(app, getIso());
        return getFiatForEla(app, amount, BRSharedPrefs.getPreferredFiatIso(app));
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        BigDecimal fiatData = getFiatForEla(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        if(fiatData==null || fiatData.doubleValue()==0) return new BigDecimal(0);
        return amount.multiply(fiatData);
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal amount) {
        if(amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        BigDecimal fiatData = getFiatForEla(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        return amount.multiply(fiatData);
    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        return amount;
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        return amount;
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        BigDecimal fiatData = getFiatForEla(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        if(fiatData == null) return new BigDecimal(0);
        if(fiatData.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        BigDecimal tmp = amount.divide(fiatData, 8, BRConstants.ROUNDING_MODE);
        return tmp;
    }
}

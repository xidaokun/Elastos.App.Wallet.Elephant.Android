package com.breadwallet.tools.util;

import java.math.RoundingMode;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 2/16/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public final class BRConstants {

    /**
     * Native library name
     */
    public static final String NATIVE_LIB_NAME = "core";

    /**
     * Permissions
     */
    public static final int CAMERA_REQUEST_ID = 34;
    public static final int GEO_REQUEST_ID = 35;
    public static final int CAMERA_REQUEST_GLIDERA_ID = 36;

    /**
     * Request codes for auth
     */
    public static final int SHOW_PHRASE_REQUEST_CODE = 111;
    public static final int PAY_REQUEST_CODE = 112;
    public static final int CANARY_REQUEST_CODE = 113;
    public static final int PUT_PHRASE_NEW_WALLET_REQUEST_CODE = 114;
    public static final int PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE = 115;
    public static final int PAYMENT_PROTOCOL_REQUEST_CODE = 116;
    public static final int REQUEST_PHRASE_BITID = 117;
    public static final int PROVE_PHRASE_REQUEST = 119;
    public static final int UPLOAD_FILE_REQUEST = 120;
    public static final int SIGN_PURPOSE_REQUEST = 121;
    public static final int SIGN_CONTENT_REQUEST = 122;

    /**
     * Request codes for take picture
     */
    public static final int SCANNER_REQUEST = 201;
    public static final int REQUEST_IMAGE_CAPTURE = 203;
    public static final int SCANNER_DID_REQUEST = 204;
    public static final int ADD_APP_URL_REQUEST = 205;

    public static final String CANARY_STRING = "canary";


    public static final int PROFILE_REQUEST_NICKNAME = 301;
    public static final int PROFILE_REQUEST_EMAIL = 302;
    public static final int PROFILE_REQUEST_MOBILE = 303;
    public static final int PROFILE_REQUEST_ID = 304;

    /**
     * Currency units
     */
    public static final int CURRENT_UNIT_BITS = 0;
    public static final int CURRENT_UNIT_MBITS = 1;
    public static final int CURRENT_UNIT_BITCOINS = 2;

    public static final String BITS_SYMBOL = "\u0180";
    public static final String ETH_SYMBOL = "\u039E";
    public static final String BITCOIN_SYBMOL_OLD = "\u0243";
    public static final String BITCOIN_SYMBOL = "\u20BF";

    public static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;
    public static final boolean WRITE_AHEAD_LOGGING = true;

    /**
     * Support Center article ids.
     */
    public static final String FAQ_DISPLAY_CURRENCY = "display-currency";
    public static final String FAQ_RECOVER_WALLET = "recover-wallet";
    public static final String FAQ_RESCAN = "re-scan";
    public static final String FAQ_SECURITY_CENTER = "security-center";
    public static final String FAQ_PAPER_KEY = "paper-key";
    public static final String FAQ_ENABLE_FINGERPRINT = "enable-fingerprint-authentication";
    public static final String FAQ_FINGERPRINT_SPENDING_LIMIT = "fingerprint-spending-limit";
    public static final String FAQ_TRANSACTION_DETAILS = "transaction-details";
    public static final String FAQ_RECEIVE = "receive-bitcoin";
    public static final String FAQ_REQUEST_AMOUNT = "request-amount";
    public static final String FAQ_SEND = "send-bitcoin";
    public static final String FAQ_WALLET_DISABLE = "wallet-disabled";
    public static final String FAQ_RESET_PIN_WITH_PAPER_KEY = "reset-pin-paper-key";
    public static final String FAQ_SET_PIN = "set-pin";
    public static final String FAQ_IMPORT_WALLET = "import-wallet";
    public static final String FAQ_WRITE_PAPER_KEY = "write-phrase";
    public static final String FAQ_START_VIEW = "start-view";
    public static final String FAQ_WIPE_WALLET = "wipe-wallet";
    public static final String FAQ_LOOP_BUG = "android-loop-bug";
    public static final String FAQ_BCH = "bitcoin-cash";

    /**
     * API Constants
     */

    public static final String HTTPS_PROTOCOL = "https://";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_VALUE_CONTENT_TYPE = "application/json; charset=utf-8";
    public static final String HEADER_VALUE_ACCEPT = "application/json";


    /**
     * Extra constants
     */

    public static final String EXTRA_URL = "com.breadwallet.EXTRA_URL";
    public static final String DRAWABLE = "drawable";
    public static final String CURRENCY_PARAMETER_STRING_FORMAT = "%s?currency=%s";

    /**
     * Social media links and privacy policy URLS
     *
     */

    public static final String URL_PRIVACY_POLICY = "https://elephantwallet.app/privacy-policy/";
    public static final String URL_TWITTER = "https://twitter.com/breadapp";
    public static final String URL_REDDIT = "https://reddit.com/r/breadwallet/";
    public static final String URL_BLOG  = "http://t.me/elastoswalletelephant";


    public static final String DPOS_VOTE_ID = "552453550a0ad3ad6cedd21a7bd47d2a3049ebb17099e892d92b93f9bd72bdeed9a439806118358422f3cb957a1ddebdc768ff667c2bd7359063d79d1618bf9c";
    public static final String REA_PACKAGE_ID = "8FD01FF48C37DC11B53DF6E4BDB07924A3BF7034AEC2E0CC0CCDAC253F1AD006492F0E809E1274C1F20A819E438C8A4FDA99CD34A9E4B210337D6F26203A7B9D";
    public static final String ELA_NEWS_ID = "7277effb64cd41460829a4d1262884e1c4049dc2aaf2c091160385ccabbf6b31028988dcebd5995a34207964d859f4e5718111afcc1ab13c6e07f073931e0d93";
    public static final String DEVELOPER_WEBSITE = "c7457fabbb88243ce2d632a7fe31ff7f659320e835ddc4c2763c433c4d21d84e9f6d831eced288d687f684cf56d16cfd6e43e6fda61ee4912d52cce0d65fa892";
    public static final String DEVELOPER_WEBSITE_TEST = "99A808FDFE4BEC71D38F329E4800E8564161020253F74A1492718D43D5D369C05CED80F84FE4B9176A88CEE9B12508393F365C00C661D2D606239B79C6E1F182";
    public static final String EXCHANGE_ID = "3c9de10ab6bd6e86dd9891bd5b62c8a1c0cdd2e32172fc950a13a1e62c7e5fe0a162c971562be4a18ea3ccad722d2d8fd351aa05808f57028278bf6e49d0d210";
    public static final String HASH_ID = "14bd1d772afc8e633ce95b988646bd9ed5df65cb33ec98a43b41e6cb6de5276ca9cce91d069dc688619f1a250842b37d380d4fb984c9f912a2f86edcf5dddba9";


    public static final String ELAPHANT_APP_NAME = "elaphant";
    public static final String ELAPHANT_APP_ID = "8816ed501878a9f4404f5926c4fb2df56239424e41da9c449b4db35e9a8b99d5f976a0537858d709511b5b41cf11c0e88be8778008eb5f918a6aa712ac20c421";
    public static final String ELAPHANT_APP_PUBLICKEY = "03de176db829872772bf9e9d8bd79dd48cb6fd33d463f9d8620c9bad3fed790b59";
    public static final String ELAPHANT_APP_DID = "iocKHcPLRQKoyNrf8yMRtuWkeAgi3Ed9ns";

    public static final String ELA_ASSET_ID = "a3d0eaa466df74983b5d7c543de6904f4c9418ead5ffd6d25814234a96db37b0";
    public static final String IOEX_ASSET_ID = "61ccbfae9f8ce9660a71321041917139cb72cbb85bd105e92f0ed32cb1d1298f";

    private BRConstants() {
    }

}

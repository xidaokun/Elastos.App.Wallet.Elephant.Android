package com.breadwallet.wallet.wallets.ela.response.create;

import com.breadwallet.vote.PayLoadEntity;

import java.util.List;

public class Payload {
    public String type = "vote";
    public List<PayLoadEntity> candidatePublicKeys;
    public List<PayLoadEntity> candidateCrcs;

}

package com.breadwallet.vote;

import java.util.List;

public class CrcTxEntity {

    public CrcTxRes Result;


    public static class CrcTxRes {
        public String txid;
        public List<Vout> vout;
    }

    public static class Vout {
        public Payload payload;
    }

    public static class Payload {
        public List<Contents> contents;
    }

    public static class Contents {
        public int votetype;
        public List<Candidates> candidates;
    }

    public static class Candidates {
        public String candidate;
        public String votes;

        public Candidates(String candidate, String votes) {
            this.candidate = candidate;
            this.votes = votes;
        }
    }
}

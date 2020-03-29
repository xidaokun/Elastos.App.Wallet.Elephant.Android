package com.breadwallet.presenter.activities.crc;

import java.util.List;

public class CrcProducerResult {

    public List<CrcProducers> result;


    public static class CrcProducers {
        public List<CrcProducer> Producer;
        public String Txid;
    }

    public static class CrcProducer {
        public String Did;
        public int Location;
        public String State;

        public CrcProducer(String did, int location, String state){
            this.Did = did;
            this.Location = location;
            this.State = state;
        }
    }

}

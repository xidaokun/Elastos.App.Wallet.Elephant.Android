package com.breadwallet.vote;

public class CrcEntity {
    public String Did;
    public int Rank;
    public String Nickname;
    public int Location;
    public String AreaEn;
    public String AreaZh;
    public String Votes;
    public String Value;
    public String Vote;

    public CrcEntity() {
    }

    public CrcEntity(String did,
                     int rank,
                     String nickName,
                     int location,
                     String votes,
                     String value) {
        this.Did = did;
        this.Rank = rank;
        this.Nickname = nickName;
        this.Location = location;
        this.Votes = votes;
        this.Value = value;
    }
}

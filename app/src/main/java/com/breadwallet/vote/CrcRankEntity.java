package com.breadwallet.vote;

public class CrcRankEntity {
    public String Did;
    public int Rank;
    public String Nickname;
    public int Location;
    public String Area;
    public String Votes;
    public String Value;

    public CrcRankEntity() {
    }

    public CrcRankEntity(String did,
                         int rank,
                         String nickName,
                         int location,
                         String area,
                         String votes,
                         String value) {
        this.Did = did;
        this.Rank = rank;
        this.Nickname = nickName;
        this.Location = location;
        this.Area = area;
        this.Votes = votes;
        this.Value = value;
    }
}

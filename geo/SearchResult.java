/*
此类用来封装 response 拿到的结果 

*/

package cn.geo;

import java.io.Serializable;

public class SearchResult implements Serializable {

    private static final long serialVersionUID = -2531842843598730338L;

    private  int tooK ;

    private int searchTime;

    private int resultCount;

    private int threadSize;

    private long totalHits;


    public int getTooK() {
        return tooK;
    }

    public SearchResult setTooK(int tooK) {
        this.tooK = tooK;
        return this;
    }


   public int getSearchTime() {
        return searchTime;
    }

    public SearchResult setSearchTime(int searchTime) {
        this.searchTime = searchTime;
        return this;
    }

    public int getResultCount() {
        return resultCount;
    }

    public SearchResult setResultCount(int resultCount) {
        this.resultCount = resultCount;
        return this;
    }

    public int getThreadSize() {
        return threadSize;
    }

    public SearchResult setThreadSize(int threadSize) {
        this.threadSize = threadSize;
        return this;
    }

    public long getTotalHits() {
        return totalHits;
    }

    public SearchResult setTotalHits(long totalHits) {
        this.totalHits = totalHits;
        return this;
    }


    @Override
    public String toString() {
        return "SearchResult{" +
                "tooK=" + tooK +
                ", searchTime=" + searchTime +
                ", resultCount=" + resultCount +
                ", threadSize=" + threadSize +
                ", totalHits=" + totalHits +
                '}';
    }
}

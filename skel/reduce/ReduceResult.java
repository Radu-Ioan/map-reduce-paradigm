package reduce;

import java.util.List;


public class ReduceResult {
    private double rank;
    private String name;
    private int id;
    private long maxDim;
    private long maxDimApps;
    /**
     * Not written into the files
     */
    private List<String> longestWords;

    public ReduceResult(double rank, String name, int id, long maxDim,
                          long maxDimApps, List<String> longestWords) {
        this.rank = rank;
        this.name = name;
        this.id = id;
        this.maxDim = maxDim;
        this.maxDimApps = maxDimApps;
        this.longestWords = longestWords;
    }

    public double getRank() {
        return rank;
    }
    public void setRank(double rank) {
        this.rank = rank;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public long getMaxDim() {
        return maxDim;
    }
    public void setMaxDim(long maxDim) {
        this.maxDim = maxDim;
    }
    public long getMaxDimApps() {
        return maxDimApps;
    }
    public void setMaxDimApps(long maxDimApps) {
        this.maxDimApps = maxDimApps;
    }
    public List<String> getLongestWords() {
        return longestWords;
    }
    public void setLongestWords(List<String> longestWords) {
        this.longestWords = longestWords;
    }
}

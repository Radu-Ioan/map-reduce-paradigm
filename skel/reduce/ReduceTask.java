package reduce;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class ReduceTask {
    private String docName;
    /** [ {(length : apps)}, ] */
    private List<Map<Integer, Integer>> appsDictionaries;
    private List<List<String>> longestWordsLists;

    public ReduceTask(String docName) {
        this.docName = docName;
        appsDictionaries = new ArrayList<>();
        longestWordsLists = new ArrayList<>();
    }

    public String getDocName() {
        return docName;
    }

    public List<Map<Integer, Integer>> getAppsDictionaries() {
        return appsDictionaries;
    }

    public List<List<String>> getLongestWordsLists() {
        return longestWordsLists;
    }
}

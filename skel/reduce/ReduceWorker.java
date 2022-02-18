package reduce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


public class ReduceWorker extends Thread {
    private int id;
    private int p;
    /**
     * data shared by all reduce threads: documentsTasks are input, and
     * documentsResults must be filled by instances of this class
     */
    private ReduceTask[] documentsTasks;
    /**
     * [{length : apps}] -> represents the results after combine step,
     * but only for the documents designated to a certain instance
     */
    private List<Map<Integer, Integer>> docsDictionaries;
    /**
     * the lists with the longest words per document, for each document
     * inspected by one instance
     */
    private List<List<String>> longestWordsLists;
    /** the results after reduce step (each instance fills a subinterval) */
    private ReduceResult[] documentsResults;

    /** list with the sequence of Fibonacci */
    private List<Long> fibonacci;

    public ReduceWorker(int id, int p, ReduceTask[] documentsDictionaries,
                        ReduceResult[] documentsResults) {
        this.id = id;
        this.p = p;
        this.documentsTasks = documentsDictionaries;
        this.documentsResults = documentsResults;

        fibonacci = new ArrayList<>();
        fibonacci.add(0L);
        fibonacci.add(1L);
    }

    /**
     * Returns the idx'th termen of the fibonacci sequence
     * @param idx index in the sequence
     * @return fib[idx] value
     */
    private long getFib(int idx) {
        if ((long) idx >= fibonacci.size()) {
            for (int i = fibonacci.size(); i <= idx; i++) {
                var fib0 = fibonacci.get(i - 2);
                var fib1 = fibonacci.get(i - 1);
                fibonacci.add(fib0 + fib1);
            }
        }
        return fibonacci.get(idx);
    }

    @Override
    public void run() {
        combine();
        reduce();
    }

    private void combine() {
        int noDocs = documentsTasks.length;
        docsDictionaries = new ArrayList<>();
        longestWordsLists = new ArrayList<>();

        int len = (int) Math.ceil((double) noDocs / (double) p);
        int begin = id * len;
        int end = Math.min(begin + len - 1, noDocs - 1);

        for (int i = begin; i <= end; i++) {
            ReduceTask docData = documentsTasks[i];
            Map<Integer, Integer> combineObject = new HashMap<>();

            for (var dictionary : docData.getAppsDictionaries()) {
                // take each dictionary assigned to this doc
                for (var lenAppPair : dictionary.entrySet()) {
                    // take each pair from a certain dictionary
                    int certainLen = lenAppPair.getKey();
                    int certainNoApps = lenAppPair.getValue();

                    if (!combineObject.containsKey(certainLen)) {
                        combineObject.put(certainLen, certainNoApps);
                    } else {
                        int oldNoApps = combineObject.get(certainLen);
                        combineObject.replace(certainLen, oldNoApps,
                                              oldNoApps + certainNoApps);
                    }
                }
            }
            docsDictionaries.add(combineObject);

            int maxLength = 0;
            List<String> longestWords = new LinkedList<>();
            for (var stringList : docData.getLongestWordsLists()) {
                for (String s : stringList) {
                    int strLen = s.length();
                    if (strLen > maxLength) {
                        longestWords.clear();
                        longestWords.add(s);
                    } else if (strLen == maxLength) {
                        ((LinkedList<String>) longestWords).addFirst(s);
                    }
                }
            }
            longestWordsLists.add(longestWords);
        }
    }

    private void reduce() {
        int noDocs = documentsTasks.length;
        int len = (int) Math.ceil((double) noDocs / (double) p);
        int begin = id * len;
        int end = Math.min(begin + len - 1, noDocs - 1);

        for (int i = begin; i <= end; i++) {
            double docRank = 0.0;
            double totalWords = 0.0;
            int maxLength = 0;

            for (var entry : docsDictionaries.get(i - begin).entrySet()) {
                var wordLen = entry.getKey();
                var apps = entry.getValue();
                docRank += getFib(wordLen + 1) * apps;
                totalWords += apps;
                if (wordLen > maxLength)
                    maxLength = wordLen;
            }
            docRank /= totalWords;

            var result = new ReduceResult(docRank,
                                          documentsTasks[i].getDocName(),
                                          i, maxLength,
                                          docsDictionaries.get(i - begin)
                                         .get(maxLength),
                                          longestWordsLists.get(i - begin));
            documentsResults[i] = result;
        }
    }
}

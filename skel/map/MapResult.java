package map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MapResult {
    /** the document to which belongs the fragment result */
    private String docName;
    /** [{length : apps}] */
    private Map<Integer, Integer> dictionary;
    private List<String> longestWords;

    public MapResult(String docName) {
        this.docName = docName;
        dictionary = new HashMap<>();
        longestWords = new ArrayList<>();
    }

    public String getDocName() {
        return docName;
    }

    public Map<Integer, Integer> getDictionary() {
        return dictionary;
    }

    public List<String> getLongestWords() {
        return longestWords;
    }
}

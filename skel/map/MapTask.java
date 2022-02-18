package map;


public class MapTask {
    private final String docName;
    private final long startOffset;
    private final long dim;
    private final int docId;

    public MapTask(String docName, long startOffset, long dim, int docId) {
        this.docName = docName;
        this.startOffset = startOffset;
        this.dim = dim;
        this.docId = docId;
    }

    public String getDocName() {
        return docName;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public long getDim() {
        return dim;
    }

    public int getDocId() {
        return docId;
    }
}

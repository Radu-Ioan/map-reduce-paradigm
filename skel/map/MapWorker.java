package map;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;


public class MapWorker extends Thread {
    private int id;
    private int p;
    private List<MapTask> tasks;
    private boolean[] isSeparator;
    private long[] docDims;
    /** the dimension of a fragment from a document */
    private int D;
    /**
     * the buffer where the instances of this class will put the results;
     * each thread fills a subinterval from this array
     */
    private MapResult[] mapResults;

    public MapWorker(int id, int p, List<MapTask> tasks,
                     boolean[] separatorsMask, long[] docDims, int D,
                     MapResult[] mapResults) {
        this.id = id;
        this.p = p;
        this.tasks = tasks;
        this.isSeparator = separatorsMask;
        this.docDims = docDims;
        this.D = D;
        this.mapResults = mapResults;
    }

    @Override
    public void run() {
        int len = (int) Math.ceil((double) tasks.size() / (double) p);
        int begin = id * len;
        int end = Math.min(begin + len - 1, tasks.size() - 1);

        String currentDocName = null;
        RandomAccessFile fileAccess = null;
        byte[] fileDataBuffer = null;

        for (int i = begin; i <= end; i++) {
            // working with one by one task
            String taskDocName = tasks.get(i).getDocName();
            MapResult stat = new MapResult(taskDocName);

            if (!taskDocName.equals(currentDocName)) {
                try {
                    fileAccess = new RandomAccessFile(taskDocName, "r");
                    currentDocName = taskDocName;
                    if (fileDataBuffer == null) {
                        // We ensure memory also for the case when the last
                        // word in this fragment exceeds the reserved space;
                        // Also, we suppose that a word doesn't have a length
                        // greater than 2 * D
                        fileDataBuffer = new byte[D * 2];
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // used in case that left must be shifted to right
            long offset = tasks.get(i).getStartOffset();

            long left = findLeftStart(offset, fileAccess, fileDataBuffer);
            long right = findRightStop(offset, fileAccess, fileDataBuffer,
                                       docDims[tasks.get(i).getDocId()]);

            if (left >= D || right >= 2 * D) {
                mapResults[i] = stat;
                continue;
            }

            int wordLen = 0;
            int maxLen = 0;
            StringBuilder readWord = new StringBuilder();
            int j = (int) left;

            while (j <= right) {
                while (j <= right && isSeparator[fileDataBuffer[j]]) {
                    j++;
                }

                while (j <= right && !isSeparator[fileDataBuffer[j]]) {
                    readWord.append((char) fileDataBuffer[j]);
                    wordLen++;
                    j++;
                }

                if (wordLen > maxLen) {
                    maxLen = wordLen;
                    stat.getLongestWords().clear();
                    stat.getLongestWords().add(readWord.toString());
                } else if (wordLen == maxLen && wordLen != 0) {
                    stat.getLongestWords().add(readWord.toString());
                }

                if (stat.getDictionary().containsKey(wordLen)) {
                    var old = stat.getDictionary().get(wordLen);
                    stat.getDictionary().replace(wordLen, old, old + 1);
                } else if (wordLen != 0) {
                    stat.getDictionary().put(wordLen, 1);
                }

                wordLen = 0;
                readWord.delete(0, readWord.length());
            }
            Arrays.fill(fileDataBuffer, (byte) 0);
            mapResults[i] = stat;
        }
    }

    /**
     * Calculates the index in the data buffer where the search can start for
     * a certain fragment based on the following parameters
     * @param offset the offset in the file
     * @param fileAccess the RandomAccessFile object for the specific document
     * @param fileDataBuffer the byte array for taking the data from the file
     * @return the index for searching start
     */
    private int findLeftStart(long offset, RandomAccessFile fileAccess,
                              byte[] fileDataBuffer) {
        // for checking the last char from the previous fragment
        byte[] charBuffer = new byte[1];
        // return variable
        int left = 0;

        // read the required fragment
        try {
            fileAccess.seek(offset);
            fileAccess.read(fileDataBuffer, 0, D);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (offset > 0 && !isSeparator[fileDataBuffer[0]]) {
            // check whether the fragment start continues the previous last
            // word
            try {
                fileAccess.seek(offset - 1);
                // read the last char from the previous fragment
                fileAccess.read(charBuffer, 0, 1);
                if (!isSeparator[charBuffer[0]]) {
                    left = 0;
                    // iterate until the next separator
                    // (because there you can start processing this fragment
                    // without overlapping with the previous)
                    while (left < D && !isSeparator[fileDataBuffer[left]])
                        left++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return left;
    }

    /**
     * Calculates the index in the data buffer where the search must stop for
     * a certain fragment based on the following parameters (it is assumed that
     * a word doesn't exceeds a 2 * D length)
     * @param offset the offset in the file
     * @param fileAccess the RandomAccessFile object for the specific document
     * @param fileDataBuffer the byte array for taking the data from the file
     * @return the index for searching stop
     */
    private int findRightStop(long offset, RandomAccessFile fileAccess,
                              byte[] fileDataBuffer, long docLength) {
        // for checking the first char from the next fragment
        byte[] charBuffer = new byte[1];
        // return variable
        int right = D - 1;

        try {
            fileAccess.seek(offset + D - 1);
            fileAccess.read(charBuffer, 0, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // verify to don't be the last fragment from the doc
        if (offset < docLength - D && !isSeparator[charBuffer[0]]) {
            // check whether the fragment end continues on the next
            try {
                // read the next fragment and in the same time fill the
                // second part of the buffer
                fileAccess.seek(offset + D);
                fileAccess.read(fileDataBuffer, D, D);
                if (!isSeparator[fileDataBuffer[D]]) {
                    right = D;
                    // iterate until the next separator
                    // (because there you can stop processing the fragment
                    // without overlapping with the next)
                    while (!isSeparator[fileDataBuffer[right]])
                        right++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return right;
    }
}

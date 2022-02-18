import java.io.RandomAccessFile;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import map.MapResult;
import map.MapTask;
import map.MapWorker;
import reduce.ReduceResult;
import reduce.ReduceTask;
import reduce.ReduceWorker;


class Solver {
    /** number of workers */
    private final int p;
    /** other constants */
    private final String inputFileName, outputFileName;
    private final String DOCUMENTS_FOLDER = "tests/files/";
    private final char COMMA = ',';
    private final String separators
        = ";:/?~\\.,><`[]{}()!@#$%^&-_+'=*\"| \t\r\n";
    // if c is included in separators, then separatorMask[c] = true
    // this variable is used by map threads as a hashtable
    private boolean[] separatorsMask;

    /** the dimension of a fragment from a document */
    private int D;
    private int noDocs;
    private String[] docNames;
    private long[] docDims;

    private List<MapTask> mapTasks;
    /** here the map workers will put the data calculated for each task */
    private MapResult[] mapResults;

    private ReduceTask[] reduceTasks;
    /** the desired result for the homework */
    private ReduceResult[] reduceResults;

    public Solver(int p, String inputFileName, String outputFileName) {
        this.p = p;
        this.inputFileName = inputFileName;
        this.outputFileName = outputFileName;
        // the separators fit into one byte space for encoding since all the
        // characters are ASCII, otherwise I would have used a hash object
        separatorsMask = new boolean[256];
        for (char c : separators.toCharArray()) {
            separatorsMask[c] = true;
        }
        separatorsMask[0] = true;
    }

    public void solve() {
        readInput();
        createMapTasks();
        // the map workers will put the results into mapResults
        solveMapTasks();
        // this instance will create the reduce tasks based on the map results
        createReduceTasks();
        solveReduceTasks();
        sortAndWriteOutput();
    }

    private void readInput() {
        try (BufferedReader objReader
                     = new BufferedReader(new FileReader(inputFileName))) {
            D = Integer.parseInt(objReader.readLine());
            noDocs = Integer.parseInt(objReader.readLine());
            docNames = new String[noDocs];

            for (int i = 0; i < noDocs; i++)
                docNames[i] = objReader.readLine();

        } catch (NumberFormatException e) {
            System.out.println("Not a suitable string for converting into"
                + " integer");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createMapTasks() {
        mapTasks = new ArrayList<>();
        docDims = new long[noDocs];

        for (int i = 0; i < noDocs; i++) {
            try (RandomAccessFile raf
                         = new RandomAccessFile(docNames[i], "r")) {
                docDims[i] = raf.length();

                for (long j = 0; j < docDims[i]; j += D) {
                    var delta = docDims[i] - j;
                    mapTasks.add(new MapTask(docNames[i], j,
                        (delta >= D) ? D : delta, i));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void solveMapTasks() {
        mapResults = new MapResult[mapTasks.size()];
        Thread[] mapWorkers = new MapWorker[p];

        for (int i = 0; i < p; i++) {
            mapWorkers[i] = new MapWorker(i, p, mapTasks, separatorsMask,
                                          docDims, D, mapResults);
        }

        for (int i = 0; i < p; i++) {
            mapWorkers[i].start();
        }

        for (int i = 0; i < p; i++) {
            try {
                mapWorkers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void createReduceTasks() {
        reduceTasks = new ReduceTask[noDocs];

        // the document whose task is being building
        int docId = 0;
        reduceTasks[docId] = new ReduceTask(docNames[docId]);

        for (int i = 0; i < mapResults.length; i++) {
            var result = mapResults[i];
            if (!result.getDocName().equals(docNames[docId])) {
                // pass to the next document read, the previous was finished
                // i.e. there are no more fragments from it
                docId++;
                reduceTasks[docId] = new ReduceTask(docNames[docId]);
            }
            reduceTasks[docId].getAppsDictionaries()
                              .add(result.getDictionary());
            reduceTasks[docId].getLongestWordsLists()
                              .add(result.getLongestWords());
        }
    }

    private void solveReduceTasks() {
        Thread[] reduceWorkers = new ReduceWorker[p];
        reduceResults = new ReduceResult[noDocs];

        for (int i = 0; i < p; i++) {
            reduceWorkers[i] = new ReduceWorker(i, p, reduceTasks,
                                                reduceResults);
        }

        for (int i = 0; i < p; i++) {
            reduceWorkers[i].start();
        }

        for (int i = 0; i < p; i++) {
            try {
                reduceWorkers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void sortAndWriteOutput() {
        Arrays.sort(reduceResults, (d1, d2) -> {
            Double r1 = d1.getRank();
            Double r2 = d2.getRank();
            if (r1.equals(r2)) {
                return d1.getId() - d2.getId();
            }
            return r2.compareTo(r1);
        });

        int docNameOffset = DOCUMENTS_FOLDER.length();

        try (FileWriter fileWriter = new FileWriter(outputFileName)) {
            BufferedWriter writer = new BufferedWriter(fileWriter);
            for (int i = 0; i < noDocs; i++) {
                String name = reduceResults[i].getName();
                Double rank = reduceResults[i].getRank();
                Long maxWordLen = reduceResults[i].getMaxDim();
                Long maxWordsApps = reduceResults[i].getMaxDimApps();

                writer.write(name.substring(docNameOffset));
                writer.write(COMMA);
                writer.write(String.format("%.2f", rank));
                writer.write(COMMA);
                writer.write(maxWordLen.toString());
                writer.write(COMMA);
                writer.write(maxWordsApps.toString());
                writer.newLine();
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class Tema2 {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: Tema2 <workers> <in_file> <out_file>");
            return;
        }
        int p = Integer.parseInt(args[0]);
        String inputFileName = args[1];
        String outputFileName = args[2];
        var student = new Solver(p, inputFileName, outputFileName);
        student.solve();
    }
}

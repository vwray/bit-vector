package sparsearray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.profiler.Profiler;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;

import rank.RankSupport;
import select.SelectSupport;

/**
 * Test class for {@link SparseArray}.
 * 
 * @author Valerie Wray
 *
 */
class SparseArrayTest {

    @Test
    void testSparseArray() {
        SparseArray sparseArray = new SparseArray(100);
        sparseArray.append("foo", 1);
        sparseArray.append("bar", 5);
        sparseArray.append("baz", 9);
        sparseArray.finalize();

        StringBuilder stringAtRank1 = new StringBuilder();
        assertTrue(sparseArray.getAtRank(1, stringAtRank1));
        assertEquals("bar", stringAtRank1.toString());

        StringBuilder stringAtIndex3 = new StringBuilder();
        assertFalse(sparseArray.getAtIndex(3, stringAtIndex3));

        StringBuilder stringAtIndex5 = new StringBuilder();
        assertTrue(sparseArray.getAtIndex(5, stringAtIndex5));
        assertEquals("bar", stringAtIndex5.toString());

        assertEquals(1, sparseArray.getIndexOf(1));
        assertEquals(5, sparseArray.getIndexOf(2));
        assertEquals(9, sparseArray.getIndexOf(3));
        assertEquals(-1, sparseArray.getIndexOf(4));

        assertEquals(0, sparseArray.numberOfElementsAt(0));
        assertEquals(2, sparseArray.numberOfElementsAt(5));
        assertEquals(2, sparseArray.numberOfElementsAt(6));
        assertEquals(3, sparseArray.numberOfElementsAt(9));
    }

    @Test
    void testLoadAndSaveSparseArray() throws IOException, ClassNotFoundException {
        SparseArray sparseArray = new SparseArray(100);
        sparseArray.append("foo", 1);
        sparseArray.append("bar", 5);
        sparseArray.append("baz", 9);
        sparseArray.finalize();
        sparseArray.save("src/test/resources/sparseArray.bin");
        SparseArray sparseArray2 = SparseArray.load("src/test/resources/sparseArray.bin");
        SelectSupport selectSupport = sparseArray.getSelectSupport();
        SelectSupport selectSupport2 = sparseArray2.getSelectSupport();
        assertEquals(selectSupport.getBitVector(), selectSupport2.getBitVector());
        assertEquals(selectSupport.getBitVectorLength(), selectSupport2.getBitVectorLength());
        assertEquals(selectSupport.getChunkSize(), selectSupport2.getChunkSize());
        assertEquals(selectSupport.getSubchunkSize(), selectSupport2.getSubchunkSize());
        assertEquals(selectSupport.getCumulativeRanks().get(0), selectSupport2.getCumulativeRanks().get(0));
        assertEquals(selectSupport.getSubchunkCumulativeRanks().get(1),
                selectSupport2.getSubchunkCumulativeRanks().get(1));
        assertEquals(selectSupport.getNumberOfChunks(), selectSupport2.getNumberOfChunks());
        assertEquals(selectSupport.getNumberOfSubchunks(), selectSupport2.getNumberOfSubchunks());
    }

    /**
     * Tests runtimes of multiple operations performed in sequence and plots the
     * results.
     */
    // @Test
    void testTimesPlotData() throws IOException, PythonExecutionException {

        List<Double> runtimes1PercentCapacity = new ArrayList<>();
        List<Double> runtimes5PercentCapacity = new ArrayList<>();
        List<Double> runtimes10PercentCapacity = new ArrayList<>();
        List<Double> runtimes30PercentCapacity = new ArrayList<>();

        // throw these away; first few take longer
        for (int N : Arrays.asList(1000, 10000, 50000, 100000, 250000, 500000)) {
            executeOperations(.01, N);

        }

        List<Integer> valuesOfN = new ArrayList<>();
        for (int N = 100; N < 1001000; N += 1000) {
            valuesOfN.add(N);
        }
        for (int N : valuesOfN) {
            runtimes1PercentCapacity.add(executeOperations(.01, N));
            runtimes5PercentCapacity.add(executeOperations(.05, N));
            runtimes10PercentCapacity.add(executeOperations(.1, N));
            runtimes30PercentCapacity.add(executeOperations(.3, N));

        }
        Plot plt = Plot.create();
        plt.plot().add(valuesOfN, runtimes1PercentCapacity).label("1% capacity"); // , "o"
        plt.plot().add(valuesOfN, runtimes5PercentCapacity).label("5% capacity");
        plt.plot().add(valuesOfN, runtimes10PercentCapacity).label("10% capacity");
        plt.plot().add(valuesOfN, runtimes30PercentCapacity).label("30% capacity");
        plt.title("Runtime of SparseArray Operations");
        plt.xlabel("Size of Bit Vector");
        plt.ylabel("Runtime in Nanoseconds");
        plt.legend();
        plt.show();
    }

    // @Test
    void testGetAtRankPlotData() throws IOException, PythonExecutionException {

        List<Double> runtimes1PercentCapacity = new ArrayList<>();
        List<Double> runtimes5PercentCapacity = new ArrayList<>();
        List<Double> runtimes10PercentCapacity = new ArrayList<>();
        List<Double> runtimes30PercentCapacity = new ArrayList<>();

        // throw these away; first few take longer
        for (int N : Arrays.asList(1000, 10000, 50000, 100000, 250000, 500000)) {
            executeGetAtRank(.01, N);

        }

        List<Integer> valuesOfN = Arrays.asList(1000, 10000, 50000, 100000, 250000, 500000, 750000, 1000000, 1500000,
                2000000, 5000000);
        // for each value of N, do multiple runs and average them
        for (int N : valuesOfN) {
            runtimes1PercentCapacity.add(executeGetAtRank(.01, N));
            runtimes5PercentCapacity.add(executeGetAtRank(.05, N));
            runtimes10PercentCapacity.add(executeGetAtRank(.1, N));
            runtimes30PercentCapacity.add(executeGetAtRank(.3, N));
        }

        Plot plt = Plot.create();
        plt.plot().add(valuesOfN, runtimes1PercentCapacity).label("1% capacity"); // , "o"
        plt.plot().add(valuesOfN, runtimes5PercentCapacity).label("5% capacity");
        plt.plot().add(valuesOfN, runtimes10PercentCapacity).label("10% capacity");
        plt.plot().add(valuesOfN, runtimes30PercentCapacity).label("30% capacity");
        plt.title("Runtime of SparseArray getAtRank");
        plt.xlabel("Size of Bit Vector");
        plt.ylabel("Runtime in Nanoseconds");
        plt.legend();
        plt.show();
    }

    // @Test
    void testGetAtIndexPlotData() throws IOException, PythonExecutionException {

        List<Double> runtimes1PercentCapacity = new ArrayList<>();
        List<Double> runtimes5PercentCapacity = new ArrayList<>();
        List<Double> runtimes10PercentCapacity = new ArrayList<>();
        List<Double> runtimes30PercentCapacity = new ArrayList<>();

        // throw these away; first few take longer
        for (int N : Arrays.asList(1000, 10000, 50000, 100000, 250000, 500000)) {
            executeGetAtIndex(.01, N);

        }

        List<Integer> valuesOfN = Arrays.asList(1000, 10000, 50000, 100000, 250000, 500000, 750000, 1000000, 1500000,
                2000000, 5000000);

        for (int N : valuesOfN) {
            runtimes1PercentCapacity.add(executeGetAtIndex(.01, N));
            runtimes5PercentCapacity.add(executeGetAtIndex(.05, N));
            runtimes10PercentCapacity.add(executeGetAtIndex(.1, N));
            runtimes30PercentCapacity.add(executeGetAtIndex(.3, N));

        }
        Plot plt = Plot.create();
        plt.plot().add(valuesOfN, runtimes1PercentCapacity).label("1% capacity"); // , "o"
        plt.plot().add(valuesOfN, runtimes5PercentCapacity).label("5% capacity");
        plt.plot().add(valuesOfN, runtimes10PercentCapacity).label("10% capacity");
        plt.plot().add(valuesOfN, runtimes30PercentCapacity).label("30% capacity");
        plt.title("Runtime of SparseArray getAtIndex");
        plt.xlabel("Size of Bit Vector");
        plt.ylabel("Runtime in Nanoseconds");
        plt.legend();
        plt.show();
    }

    // @Test
    void testGetIndexOfPlotData() throws IOException, PythonExecutionException {

        List<Double> runtimes1PercentCapacity = new ArrayList<>();
        List<Double> runtimes5PercentCapacity = new ArrayList<>();
        List<Double> runtimes10PercentCapacity = new ArrayList<>();
        List<Double> runtimes30PercentCapacity = new ArrayList<>();

        // throw these away; first few take longer
        for (int N : Arrays.asList(1000, 10000, 50000, 100000, 250000, 500000)) {
            executeGetIndexOf(.01, N);

        }

        List<Integer> valuesOfN = Arrays.asList(1000, 10000, 50000, 100000, 250000, 500000, 750000, 1000000, 1500000,
                2000000, 5000000);
        for (int N : valuesOfN) {
            runtimes1PercentCapacity.add(executeGetIndexOf(.01, N));
            runtimes5PercentCapacity.add(executeGetIndexOf(.05, N));
            runtimes10PercentCapacity.add(executeGetIndexOf(.1, N));
            runtimes30PercentCapacity.add(executeGetIndexOf(.3, N));

        }
        Plot plt = Plot.create();
        plt.plot().add(valuesOfN, runtimes1PercentCapacity).label("1% capacity"); // , "o"
        plt.plot().add(valuesOfN, runtimes5PercentCapacity).label("5% capacity");
        plt.plot().add(valuesOfN, runtimes10PercentCapacity).label("10% capacity");
        plt.plot().add(valuesOfN, runtimes30PercentCapacity).label("30% capacity");
        plt.title("Runtime of SparseArray getIndexOf");
        plt.xlabel("Size of Bit Vector");
        plt.ylabel("Runtime in Nanoseconds");
        plt.legend();
        plt.show();
    }

    // @Test
    void testNumberOfElementsAtPlotData() throws IOException, PythonExecutionException {

        List<Double> runtimes1PercentCapacity = new ArrayList<>();
        List<Double> runtimes5PercentCapacity = new ArrayList<>();
        List<Double> runtimes10PercentCapacity = new ArrayList<>();
        List<Double> runtimes30PercentCapacity = new ArrayList<>();

        // throw these away; first few take longer
        for (int N : Arrays.asList(1000, 10000, 50000, 100000, 250000, 500000)) {
            executeGetNumberOfElementsAt(.01, N);

        }

        List<Integer> valuesOfN = Arrays.asList(1000, 10000, 50000, 100000, 250000, 500000, 750000, 1000000, 1500000,
                2000000, 5000000);
        for (int N : valuesOfN) {
            runtimes1PercentCapacity.add(executeGetNumberOfElementsAt(.01, N));
            runtimes5PercentCapacity.add(executeGetNumberOfElementsAt(.05, N));
            runtimes10PercentCapacity.add(executeGetNumberOfElementsAt(.1, N));
            runtimes30PercentCapacity.add(executeGetNumberOfElementsAt(.3, N));

        }
        Plot plt = Plot.create();
        plt.plot().add(valuesOfN, runtimes1PercentCapacity).label("1% capacity"); // , "o"
        plt.plot().add(valuesOfN, runtimes5PercentCapacity).label("5% capacity");
        plt.plot().add(valuesOfN, runtimes10PercentCapacity).label("10% capacity");
        plt.plot().add(valuesOfN, runtimes30PercentCapacity).label("30% capacity");
        plt.title("Runtime of SparseArray numberOfElementsAt");
        plt.xlabel("Size of Bit Vector");
        plt.ylabel("Runtime in Nanoseconds");
        plt.legend();
        plt.show();
    }

    private double executeOperations(double capacity, int N) {
        Random random = new Random();
        SparseArray sparseArray = new SparseArray(N);
        int section = (int) (N * capacity);
        System.out.println("section: " + section);
        for (int i = 1; i <= section; i++) {

            int lowerBound = (N / section) * (i - 1);
            int upperBound = (N / section) * i;
            int randomPosition = random.nextInt(lowerBound, upperBound);

            sparseArray.append("element" + i, randomPosition);
        }

        sparseArray.finalize();

        // average 200 runs
        long runtimeRunningTotal = 0;
        for (int j = 0; j < 200; j++) {
            runtimeRunningTotal += timeOperations(sparseArray);
        }
        runtimeRunningTotal = runtimeRunningTotal / (long) 200;
        System.out.println("Runtime average for " + capacity + " capacity, " + N + " size array: " + runtimeRunningTotal
                + " microseconds");
        return runtimeRunningTotal;
    }

    private double executeGetAtRank(double capacity, int N) {
        Random random = new Random();
        SparseArray sparseArray = new SparseArray(N);
        int section = (int) (N * capacity);
        System.out.println("section: " + section);
        for (int i = 1; i <= section; i++) {

            int lowerBound = (N / section) * (i - 1);
            int upperBound = (N / section) * i;
            int randomPosition = random.nextInt(lowerBound, upperBound);

            sparseArray.append("element" + i, randomPosition);
        }

        sparseArray.finalize();

        // average 200 runs
        long runtimeRunningTotal = 0;
        for (int j = 0; j < 200; j++) {
            runtimeRunningTotal += timeGetAtRank(sparseArray, random.nextInt(0, sparseArray.numberOfElements()),
                    new StringBuilder());
        }
        runtimeRunningTotal = runtimeRunningTotal / (long) 200;
        System.out.println("Runtime average for " + capacity + " capacity, " + N + " size array: " + runtimeRunningTotal
                + " microseconds");
        return runtimeRunningTotal;
    }

    private double executeGetAtIndex(double capacity, int N) {
        Random random = new Random();
        SparseArray sparseArray = new SparseArray(N);
        int section = (int) (N * capacity);
        System.out.println("section: " + section);
        for (int i = 1; i <= section; i++) {

            int lowerBound = (N / section) * (i - 1);
            int upperBound = (N / section) * i;
            int randomPosition = random.nextInt(lowerBound, upperBound);

            sparseArray.append("element" + i, randomPosition);
        }

        sparseArray.finalize();

        // average 200 runs
        long runtimeRunningTotal = 0;
        for (int j = 0; j < 200; j++) {
            runtimeRunningTotal += timeGetAtIndex(sparseArray, random.nextInt(0, sparseArray.size()),
                    new StringBuilder());
        }
        runtimeRunningTotal = runtimeRunningTotal / (long) 200;
        System.out.println("Runtime average for " + capacity + " capacity, " + N + " size array: " + runtimeRunningTotal
                + " microseconds");
        return runtimeRunningTotal;
    }

    private double executeGetIndexOf(double capacity, int N) {
        Random random = new Random();
        SparseArray sparseArray = new SparseArray(N);
        int section = (int) (N * capacity);
        System.out.println("section: " + section);
        for (int i = 1; i <= section; i++) {

            int lowerBound = (N / section) * (i - 1);
            int upperBound = (N / section) * i;
            int randomPosition = random.nextInt(lowerBound, upperBound);

            sparseArray.append("element" + i, randomPosition);
        }

        sparseArray.finalize();

        // average 200 runs
        long runtimeRunningTotal = 0;
        for (int j = 0; j < 200; j++) {
            runtimeRunningTotal += timeGetIndexOf(sparseArray, random.nextInt(0, sparseArray.numberOfElements()));
        }
        runtimeRunningTotal = runtimeRunningTotal / (long) 200;
        System.out.println("Runtime average for " + capacity + " capacity, " + N + " size array: " + runtimeRunningTotal
                + " microseconds");
        return runtimeRunningTotal;
    }

    private double executeGetNumberOfElementsAt(double capacity, int N) {
        Random random = new Random();
        SparseArray sparseArray = new SparseArray(N);
        int section = (int) (N * capacity);
        System.out.println("section: " + section);
        for (int i = 1; i <= section; i++) {

            int lowerBound = (N / section) * (i - 1);
            int upperBound = (N / section) * i;
            int randomPosition = random.nextInt(lowerBound, upperBound);

            sparseArray.append("element" + i, randomPosition);
        }

        sparseArray.finalize();

        // average 200 runs
        long runtimeRunningTotal = 0;
        for (int j = 0; j < 200; j++) {
            runtimeRunningTotal += timeNumberOfElementsAt(sparseArray, random.nextInt(0, sparseArray.size()));
        }
        runtimeRunningTotal = runtimeRunningTotal / (long) 200;
        System.out.println("Runtime average for " + capacity + " capacity, " + N + " size array: " + runtimeRunningTotal
                + " microseconds");
        return runtimeRunningTotal;
    }

    private long timeGetAtRank(SparseArray sparseArray, int r, StringBuilder element) {
        Profiler myProfiler = new Profiler("SparseArrayProfiler");
        myProfiler.start("Timing getAtRank " + r);

        sparseArray.getAtRank(r, element);

        myProfiler.stop();
        return myProfiler.elapsedTime();

    }

    private long timeGetAtIndex(SparseArray sparseArray, int r, StringBuilder element) {
        Profiler myProfiler = new Profiler("SparseArrayProfiler");
        myProfiler.start("Timing operations");

        sparseArray.getAtIndex(r, element);

        myProfiler.stop();
        return myProfiler.elapsedTime();
    }

    private long timeGetIndexOf(SparseArray sparseArray, int r) {
        Profiler myProfiler = new Profiler("SparseArrayProfiler");
        myProfiler.start("Timing operations");

        sparseArray.getIndexOf(r);

        myProfiler.stop();
        return myProfiler.elapsedTime();
    }

    private long timeNumberOfElementsAt(SparseArray sparseArray, int r) {
        Profiler myProfiler = new Profiler("SparseArrayProfiler");
        myProfiler.start("Timing operations");

        sparseArray.numberOfElementsAt(r);

        myProfiler.stop();
        return myProfiler.elapsedTime();
    }

    private long timeOperations(SparseArray sparseArray) {
        Profiler myProfiler = new Profiler("SparseArrayProfiler");
        myProfiler.start("Timing operations");

        int numberOfElements = sparseArray.numberOfElements();
        int size = sparseArray.size();
        sparseArray.getAtRank(numberOfElements / 2, new StringBuilder());
        sparseArray.getAtIndex(size / 2, new StringBuilder());
        sparseArray.getIndexOf(numberOfElements);
        sparseArray.numberOfElementsAt(size - 1);

        myProfiler.stop();
        return myProfiler.elapsedTime();
    }

    @Test
    void testStringMemoryUsage() {
        String string1 = "string1";
        System.out.println(GraphLayout.parseInstance(string1).toPrintable());
        System.out.println("total count: " + GraphLayout.parseInstance(string1).totalSize());

        String string2 = "";
        System.out.println(GraphLayout.parseInstance(string2).toPrintable());
        System.out.println("total count: " + GraphLayout.parseInstance(string2).totalSize());
    }

    // @Test
    void testSparseArrayOverheadSavings() throws IOException, PythonExecutionException {
        List<Double> overheads1PercentCapacity = new ArrayList<>();
        List<Double> overheads5PercentCapacity = new ArrayList<>();
        List<Double> overheads10PercentCapacity = new ArrayList<>();
        List<Double> overheads30PercentCapacity = new ArrayList<>();

        List<Integer> valuesOfN = Arrays.asList(1000, 10000, 50000, 100000, 250000, 500000, 750000, 1000000, 1500000,
                2000000);
        for (int N : valuesOfN) {
            double a = getRankSupportOverheadForCapacity(.01, N);
            double b = (double) getExplicitArrayOverheadForCapacity(.01, N);
            overheads1PercentCapacity.add(100 * (b - a) / b);

            a = getRankSupportOverheadForCapacity(.05, N);
            b = (double) getExplicitArrayOverheadForCapacity(.05, N);
            overheads5PercentCapacity.add(100 * (b - a) / b);

            a = getRankSupportOverheadForCapacity(.1, N);
            b = (double) getExplicitArrayOverheadForCapacity(.1, N);
            overheads10PercentCapacity.add(100 * (b - a) / b);

            a = getRankSupportOverheadForCapacity(.3, N);
            b = (double) getExplicitArrayOverheadForCapacity(.3, N);
            overheads30PercentCapacity.add(100 * (b - a) / b);

        }

        Plot plt = Plot.create();
        plt.plot().add(valuesOfN, overheads1PercentCapacity).label("1% capacity"); // , "o"
        plt.plot().add(valuesOfN, overheads5PercentCapacity).label("5% capacity");
        plt.plot().add(valuesOfN, overheads10PercentCapacity).label("10% capacity");
        plt.plot().add(valuesOfN, overheads30PercentCapacity).label("30% capacity");
        plt.title("Sparse Array Overhead Savings");
        plt.xlabel("Size of Sparse Array");
        plt.ylabel("Percentage of Overhead Savings");
        plt.legend();
        plt.show();
    }

    private long getRankSupportOverheadForCapacity(double capacity, int N) {
        BitSet bitVector = new BitSet(N);
        setRandomCapacityBits(bitVector, capacity, N);
        RankSupport rankSupport = new RankSupport(bitVector, N);
        System.out.println("Overhead of Array length " + N + " with sparsity " + capacity);
        System.out.println("RankSupport: " + rankSupport.overhead());

        return N + rankSupport.overhead();
    }

    private long getExplicitArrayOverheadForCapacity(double capacity, int N) {
        System.out.println("Explicit array: " + Math.ceil(40 * (1.0 - capacity) * N * 8));
        return (long) Math.ceil(40 * (1.0 - capacity) * N * 8);
    }

    private void setRandomCapacityBits(BitSet bitVector, double capacity, int N) {
        int section = (int) (N * capacity);
        Random random = new Random();
        for (int i = 1; i <= section; i++) {

            int lowerBound = (N / section) * (i - 1);
            int upperBound = (N / section) * i;
            int randomPosition = random.nextInt(lowerBound, upperBound);

            bitVector.set(randomPosition);
        }
    }
}

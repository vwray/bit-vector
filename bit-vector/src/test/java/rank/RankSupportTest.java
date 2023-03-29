package rank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.Vector;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.ClassLayout;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.github.sh0nk.matplotlib4j.builder.ScaleBuilder.Scale;

/**
 * Test class for {@link RankSupport}.
 * 
 * @author Valerie Wray
 *
 */
class RankSupportTest {

    @Test
    void testOverhead() {
        BitSet bitVector = new BitSet(100);
        bitVector.set(0, 50);
        RankSupport rankSupport = new RankSupport(bitVector, 100);
        long value = rankSupport.overhead();
        assertEquals(rankSupport.overheadVia3rdParty(), rankSupport.overhead());
    }

    @Test
    void testOverhead3rdParty() {
        BitSet bitVector = new BitSet(10000);
        bitVector.set(0, 50);
        RankSupport rankSupport = new RankSupport(bitVector, 10000);
        System.out.println(ClassLayout.parseInstance(rankSupport.getCumulativeRanks()).toPrintable());
        assertEquals(rankSupport.overhead(), rankSupport.overheadVia3rdPartyGraphLayout());

        assertEquals(rankSupport.getNumberOfChunks() * 32 + (8 + 4 + 4 + 4) * 8,
                ClassLayout.parseInstance(rankSupport.getCumulativeRanks()).instanceSize() * 8);
        assertEquals(rankSupport.getNumberOfChunks() * rankSupport.getNumberOfSubchunks() * 32 + (8 + 4 + 4 + 4) * 8,
                ClassLayout.parseInstance(rankSupport.getSubchunkCumulativeRanks()).instanceSize() * 8);
    }

    @Test
    void testPrecomputeCumulativeRanks16() {
        BitSet bitVector = new BitSet(16);
        bitVector.set(7, 16);
        RankSupport rankSupport = new RankSupport(bitVector, 16);
        rankSupport.precomputeCumulativeRanks();
        int[] cumulativeRanks = rankSupport.getCumulativeRanks();
        assertEquals(1, cumulativeRanks.length);
        assertEquals(0, cumulativeRanks[0]);
    }

    /**
     * log_2 (128) = 7. 7^2 = 49. Should have 3 chunks, size 49. All bits set in
     * bitvector.
     */
    @Test
    void testPrecomputeCumulativeRanks128() {
        BitSet bitVector = new BitSet(128);
        bitVector.set(0, 128);
        RankSupport rankSupport = new RankSupport(bitVector, 128);
        rankSupport.precomputeCumulativeRanks();
        int[] cumulativeRanks = rankSupport.getCumulativeRanks();
        assertEquals(3, cumulativeRanks.length);
        assertEquals(0, cumulativeRanks[0]);
        assertEquals(49, cumulativeRanks[1]);
        assertEquals(98, cumulativeRanks[2]);
    }

    /**
     * log_2 (128) = 7. 7^2 = 49. Should have 3 chunks, size 49. All bits set in
     * bitvector. 7/2 = 3.5 subchunk size of 3. 49/3 = 17 total subchunks in each
     * chunk
     */
    @Test
    void testPrecomputeSubchunkCumulativeRanks128() {
        BitSet bitVector = new BitSet(128);
        bitVector.set(0, 128);
        RankSupport rankSupport = new RankSupport(bitVector, 128);
        rankSupport.precomputeSubchunkCumulativeRanks();
        int[][] subchunkCumulativeRanks = rankSupport.getSubchunkCumulativeRanks();

        assertEquals(3, subchunkCumulativeRanks.length);
        assertEquals(17, subchunkCumulativeRanks[0].length);
        assertEquals(0, subchunkCumulativeRanks[0][0]);
        assertEquals(3, subchunkCumulativeRanks[0][1]);
        assertEquals(6, subchunkCumulativeRanks[0][2]);
        assertEquals(9, subchunkCumulativeRanks[0][3]);
        assertEquals(0, subchunkCumulativeRanks[1][0]);
    }

    @Test
    void testRank1_allSet() {
        BitSet bitVector = new BitSet(128);
        bitVector.set(0, 128);
        RankSupport rankSupport = new RankSupport(bitVector, 128);
        assertEquals(8, rankSupport.rank1(8));
        assertEquals(10, rankSupport.rank1(10));
        assertEquals(18, rankSupport.rank1(18));
        assertEquals(127, rankSupport.rank1(127));
    }

    @Test
    void testRank1_largeArrayAllSet() {
        BitSet bitVector = new BitSet(1000000);
        bitVector.set(0, 1000000);
        RankSupport rankSupport = new RankSupport(bitVector, 1000000);
        assertEquals(126998, rankSupport.rank1(126998));
        assertEquals(0, rankSupport.rank1(0));
        assertEquals(37, rankSupport.rank1(37));
        assertEquals(1000, rankSupport.rank1(1000));
        assertEquals(99999, rankSupport.rank1(99999));
        assertEquals(999999, rankSupport.rank1(999999));
    }

    @Test
    void testRank1_1000() {
        BitSet bitVector = new BitSet(1000);
        bitVector.set(0, 1000);
        RankSupport rankSupport = new RankSupport(bitVector, 1000);
        assertEquals(0, rankSupport.rank1(0));
        assertEquals(37, rankSupport.rank1(37));
        assertEquals(999, rankSupport.rank1(999));
    }

    @Test
    void testRank1_someSet() {
        BitSet bitVector = new BitSet(70);
        bitVector.set(10, 30);
        bitVector.set(40, 45);
        bitVector.set(61, 63);
        RankSupport rankSupport = new RankSupport(bitVector, 70);
        assertEquals(0, rankSupport.rank1(9));
        assertEquals(1, rankSupport.rank1(11));
        assertEquals(15, rankSupport.rank1(25));
        assertEquals(20, rankSupport.rank1(31));
        assertEquals(20, rankSupport.rank1(39));
        assertEquals(23, rankSupport.rank1(43));
    }

    @Test
    void testRank1_noneSet() {
        BitSet bitVector = new BitSet(20000);
        RankSupport rankSupport = new RankSupport(bitVector, 20000);
        assertEquals(0, rankSupport.rank1(0));
        assertEquals(0, rankSupport.rank1(37));
        assertEquals(0, rankSupport.rank1(1000));
        assertEquals(0, rankSupport.rank1(19999));
    }

    @Test
    void testRank1_times() {
        BitSet bitVector = new BitSet(1000);
        bitVector.set(0, 1000);
        RankSupport rankSupport = new RankSupport(bitVector, 1000);
        for (int i = 0; i < 10; i++) {
            // Instant start = Instant.now();
            long time = rankSupport.rank1GetTime(i * 10);
            System.out.println(time + " nanoseconds elapsed");
            // Instant end = Instant.now();
            // System.out.println("Instant difference from inside test: " +
            // Duration.between(start, end));
        }
//        BitSet bitVector2 = new BitSet(1000000000);
//        bitVector2.set(0, 1000000000);
//        RankSupport rankSupport2 = new RankSupport(bitVector2, 1000000000);
//        long time = rankSupport2.rank1GetTime(1000000000 / 2);
    }

    /**
     * Measures and plots runtime data.
     * 
     * @throws IOException
     * @throws PythonExecutionException
     */
    @Test
    void testTimesGetPlot() throws IOException, PythonExecutionException {
        List<Integer> valuesOfN = new ArrayList<>();
        List<Double> runtimes = new ArrayList<>();

        Random random = new Random();

        // running these first to throw away; first few runs always take longer
        for (int N = 100; N < 1000; N *= 1.5) {
            BitSet bitVector = new BitSet(N);
            bitVector.set(0, N);
            RankSupport rankSupport = new RankSupport(bitVector, N);
            for (int j = 0; j < 200; j++) {
                rankSupport.rank1GetTime(random.nextInt(0, N));
            }
        }

        // for each value of N, do multiple runs and average them
        int N = 100;
        while (N < 1000000) {
            BitSet bitVector = new BitSet(N);
            bitVector.set(0, N);
            RankSupport rankSupport = new RankSupport(bitVector, N);
            double runningTotal = 0;
            for (int j = 0; j < 200; j++) {
                runningTotal += rankSupport.rank1GetTime(random.nextInt(0, N));
            }
            runningTotal = (runningTotal / 100.0) / Math.pow(10, 3); // convert to microseconds
            valuesOfN.add(N);
            runtimes.add(runningTotal);
            System.out.println(N);
            N = N + 1000;
        }
        // createScatterPlot(valuesOfN, runtimes);
        Plot plt = Plot.create();
        plt.plot().add(valuesOfN, runtimes, "o");
        plt.title("Runtime of Rank Operation");
        // plt.xscale(Scale.log);
        plt.xlabel("Size of Bit Vector");
        plt.ylabel("Runtime in Microseconds");
        // plt.legend().loc("upper right");
        plt.show();
    }

    /**
     * Measures and plots overhead data.
     * 
     * @throws IOException
     * @throws PythonExecutionException
     */
    @Test
    void testOverheadGetPlot() throws IOException, PythonExecutionException {
        List<Integer> valuesOfN = new ArrayList<>();
        List<Double> overhead = new ArrayList<>();

        Random random = new Random();

        // for each value of N, do multiple runs and average them
        int N = 100;
        while (N < 1000000) {
            BitSet bitVector = new BitSet(N);
            // bitVector.set(0, N);
            // bitVector.set(N - 1);

            // setting bitVector at approximately 10% capacity
            for (int i = 0; i < N; i++) {
                if (random.nextInt(0, 100) < 5) {
                    bitVector.set(i);
                }
            }

            RankSupport rankSupport = new RankSupport(bitVector, N);
            valuesOfN.add(N);
            overhead.add((double) rankSupport.overhead() / 8);
            System.out.println(N);
            N = N + 1000;
        }
        // createScatterPlot(valuesOfN, overhead);
        Plot plt = Plot.create();
        plt.plot().add(valuesOfN, overhead, "o").label("Overhead");
        plt.plot()
                .add(valuesOfN,
                        valuesOfN.stream().map(xi -> (10000 * Math.log(xi) / Math.log(2))).collect(Collectors.toList()))
                .label("log(x)");
        plt.title("Overhead of RankSupport");
        plt.yscale(Scale.log);
        plt.xlabel("Length of Bit Vector");
        plt.ylabel("Overhead in Bytes");
        // plt.legend().loc("upper right");
        plt.show();
    }

    private void createScatterPlot(List<? extends Number> x, List<? extends Number> y)
            throws IOException, PythonExecutionException {
        // List<Integer> x = Arrays.asList(1, 10, 100);// NumpyUtils.linspace(-3, 3,
        // 100);
        // List<Double> y = Arrays.asList(.05, .06, .09); // x.stream().map(xi ->
        // Math.sin(xi) +
        // Math.random()).collect(Collectors.toList());

        Plot plt = Plot.create();
        plt.plot().add(x, y, "o").label("sin");
        plt.title("Runtime of Rank Operation");
        // plt.xscale(Scale.log);
        plt.xlabel("Size of Bit Vector");
        plt.ylabel("Runtime in Microseconds");
        // plt.legend().loc("upper right");
        plt.show();
    }

//  private void plotCoordinates() throws IOException, PythonExecutionException {
//      Plot plt = Plot.create();
//      plt.plot().add(Arrays.asList(1.3, 5)).label("label").linestyle("--");
//      plt.xlabel("xlabel");
//      plt.ylabel("ylabel");
//      plt.text(0.5, 0.2, "text");
//      plt.title("Title!");
//      plt.legend();
//      plt.xscale(Scale.log);
//      plt.show();
//  }

    @Test
    public void testPlotOneHistogram() throws IOException, PythonExecutionException {
        Random rand = new Random();
        // List<Double> x = IntStream.range(0, 1000).mapToObj(i ->
        // rand.nextGaussian()).collect(Collectors.toList());

        List<Integer> x = Arrays.asList(5, 6, 7, 6, 5);

        Plot plt = Plot.create();
        plt.hist().add(x);// .orientation(HistBuilder.Orientation.horizontal);
        plt.ylim(-5, 5);
        plt.title("histogram");
        plt.legend().loc("upper right");
        plt.show();
    }

    @Test
    void testIntArrayStorage() {
        byte[] array1 = new byte[] { 1, 2, 3, 4, 1 };
        int[] array2 = new int[] { 100000000, 1000000, 999999999, 2090249020, 235029389 };
        int[] array3 = new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
        int int1 = 1;

        Vector<Integer> intVector = new Vector<>();
        intVector.addAll(
                Arrays.asList(new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 }));

        // intVector

        // These arrays are both the same size, even though ints are very different
        // sizes
        System.out.println("array1: " + ClassLayout.parseInstance(array1).toPrintable());
        long sizeArray1 = ClassLayout.parseInstance(array1).instanceSize() * 8;
        System.out.println("array2: " + ClassLayout.parseInstance(array2).toPrintable());
        long sizeArray2 = ClassLayout.parseInstance(array2).instanceSize() * 8;
        System.out.println("array3: " + ClassLayout.parseInstance(array2).toPrintable());
        long sizeArray3 = ClassLayout.parseInstance(array3).instanceSize() * 8;
        System.out.println("int1: " + ClassLayout.parseInstance(array3).toPrintable());
        long sizeIntVector = ClassLayout.parseInstance(intVector).instanceSize() * 8;
        System.out.println("int1: " + ClassLayout.parseInstance(intVector).toPrintable());

    }

    @Test
    void testLoadAndSaveRankSupport() throws IOException, ClassNotFoundException {
        BitSet bitVector = new BitSet(70);
        bitVector.set(10, 30);
        bitVector.set(40, 45);
        bitVector.set(61, 63);
        RankSupport rankSupport = new RankSupport(bitVector, 70);
        rankSupport.save("src/test/resources/rankSupport.bin");
        RankSupport rankSupport2 = RankSupport.load("src/test/resources/rankSupport.bin");
        assertEquals(rankSupport.getBitVector(), rankSupport2.getBitVector());
        assertEquals(rankSupport.getBitVectorLength(), rankSupport2.getBitVectorLength());
        assertEquals(rankSupport.getChunkSize(), rankSupport2.getChunkSize());
        assertEquals(rankSupport.getSubchunkSize(), rankSupport2.getSubchunkSize());
        assertEquals(rankSupport.getCumulativeRanks()[1], rankSupport2.getCumulativeRanks()[1]);
        assertEquals(rankSupport.getSubchunkCumulativeRanks()[1][1], rankSupport2.getSubchunkCumulativeRanks()[1][1]);
        assertEquals(rankSupport.getNumberOfChunks(), rankSupport2.getNumberOfChunks());
        assertEquals(rankSupport.getNumberOfSubchunks(), rankSupport2.getNumberOfSubchunks());
    }

    @Test
    void testLoadAndSaveBitVector() throws IOException, ClassNotFoundException {
        BitSet bitVector = new BitSet(70);
        bitVector.set(10, 30);
        bitVector.set(40, 45);
        bitVector.set(61, 63);
        RankSupport rankSupport = new RankSupport(bitVector, 70);
        rankSupport.saveBitVector("src/test/resources/bitVector.bin");
        BitSet bitVector2 = RankSupport.loadBitVector("src/test/resources/bitVector.bin");
        assertEquals(bitVector.cardinality(), bitVector2.cardinality());
        assertEquals(bitVector, bitVector2);

    }
}

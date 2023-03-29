package rank;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;

import edu.berkeley.cs.succinct.util.vector.IntVector;

/**
 * Test class for {@link RankSupport}.
 * 
 * @author Valerie Wray
 *
 */
class RankSupportTest {

    @Test
    void testIntVectorAddAndGet() {
        double logValueBase10 = Math.log(10000);
        double logOf2 = Math.log(2);
        double division = logValueBase10 / logOf2;
        int intDivision = (int) Math.ceil(division);
        IntVector vector = new IntVector(10000, intDivision);
        for (int i = 0; i < 10000; i++) {
            vector.add(i, i);
        }
        for (int i = 0; i < 10000; i++) {
            assertEquals(i, vector.get(i));
        }
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
        IntVector cumulativeRanks = rankSupport.getCumulativeRanks();
        assertEquals(7, cumulativeRanks.getBitWidth());
        assertEquals(0, cumulativeRanks.get(0));
        assertEquals(49, cumulativeRanks.get(1));
        assertEquals(98, cumulativeRanks.get(2));
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
        IntVector subchunkCumulativeRanks = rankSupport.getSubchunkCumulativeRanks();

        assertEquals(49, rankSupport.getChunkSize());
        assertEquals(3, rankSupport.getSubchunkSize());
        assertEquals(3, rankSupport.getNumberOfChunks());
        assertEquals(17, rankSupport.getNumberOfSubchunks());
        assertEquals(7, rankSupport.getCumulativeRanks().getBitWidth());

        assertEquals(5, rankSupport.rank1(5));

        assertEquals(6, subchunkCumulativeRanks.getBitWidth());
        assertEquals(0, subchunkCumulativeRanks.get(0));
        assertEquals(3, subchunkCumulativeRanks.get(1));
        assertEquals(6, subchunkCumulativeRanks.get(2));
        assertEquals(9, subchunkCumulativeRanks.get(3));
        assertEquals(0, subchunkCumulativeRanks.get(49));
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

    /**
     * Measures and plots runtime data.
     * 
     * @throws IOException
     * @throws PythonExecutionException
     */
    // @Test
    void testTimesGetPlot() throws IOException, PythonExecutionException {
        List<Integer> valuesOfN = new ArrayList<>();
        List<Double> runtimes = new ArrayList<>();

        Random random = new Random();

        // running these first to throw away; first few runs always take longer
        for (int N = 100; N < 1000; N++) {
            BitSet bitVector = new BitSet(N);
            bitVector.set(0, N);
            RankSupport rankSupport = new RankSupport(bitVector, N);
            for (int j = 0; j < 200; j++) {
                rankSupport.rank1GetTime(random.nextInt(0, N));
            }
        }

        // for each value of N, do multiple runs and average them
        int N = 100;
        while (N < 5000000) {
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
            N = N + 10000;
        }
        Plot plt = Plot.create();
        plt.plot().add(valuesOfN, runtimes, "o");
        plt.title("Runtime of Rank Operation");
        plt.xlabel("Size of Bit Vector");
        plt.ylabel("Runtime in Microseconds");
        plt.show();
    }

    /**
     * Measures and plots overhead data.
     * 
     * @throws IOException
     * @throws PythonExecutionException
     */
    // @Test
    void testOverheadGetPlot() throws IOException, PythonExecutionException {
        List<Integer> valuesOfN = new ArrayList<>();
        List<Double> overhead = new ArrayList<>();
        List<Double> overheadHalfCapacity = new ArrayList<>();

        // for each value of N, do multiple runs and average them
        int N = 100;
        while (N < 1000000) {
            BitSet bitVector = new BitSet(N);
            // setting bit-vector at full capacity
            bitVector.set(0, N);

            RankSupport rankSupport = new RankSupport(bitVector, N);
            valuesOfN.add(N);
            overhead.add((double) rankSupport.overhead());
            System.out.println(N);
            N = N + 1000;
        }

        // for each value of N, do multiple runs and average them
        N = 100;
        while (N < 1000000) {
            BitSet bitVector = new BitSet(N);
            // setting bit-vector at half capacity
            setRandomCapacityBits(bitVector, .5, N);

            RankSupport rankSupport = new RankSupport(bitVector, N);
            overheadHalfCapacity.add((double) rankSupport.overhead());
            System.out.println(N);
            N = N + 1000;
        }
        Plot plt = Plot.create();
        plt.plot().add(valuesOfN, overhead).label("Overhead 100% capacity");
        plt.plot().add(valuesOfN, overheadHalfCapacity).label("Overhead 50% capacity");
        plt.plot().add(valuesOfN, valuesOfN.stream().map(xi -> xi).collect(Collectors.toList())).label("N");
        plt.title("Overhead of RankSupport");
        plt.xlabel("Length of Bit Vector");
        plt.ylabel("Overhead in Bits");
        plt.legend();
        plt.show();
    }

    private void setRandomCapacityBits(BitSet bitVector, double capacity, int N) {
        int section = (int) (N * capacity);
        System.out.println("section: " + section);
        Random random = new Random();
        for (int i = 1; i <= section; i++) {

            int lowerBound = (N / section) * (i - 1);
            int upperBound = (N / section) * i;
            int randomPosition = random.nextInt(lowerBound, upperBound);

            bitVector.set(randomPosition);
        }
    }

    @Test
    void testLoadAndSaveRankSupport() throws IOException, ClassNotFoundException {
        BitSet bitVector = new BitSet(70);
        bitVector.set(10, 30);
        bitVector.set(40, 45);
        bitVector.set(61, 63);
        RankSupport rankSupport = new RankSupport(bitVector, 70);
        rankSupport.save("src/test/resources/rankSupport5.bin");
        RankSupport rankSupport2 = RankSupport.load("src/test/resources/rankSupport5.bin");
        assertEquals(rankSupport.getBitVector(), rankSupport2.getBitVector());
        assertEquals(rankSupport.getBitVectorLength(), rankSupport2.getBitVectorLength());
        assertEquals(rankSupport.getChunkSize(), rankSupport2.getChunkSize());
        assertEquals(rankSupport.getSubchunkSize(), rankSupport2.getSubchunkSize());
        assertEquals(rankSupport.getCumulativeRanks().get(0), rankSupport2.getCumulativeRanks().get(0));
        assertEquals(rankSupport.getSubchunkCumulativeRanks().get(1), rankSupport2.getSubchunkCumulativeRanks().get(1));
        int index = rankSupport.getNumberOfChunks() * rankSupport.getNumberOfSubchunks()
                - rankSupport.getNumberOfSubchunks();
        assertEquals(rankSupport.getSubchunkCumulativeRanks().get(index),
                rankSupport2.getSubchunkCumulativeRanks().get(index));
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

package select;

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

/**
 * Test class for {@link SelectSupport}.
 * 
 * @author Valerie Wray
 *
 */
class SelectSupportTest {

    @Test
    void testSelect1_allSet() {
        BitSet bitVector = new BitSet(128);
        bitVector.set(0, 128);
        SelectSupport selectSupport = new SelectSupport(bitVector, 128);
        assertEquals(8, selectSupport.select1(8));
        assertEquals(10, selectSupport.select1(10));
        assertEquals(18, selectSupport.select1(18));
        assertEquals(127, selectSupport.select1(127));
    }

    @Test
    void testSelect1_someSet() {
        BitSet bitVector = new BitSet(70);
        bitVector.set(10, 30);
        bitVector.set(40, 45);
        bitVector.set(61, 63);
        SelectSupport selectSupport = new SelectSupport(bitVector, 70);
        assertEquals(0, selectSupport.select1(0));
        assertEquals(11, selectSupport.select1(1));
        assertEquals(25, selectSupport.select1(15));
        assertEquals(30, selectSupport.select1(20));
        assertEquals(41, selectSupport.select1(21));
        assertEquals(62, selectSupport.select1(26));
    }

    @Test
    void testLoadAndSaveSelectSupport() throws IOException, ClassNotFoundException {
        BitSet bitVector = new BitSet(70);
        bitVector.set(10, 30);
        bitVector.set(40, 45);
        bitVector.set(61, 63);
        SelectSupport selectSupport = new SelectSupport(bitVector, 70);
        selectSupport.save("src/test/resources/selectSupport.bin");
        SelectSupport selectSupport2 = SelectSupport.load("src/test/resources/selectSupport.bin");
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

    @Test
    void testLoadAndSaveBitVector() throws IOException, ClassNotFoundException {
        BitSet bitVector = new BitSet(70);
        bitVector.set(10, 30);
        bitVector.set(40, 45);
        bitVector.set(61, 63);
        SelectSupport selectSupport = new SelectSupport(bitVector, 70);
        selectSupport.saveBitVector("src/test/resources/selectSupportBitVector.bin");
        BitSet bitVector2 = SelectSupport.loadBitVector("src/test/resources/selectSupportBitVector.bin");
        assertEquals(bitVector.cardinality(), bitVector2.cardinality());
        assertEquals(bitVector, bitVector2);

    }

    /**
     * Measures and plots runtime data.
     * 
     * @throws IOException
     * @throws PythonExecutionException
     */
    @Test
    void testRuntimeGetPlot() throws IOException, PythonExecutionException {
        List<Integer> valuesOfN = new ArrayList<>();
        List<Double> runtimes = new ArrayList<>();

        Random random = new Random();

        // running these first to throw away; first few runs always take longer
        for (int N = 100; N < 1000; N += 1) {
            BitSet bitVector = new BitSet(N);
            bitVector.set(0, N);
            SelectSupport selectSupport = new SelectSupport(bitVector, N);
            for (int j = 0; j < 200; j++) {
                selectSupport.select1GetTime(random.nextInt(0, N));
            }
        }

        // for each value of N, do multiple runs and average them
        int N = 100;
        while (N < 5000000) {
            BitSet bitVector = new BitSet(N);
            bitVector.set(0, N);

            // setRandomCapacityBits(bitVector, .2, N);
            SelectSupport selectSupport = new SelectSupport(bitVector, N);
            // double[] runtimesToAverage = new double[50];
            double runningTotal = 0;
            for (int j = 0; j < 200; j++) {
                runningTotal += selectSupport.select1GetTime(random.nextInt(0, N));
            }
            runningTotal = (runningTotal / 100.0) / Math.pow(10, 3); // convert to microseconds
            valuesOfN.add(N);
            runtimes.add(runningTotal);
            // i++;
            System.out.println(N);
            N = N + 10000;
        }
        createScatterPlot(valuesOfN, runtimes,
                valuesOfN.stream().map(xi -> (Math.log(xi) / Math.log(2)) / 2 - 2).collect(Collectors.toList()));
    }

    private void createScatterPlot(List<? extends Number> x, List<? extends Number> y, List<? extends Number> z)
            throws IOException, PythonExecutionException {
        Plot plt = Plot.create();
        plt.plot().add(x, y, "o").label("select");
        plt.plot().add(x, z).label("log(N)/2 - 2");
        plt.title("Runtime of Select Operation");
        plt.xlabel("Size of Bit Vector");
        plt.ylabel("Runtime in Microseconds");
        plt.legend().loc("upper right");
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
            // setting bit-vector at full capacity
            BitSet bitVector = new BitSet(N);
            bitVector.set(0, N);

            SelectSupport selectSupport = new SelectSupport(bitVector, N);
            valuesOfN.add(N);
            overhead.add((double) selectSupport.overhead());
            System.out.println(N);
            N = N + 1000;
        }

        // for each value of N, do multiple runs and average them
        N = 100;
        while (N < 1000000) {
            BitSet bitVector = new BitSet(N);
            // setting bit-vector at half capacity
            setRandomCapacityBits(bitVector, .5, N);

            SelectSupport selectSupport = new SelectSupport(bitVector, N);
            overheadHalfCapacity.add((double) selectSupport.overhead());
            System.out.println(N);
            N = N + 1000;
        }
        Plot plt = Plot.create();
        plt.plot().add(valuesOfN, overhead).label("Overhead 100% capacity");
        plt.plot().add(valuesOfN, overheadHalfCapacity).label("Overhead 50% capacity");
        plt.plot().add(valuesOfN, valuesOfN.stream().map(xi -> xi).collect(Collectors.toList())).label("N");
        plt.title("Overhead of SelectSupport");
        plt.xlabel("Length of Bit Vector");
        plt.ylabel("Overhead in Bits");
        plt.legend();
        plt.show();
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

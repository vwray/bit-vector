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
        assertEquals(selectSupport.getCumulativeRanks()[1], selectSupport2.getCumulativeRanks()[1]);
        assertEquals(selectSupport.getSubchunkCumulativeRanks()[1][1],
                selectSupport2.getSubchunkCumulativeRanks()[1][1]);
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
    void plotCoords() throws IOException, PythonExecutionException {
        List<Integer> valuesOfN = new ArrayList<>();
        List<Double> runtimes = new ArrayList<>();

        Random random = new Random();

        // running these first to throw away; first few runs always take longer
//        for (int N = 100; N < 1000; N *= 1.5) {
//            BitSet bitVector = new BitSet(N);
//            bitVector.set(0, N);
//            SelectSupport selectSupport = new SelectSupport(bitVector, N);
//            for (int j = 0; j < 200; j++) {
//                selectSupport.select1GetTime(random.nextInt(0, N));
//            }
//        }

        // for each value of N, do multiple runs and average them
        // int i = 1;
        // for (int N = 100; N < 1000000000; N *= 1.5) {
        int N = 100;
        while (N < 1000000) {
            BitSet bitVector = new BitSet(N);
            bitVector.set(0, N);
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
            // N = (int) Math.max(1.5 * N, N + 100);
            N = N + 1000;
        }
        createScatterPlot(valuesOfN, runtimes,
                valuesOfN.stream().map(xi -> (Math.log(xi) / Math.log(2))).collect(Collectors.toList()));
        // createScatterPlot(valuesOfN, valuesOfN.stream().map(xi ->
        // Math.log(xi)).collect(Collectors.toList()));
    }

    private void createScatterPlot(List<? extends Number> x, List<? extends Number> y, List<? extends Number> z)
            throws IOException, PythonExecutionException {
        // List<Integer> x = Arrays.asList(1, 10, 100);// NumpyUtils.linspace(-3, 3,
        // 100);
        // List<Double> y = Arrays.asList(.05, .06, .09); // x.stream().map(xi ->
        // Math.sin(xi) +
        // Math.random()).collect(Collectors.toList());

        Plot plt = Plot.create();
        plt.plot().add(x, y, "o").label("select");
        plt.plot().add(x, z).label("log(x)");
        // plt.plot().add(x, ((List<Double>)x).stream().map(xi -> Math.sin(xi)).collect(
        // Collectors.toList(), "o")).label("log");
        plt.title("Runtime of Select Operation");
        // plt.xscale(Scale.log);
        plt.xlabel("Size of Bit Vector");
        plt.ylabel("Runtime in Microseconds");
        plt.legend().loc("upper right");
        plt.show();
    }

}

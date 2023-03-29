package select;

import java.io.IOException;
import java.util.BitSet;

import org.slf4j.profiler.Profiler;

import rank.RankSupport;

/**
 * A class to provide select support as an extension to {@link RankSupport}.
 * 
 * @author Valerie Wray
 *
 */
public class SelectSupport extends RankSupport {
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new SelectSupport from the bit-vector and the length.
     * 
     * @param bitVector       the {@link BitSet}
     * @param bitVectorLength the length of the {@link BitSet}
     */
    public SelectSupport(BitSet bitVector, int bitVectorLength) {
        super(bitVector, bitVectorLength);
        // TODO Auto-generated constructor stub
    }

    /**
     * Gets the position, in the underlying bit-vector, of the FIRST index, j for
     * which rank1(j) = i.
     * 
     * @param i the rank in the bit-vector of which to select the first index having
     *          that rank
     * @return the index in the bit-vector
     */
    public int select1(int i) {
        // binary search calling rank1
        int left = 0;
        int leftRank = 0;
        int right = getBitVectorLength();
        // int rightRank = right;
        int center;
        int centerRank;
        while (true) {
            center = (left + right) / 2;
            centerRank = rank1(center);
            if (i <= centerRank) {
                if (left == center - 1) {
                    return (leftRank == i ? left : center);
                }
                // bisect left
                right = center;
            } else {
                if (right == center + 1) {
                    return (right);
                }
                // bisect right
                left = center;
                leftRank = centerRank;
            }
        }
    }

    /**
     * Computes the select 1 of this bit vector at position i in log time and
     * returns the time elapsed.
     * 
     * @param i the rank in the bit-vector of which to select the first index having
     *          that rank
     * @return the time
     */
    public long select1GetTime(int i) {
        Profiler myProfiler = new Profiler("MYPROFILER");
        myProfiler.start("Computing select1 of " + i);

        select1(i);
        // Instant end = Instant.now();
        // System.out.println("Instant difference from inside rank1GetTime: " +
        // Duration.between(start, end));

        myProfiler.stop();// .print();
        return myProfiler.elapsedTime();

    }

    @Override
    public void save(String filename) throws IOException {
        super.save(filename);
    }

    @Override
    public void saveBitVector(String filename) throws IOException {
        super.saveBitVector(filename);
    }

    /**
     * Loads the SelectSupport from the specified file.
     * 
     * @param filename the file name of the file containing the SelectSupport to
     *                 load
     * @return the RankSupport
     * @throws IOException            if the file is not able to be loaded
     * @throws ClassNotFoundException if a class is not able to be found during
     *                                deserialization
     */
    public static SelectSupport load(String filename) throws IOException, ClassNotFoundException {
        return (SelectSupport) RankSupport.load(filename);
    }

    /**
     * Loads the bit-vector from the specified file.
     * 
     * @param filename the file name of the file containing the bit-vector to load
     * @return the {@link BitSet}
     * @throws IOException            if the file is not able to be loaded
     * @throws ClassNotFoundException if a class is not able to be found during
     *                                deserialization
     */
    public static BitSet loadBitVector(String filename) throws IOException, ClassNotFoundException {
        return RankSupport.loadBitVector(filename);
    }

}

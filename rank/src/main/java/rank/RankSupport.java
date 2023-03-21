/**
 * 
 */
package rank;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.BitSet;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.info.GraphLayout;
import org.slf4j.profiler.Profiler;

import net.kothar.compactlist.CompactList;

/**
 * A class to provide rank support as a wrapper to an underlying {@link BitSet}
 * which allows bit vector access.
 * 
 * @author Valerie Wray
 *
 */
public class RankSupport implements Serializable {
    private static final long serialVersionUID = 1L;
    private BitSet bitVector;
    private int bitVectorLength;
    private int chunkSize;
    private int numberOfChunks;
    private int[] cumulativeRanks;
    private IntArrayList cumulativeRanks2;
    private CompactList cumulativeRanks3;
    private CompactIntArray cumulativeRanks4;
    private int subchunkSize;
    private int numberOfSubchunks;
    private int[][] subchunkCumulativeRanks;
    private IntArrayList subchunkCumulativeRanks2;
    private CompactList subchunkCumulativeRanks3;
    private CompactIntArray subchunkCumulativeRanks4;

    /**
     * Creates a new RankSupport from the bit vector and the length.
     * 
     * @param bitVector       the {@link BitSet}
     * @param bitVectorLength the length of the {@link BitSet}
     */
    public RankSupport(BitSet bitVector, int bitVectorLength) {
        this.bitVector = bitVector;
        this.bitVectorLength = bitVectorLength;
        int logValue = (int) (Math.log(bitVectorLength) / Math.log(2));
        chunkSize = (int) Math.pow(logValue, 2);
        numberOfChunks = (int) Math.ceil((double) bitVectorLength / (double) chunkSize);
        cumulativeRanks = new int[numberOfChunks];
        cumulativeRanks2 = new IntArrayList(numberOfChunks);
        cumulativeRanks3 = new CompactList();
        // maybe second param can be smaller
        cumulativeRanks4 = new CompactIntArray(numberOfChunks,
                Math.min(bitVector.cardinality(), bitVectorLength - chunkSize));
        subchunkSize = logValue / 2;
        numberOfSubchunks = (int) Math.ceil((double) chunkSize / (double) subchunkSize); // logValue * 2;
        subchunkCumulativeRanks = new int[numberOfChunks][numberOfSubchunks];
        subchunkCumulativeRanks2 = new IntArrayList(numberOfChunks * numberOfSubchunks);
        subchunkCumulativeRanks3 = new CompactList();
        subchunkCumulativeRanks4 = new CompactIntArray(numberOfChunks * numberOfSubchunks, chunkSize - subchunkSize);
        precomputeCumulativeRanks();
        precomputeSubchunkCumulativeRanks();
        cumulativeRanks3.compact();
        subchunkCumulativeRanks3.compact();
    }

    /**
     * Precomputes the cumulative ranks for each chunk and stores them in the
     * cumulativeRanks array.
     */
    protected void precomputeCumulativeRanks() {
        // TODO: combine this with precomputing subchunks; just add cumulativeRank var
        // as separate counter

        // step through bit vector, store rank of beginning index of each chunk
        int cumulativeRank = 0;
        for (int i = 0; i < bitVectorLength; i++) {
            if (i % chunkSize == 0) {
                int division = i / chunkSize;
                // int division = (int) (i / chunkSize);
                // if (division < numberOfChunks) {
                cumulativeRanks[division] = cumulativeRank;
                cumulativeRanks2.add(cumulativeRank);
                cumulativeRanks3.addLong(cumulativeRank);
                cumulativeRanks4.add(division, cumulativeRank);
                // }
            }
            if (bitVector.get(i)) {
                cumulativeRank++;
            }
        }
    }

    /**
     * Precomputes the cumulative ranks of each subchunk in each chunk.
     */
    protected void precomputeSubchunkCumulativeRanks() {
//        for (int chunk = 0; chunk < numberOfChunks; chunk++) {
//            for (int subchunk = 0; subchunk < numberOfSubchunks; subchunk++) {
//
//            }
//        }

//        int currentChunk = 0;
//        int currentSubchunk = 0;
        int subchunkCumulativeRank = 0;
        int modChunkSize = 0;
        for (int i = 0; i < bitVectorLength; i++) {
            modChunkSize = i % chunkSize;
            // if (modChunkSize == 0) {
            // int division = (int) (i / chunkSize);
            // if (division < numberOfChunks) {
            // cumulativeRanks[division] = subchunkCumulativeRank;
            // currentChunk++;
            // }
            // }
            if (modChunkSize % subchunkSize == 0) {
                // subchunkCumulativeRanks[currentChunk][currentSubchunk] =
                // subchunkCumulativeRank;
                // currentSubchunk++;

                subchunkCumulativeRanks[i / chunkSize][modChunkSize / subchunkSize] = modChunkSize == 0 ? 0
                        : subchunkCumulativeRank;
                subchunkCumulativeRanks2.add(modChunkSize == 0 ? 0 : subchunkCumulativeRank);
                subchunkCumulativeRanks3.addLong(modChunkSize == 0 ? 0 : subchunkCumulativeRank);
                subchunkCumulativeRanks4.add((i / chunkSize) + (modChunkSize / subchunkSize),
                        modChunkSize == 0 ? 0 : subchunkCumulativeRank);
                if (modChunkSize == 0) {
                    subchunkCumulativeRank = 0;
                }
            }
            if (bitVector.get(i)) {
                subchunkCumulativeRank++;
            }
        }
    }

    /**
     * Computes the rank 1 of this bit vector at position i in constant time.
     * 
     * @param i the position of which to compute the rank
     * @return the rank
     */
    public int rank1(int i) {
        // find what chunk it is in
        int chunkNumber = i / chunkSize;
        // look up cumulative rank
        int cumulativeRank = cumulativeRanks2.get(chunkNumber);// cumulativeRanks[chunkNumber];
        // find cumulative rank in subchunk
        int subchunkNumber = (i % chunkSize) / subchunkSize;
        int subchunkCumulativeRank = subchunkCumulativeRanks2.get(chunkNumber * numberOfSubchunks + subchunkNumber);
        // subchunkCumulativeRanks[chunkNumber][subchunkNumber];
        // cardinality uses population count to find relative rank within subchunk
        int relativeSubchunkRank = bitVector.get(i - ((i % chunkSize) % subchunkSize), i).cardinality();
        // add results
        return cumulativeRank + subchunkCumulativeRank + relativeSubchunkRank;
    }

    /**
     * Computes the rank 1 of this bit vector at position i in constant time and
     * returns the time elapsed.
     * 
     * @param i the position of which to compute the rank
     * @return the rank
     */
    // @Loggable(Loggable.INFO)
    public long rank1GetTime(int i) {
        // Instant start = Instant.now();
        // TODO: try Instant.now() to calculate time inside here and compare diff
        Profiler myProfiler = new Profiler("MYPROFILER");
        myProfiler.start("Computing rank1 of " + i);

        rank1(i);
        // Instant end = Instant.now();
        // System.out.println("Instant difference from inside rank1GetTime: " +
        // Duration.between(start, end));

        myProfiler.stop();// .print();
        return myProfiler.elapsedTime();

    }

    /**
     * Computes the overhead in number of bits being used by this class. Adds up the
     * bits in use by each of the following structures and variables:
     * <ul>
     * <li>private BitSet bitVector;</li>
     * <li>private int bitVectorLength;</li>
     * <li>private int chunkSize;</li>
     * <li>private int numberOfChunks;</li>
     * <li>private int[] cumulativeRanks;</li>
     * <li>private int subchunkSize;</li>
     * <li>private int numberOfSubchunks;</li>
     * <li>private int[][] subchunkCumulativeRanks;</li>
     * </ul>
     * 
     * @return the number of bits as a long
     */
    public long overhead() {
        return 24 * 8 + // bitVector.size()) + // (16 * 8 + numberOfChunks * 32) // 32 bits to store
        // each int in
        // cumulativeRanks
        // + (16 * 8 + numberOfChunks * numberOfSubchunks * 32) // 32 bits to store each
        // int in
        // subchunkCumulativeRanks
        // 24 bytes of object overhead
                24 * 8 + cumulativeRanks4.getBitsForArray()

                + 24 * 8 + subchunkCumulativeRanks4.getBitsForArray()

                // GraphLayout.parseInstance(cumulativeRanks4).totalSize() * 8
                // + GraphLayout.parseInstance(subchunkCumulativeRanks4).totalSize() * 8

                + 32 * 5; // for bitVectorLength,
                          // chunkSize,
                          // numberOfChunks,
                          // subChunkSize, and
                          // numberOfSubchunks

    }

    /**
     * Computes the overhead in number of bits being used by this class.
     * 
     * @return the number of bits as a long
     */
    public long overheadVia3rdParty() {
        System.out.println(ClassLayout.parseInstance(this).toPrintable());
        return ClassLayout.parseInstance(this).instanceSize() * 8
                + ClassLayout.parseInstance(cumulativeRanks).instanceSize() * 8
                + ClassLayout.parseInstance(subchunkCumulativeRanks).instanceSize() * 8
                + ClassLayout.parseInstance(bitVector).instanceSize() * 8;
    }

    public long overheadVia3rdPartyGraphLayout() {
        System.out.println(GraphLayout.parseInstance(this).toPrintable());
        // System.out.println(GraphLayout.parseInstance(this).toFootprint());
        return GraphLayout.parseInstance(this).totalSize() * 8;
    }

    /**
     * Saves this RankSupport to a file with the specified file name.
     * 
     * @param filename the file name of the file to save
     * @throws IOException if the file is not able to be saved
     */
    public void save(String filename) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filename));
        objectOutputStream.writeObject(this);
        objectOutputStream.close();
    }

    /**
     * Loads the RankSupport from the specified file.
     * 
     * @param filename the file name of the file containing the RankSupport to load
     * @return the RankSupport
     * @throws IOException            if the file is not able to be loaded
     * @throws ClassNotFoundException if a class is not able to be found during
     *                                deserialization
     */
    public static RankSupport load(String filename) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(filename));
        RankSupport rankSupport = (RankSupport) objectInputStream.readObject();
        objectInputStream.close();
        return rankSupport;
    }

    /**
     * Saves this bit vector to a file with the specified file name.
     * 
     * @param filename the file name of the file to save
     * @throws IOException if the file is not able to be saved
     */
    public void saveBitVector(String filename) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filename));
        objectOutputStream.writeObject(bitVector);
        objectOutputStream.close();
    }

    /**
     * Loads the bit vector from the specified file.
     * 
     * @param filename the file name of the file containing the bit vector to load
     * @return the {@link BitSet}
     * @throws IOException            if the file is not able to be loaded
     * @throws ClassNotFoundException if a class is not able to be found during
     *                                deserialization
     */
    public static BitSet loadBitVector(String filename) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(filename));
        BitSet bitSet = (BitSet) objectInputStream.readObject();
        objectInputStream.close();
        return bitSet;
    }

    public boolean access(int index) {
        return bitVector.get(index);
    }

    public int[] getCumulativeRanks() {
        return cumulativeRanks;
    }

    public int[][] getSubchunkCumulativeRanks() {
        return subchunkCumulativeRanks;
    }

    public BitSet getBitVector() {
        return bitVector;
    }

    public int getBitVectorLength() {
        return bitVectorLength;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    public int getSubchunkSize() {
        return subchunkSize;
    }

    public int getNumberOfSubchunks() {
        return numberOfSubchunks;
    }
}

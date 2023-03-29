/**
 * 
 */
package rank;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.BitSet;

import org.slf4j.profiler.Profiler;

import edu.berkeley.cs.succinct.util.vector.IntVector;
import sparsearray.SparseArray;

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
    private transient IntVector cumulativeRanks;
    private int subchunkSize;
    private int numberOfSubchunks;
    private transient IntVector subchunkCumulativeRanks;

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
        chunkSize = (int) Math.ceil(Math.pow(logValue, 2));
        numberOfChunks = (int) Math.ceil((double) bitVectorLength / (double) chunkSize);

        subchunkSize = logValue / 2;
        numberOfSubchunks = (int) Math.ceil((double) chunkSize / (double) subchunkSize);

        initializeCumulativeRanks();
        precomputeCumulativeRanks();
    }

    /**
     * Takes a first pass through the bit-vector to determine the minimum bit
     * lengths needed for the rank supporting data structures to store the
     * precomputed ranks.
     */
    private void initializeCumulativeRanks() {
        int maxCumulativeRank = 0;
        int maxSubchunkCumulativeRank = 0;
        int cumulativeRank = 0;
        int subchunkCumulativeRank = 0;
        int modChunkSize = 0;
        for (int i = 0; i < bitVectorLength; i++) {
            modChunkSize = i % chunkSize;
            if (modChunkSize % subchunkSize == 0) {
                if (modChunkSize == 0) {
                    if (subchunkCumulativeRank > maxSubchunkCumulativeRank) {
                        maxSubchunkCumulativeRank = subchunkCumulativeRank;
                    }
                    // reset subchunk cumulative rank
                    subchunkCumulativeRank = 0;

                    if (cumulativeRank > maxCumulativeRank) {
                        maxCumulativeRank = cumulativeRank;
                    }
                }
            }
            if (bitVector.get(i)) {
                // increment cumulative ranks if bit vector entry is set
                subchunkCumulativeRank++;
                cumulativeRank++;
            }
        }

        if (maxCumulativeRank == 0) {
            maxCumulativeRank++;
        }

        if (maxSubchunkCumulativeRank == 0) {
            maxSubchunkCumulativeRank++;
        }

        cumulativeRanks = new IntVector(numberOfChunks, (int) Math.ceil(Math.log(maxCumulativeRank) / Math.log(2)));

        subchunkCumulativeRanks = new IntVector(
                (numberOfChunks - 1) * numberOfSubchunks
                        + (bitVectorLength % chunkSize == 0 ? numberOfSubchunks : bitVectorLength % chunkSize),
                // numberOfChunks * numberOfSubchunks,
                (int) Math.ceil(Math.log(maxSubchunkCumulativeRank) / Math.log(2)));

    }

    /**
     * Precomputes the cumulative ranks of each chunk and subchunk.
     */
    protected void precomputeCumulativeRanks() {
        int cumulativeRank = 0;
        int subchunkCumulativeRank = 0;
        int modChunkSize = 0;
        int subchunkIndex = 0;
        for (int i = 0; i < bitVectorLength; i++) {
            modChunkSize = i % chunkSize;
            if (modChunkSize % subchunkSize == 0) {
                // compute subchunk cumulative rank
                subchunkCumulativeRanks.add(subchunkIndex, modChunkSize == 0 ? 0 : subchunkCumulativeRank);
                subchunkIndex++;
                if (modChunkSize == 0) {
                    // reset subchunk cumulative rank
                    subchunkCumulativeRank = 0;

                    // compute cumulative rank
                    cumulativeRanks.add(i / chunkSize, cumulativeRank);
                }
            }
            if (bitVector.get(i)) {
                // increment cumulative ranks if bit vector entry is set
                subchunkCumulativeRank++;
                cumulativeRank++;
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
        int cumulativeRank = cumulativeRanks.get(chunkNumber);
        // find cumulative rank in subchunk
        int subchunkNumber = (i % chunkSize) / subchunkSize;
        int subchunkCumulativeRank = subchunkCumulativeRanks.get(chunkNumber * numberOfSubchunks + subchunkNumber);

        // slice and then call cardinality which uses popcount to find relative rank
        // within subchunk
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
    public long rank1GetTime(int i) {
        Profiler myProfiler = new Profiler("RankSupportProfiler");
        myProfiler.start("Timing rank1 of " + i);

        rank1(i);

        myProfiler.stop();
        return myProfiler.elapsedTime();

    }

    /**
     * Computes the overhead in number of bits being used by this class. Adds up the
     * bits in use by each of the following structures and variables, not including
     * the bit-vector:
     * <ul>
     * <li>private IntVector cumulativeRanks;</li>
     * <li>private IntVector subchunkCumulativeRanks;</li>
     * <li>private int bitVectorLength;</li>
     * <li>private int chunkSize;</li>
     * <li>private int numberOfChunks;</li>
     * <li>private int subchunkSize;</li>
     * <li>private int numberOfSubchunks;</li>
     * </ul>
     * 
     * @return the number of bits as a long
     */
    public long overhead() {
        return cumulativeRanks.serializedSize() * 8 + subchunkCumulativeRanks.serializedSize() * 8 + 32 * 5;
    }

    /**
     * Saves this RankSupport to a file with the specified file name.
     * 
     * @param filename the file name of the file to save
     * @throws IOException if the file is not able to be saved
     */
    public void save(String filename) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(filename));
        DataOutputStream dataOutputStream = new DataOutputStream(objectOutputStream);
        objectOutputStream.writeObject(this);
        // IntVector is not serializeable, so we serialize separately
        cumulativeRanks.writeToStream(dataOutputStream);
        subchunkCumulativeRanks.writeToStream(dataOutputStream);
        objectOutputStream.close();
    }

    /**
     * Saves this RankSupport using the specified {@link ObjectOutputStream}. Used
     * by {@link SparseArray} to save this RankSupport.
     * 
     * @param objectOutputStream the output stream to write to
     * @throws IOException if the file is not able to be saved
     */
    public void save(ObjectOutputStream objectOutputStream) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(objectOutputStream);
        objectOutputStream.writeObject(this);
        cumulativeRanks.writeToStream(dataOutputStream);
        subchunkCumulativeRanks.writeToStream(dataOutputStream);
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
        DataInputStream dataInputStream = new DataInputStream(objectInputStream);
        RankSupport rankSupport = (RankSupport) objectInputStream.readObject();
        // IntVector is not serializeable, so we deserialize separately
        rankSupport.setCumulativeRanks(IntVector.readFromStream(dataInputStream));
        rankSupport.setSubchunkCumulativeRanks(IntVector.readFromStream(dataInputStream));
        objectInputStream.close();
        return rankSupport;
    }

    /**
     * Loads the RankSupport using the specified {@link ObjectInputStream}. Used by
     * {@link SparseArray} to save this RankSupport.
     * 
     * @param objectInputStream the input stream to read from
     * @return the RankSupport
     * @throws IOException            if the file is not able to be loaded
     * @throws ClassNotFoundException if a class is not able to be found during
     *                                deserialization
     */
    public static RankSupport load(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        DataInputStream dataInputStream = new DataInputStream(objectInputStream);
        RankSupport rankSupport = (RankSupport) objectInputStream.readObject();
        rankSupport.setCumulativeRanks(IntVector.readFromStream(dataInputStream));
        rankSupport.setSubchunkCumulativeRanks(IntVector.readFromStream(dataInputStream));
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

    public IntVector getCumulativeRanks() {
        return cumulativeRanks;
    }

    public IntVector getSubchunkCumulativeRanks() {
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

    public void setCumulativeRanks(IntVector cumulativeRanks) {
        this.cumulativeRanks = cumulativeRanks;
    }

    public void setSubchunkCumulativeRanks(IntVector subchunkCumulativeRanks) {
        this.subchunkCumulativeRanks = subchunkCumulativeRanks;
    }
}

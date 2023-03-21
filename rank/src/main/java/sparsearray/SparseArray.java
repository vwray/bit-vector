package sparsearray;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import select.SelectSupport;

/**
 * Represents a sparse array with an underlying bit vector and densely packed
 * array list.
 * 
 * @author Valerie Wray
 *
 */
public class SparseArray implements Serializable {
    private static final long serialVersionUID = 1L;
    private int size;
    private BitSet bitVector;
    private List<String> denseValues;
    private SelectSupport selectSupport;

    /**
     * Creates an empty sparse array of the specified size.
     * 
     * @param size the size of the sparse array to create
     */
    public SparseArray(int size) {
        create(size);
    }

    /**
     * Creates an empty sparse array of the specified size. (Use SparseArray
     * constructor instead of calling this directly.)
     * 
     * @param size the size of the sparse array to create
     */
    public void create(int size) {
        this.size = size;
        bitVector = new BitSet(size);
        denseValues = new ArrayList<>();
    }

    /**
     * Appends the element <code>element</code> at index <code>position</code> in
     * the sparse array.
     * 
     * @param element  the element to append
     * @param position the index in the sparse array
     */
    public void append(String element, int position) {
        if (position >= size) {
            System.out.println("Error: position is greater than size. Cannot append element " + element);
            return;
        }
        bitVector.set(position);
        denseValues.add(element);
    }

    /**
     * Finalizes the elements in the sparse array and creates the
     * {@link SelectSupport} with built-in rank and select support.
     */
    public void finalize() {
        selectSupport = new SelectSupport(bitVector, size);
    }

    /**
     * Gets the rth present element from the sparse array. Returns false if the
     * sparse array contains less than r items.
     * 
     * @param r       the index of the present element to get
     * @param element the element to append
     * @return true if rth present element exists, false if the sparse array
     *         contains less than r items
     */
    public boolean getAtRank(int r, StringBuilder element) {
        if (r >= denseValues.size()) {
            return false;
        }
        element.append(denseValues.get(r));
        return true;
    }

    /**
     * Gets the rth element from the sparse array, if it exists, otherwise returns
     * false.
     * 
     * @param r       the index of the element to get
     * @param element the element to append
     * @return true if rth element exists, otherwise false
     */
    public boolean getAtIndex(int r, StringBuilder element) {
        if (!selectSupport.access(r)) {
            return false;
        }
        element.append(denseValues.get(selectSupport.rank1(r)));
        return true;
    }

    /**
     * Gets the index in the sparse array where the rth present element appears.
     * 
     * @param r the index of present elements
     * @return the index in the sparse array where the rth present element appears,
     *         or -1 if there are less than r present elements
     */
    public int getIndexOf(int r) {
        if (r > denseValues.size()) {
            return -1;
        }
        return selectSupport.select1(r) - 1;
    }

    /**
     * Gets the number of present elements up to and including index r, i.e. the
     * inclusive rank.
     * 
     * @param r the index in the sparse array
     * @return the number of present elements up to and including index r
     */
    public int numberOfElementsAt(int r) {
        // return selectSupport.rank1(r) + (selectSupport.access(r) ? 1 : 0);
        return selectSupport.access(r) ? selectSupport.rank1(r) + 1 : selectSupport.rank1(r);
    }

    /**
     * Gets the size of the sparse array.
     * 
     * @return the size of the sparse array
     */
    public int size() {
        return size;
    }

    /**
     * Gets the number of present elements in the sparse array.
     * 
     * @return the number of present elements in the sparse array
     */
    public int numberOfElements() {
        return denseValues.size();
    }

    /**
     * Saves this SparseArray to a file with the specified file name.
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
     * Loads the SparseArray from the specified file.
     * 
     * @param filename the file name of the file containing the RankSupport to load
     * @return the SparseArray
     * @throws IOException            if the file is not able to be loaded
     * @throws ClassNotFoundException if a class is not able to be found during
     *                                deserialization
     */
    public static SparseArray load(String filename) throws IOException, ClassNotFoundException {
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(filename));
        SparseArray sparseArray = (SparseArray) objectInputStream.readObject();
        objectInputStream.close();
        return sparseArray;
    }

    public SelectSupport getSelectSupport() {
        return selectSupport;
    }
}

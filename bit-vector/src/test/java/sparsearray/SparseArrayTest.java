package sparsearray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

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
        SparseArray sparseArray = new SparseArray(10);
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
        SparseArray sparseArray = new SparseArray(10);
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
        assertEquals(selectSupport.getCumulativeRanks()[1], selectSupport2.getCumulativeRanks()[1]);
        assertEquals(selectSupport.getSubchunkCumulativeRanks()[1][1],
                selectSupport2.getSubchunkCumulativeRanks()[1][1]);
        assertEquals(selectSupport.getNumberOfChunks(), selectSupport2.getNumberOfChunks());
        assertEquals(selectSupport.getNumberOfSubchunks(), selectSupport2.getNumberOfSubchunks());

//        StringBuilder stringAtRank0 = new StringBuilder();
//        assertEquals(sparseArray.getAtRank(0, stringAtRank0), )
    }

}

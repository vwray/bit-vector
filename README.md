# bit-vector

A project containing RankSupport, SelectSupport, and SparseArray classes for performing operations with bit-vectors. This program is written in Java, and dependencies are managed via Maven.

## Components

### Rank
The rank package contains the [RankSupport](/bit-vector-impl/src/main/java/rank/RankSupport.java) class containing the constant time rank implementation. To use this class, first create a new BitSet, set the desired bits, and then create a new RankSupport instance:
```
BitSet bitVector = new BitSet(128);
bitVector.set(0, 50); //sets bits 0 through 50
RankSupport rankSupport = new RankSupport(bitVector, 128);
```
Alternatively, load the RankSupport from a file:
```
RankSupport rankSupport = RankSupport.load("filepath/rankSupportFile.bin");
```
As a third option, create the RankSupport from a file containing a bit-vector:
```
BitSet bitVector = RankSupport.loadBitVector("filepath/bitVectorFile.bin");
RankSupport rankSupport = new RankSupport(bitVector, size);
```

Then call `rank1` to get the rank at a particular index:
```
int rank = rankSupport.rank1(75)
```

### Select
The select package contains the [SelectSupport](/bit-vector-impl/src/main/java/select/SelectSupport.java) class containing the log time select implementation.To use this class, first create a new BitSet, set the desired bits, and then create a new RankSupport instance:
```
BitSet bitVector = new BitSet(128);
bitVector.set(0, 50); //sets bits 0 through 50
SelectSupport selectSupport = new SelectSupport(bitVector, 128);
```
Alternatively, load the SelectSupport from a file:
```
SelectSupport selectSupport = SelectSupport.load("filepath/selectSupportFile.bin");
```
As a third option, create the SelectSupport from a file containing a bit-vector:
```
BitSet bitVector = SelectSupport.loadBitVector("filepath/bitVectorFile.bin");
SelectSupport selectSupport = new SelectSupport(bitVector, size);
```

Then call `select1` to get the rank at a particular index:
```
int select = selectSupport.select1(25);
```

### Sparse Array
The sparsearray package contains the [SparseArray](/bit-vector-impl/src/main/java/sparsearray/SparseArray.java) class containing the sparse array implementation. To use this class, create a new SparseArray with the desired size:
```
SparseArray sparseArray = new SparseArray(100);
```
Then add elements at the desired indices and finalize the sparse array:
```
sparseArray.append("foo", 1);
sparseArray.append("bar", 5);
sparseArray.append("baz", 9);
sparseArray.finalize();
```
Alternatively, load the SparseArray from a file:
```
SparseArray sparseArray = SparseArray.load("filepath/sparseArrayFile.bin");
```

Then the methods `getAtRank(int r, StringBuilder element)`, `getAtIndex(int r, StringBuilder element)`, `getIndexOf(int r)`, `numberOfElementsAt(int r)`, `size()`, and `numberOfElements()` can be called. For example,
```
StringBuilder stringAtIndex = new StringBuilder();
boolean hasStringAtIndex = sparseArray.getAtIndex(9, stringAtIndex));
```
With the sparse array created above, hasStringAtIndex would be `true` and stringAtIndex would be "baz".

## Running the code
Recommended steps to run the code:

1. Check out the code from Github.
2. Import bit-vector into Eclipse as a Maven project.
3. Run `mvn clean install` in Eclipse to clean and build the project.
4. Add this dependency to your project's pom file:
```
<dependency>
    <artifactId>bit-vector-impl</artifactId>
    <groupId>com.cmsc701.hw2</groupId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```
5. Include imports for the desired classes (RankSupport, SelectSupport, or SparseArray) into your class files and start using the methods as described above.

## Resources
For generating plots, I consulted [matplotlib4j](https://github.com/sh0nk/matplotlib4j).

I read [this article](https://howtotrainyourjava.com/tag/java-performace/) and [this article](https://www.infoworld.com/article/2077496/java-tip-130--do-you-know-your-data-size-.html) about Java performance and data sizes.

I read [this article](https://www.javamex.com/tutorials/memory/string_memory_usage.shtml) on String memory usage.

I referenced [this article](https://www.developer.com/design/exploring-java-bitset/) about using Java's BitSet.

I referenced an example using Java's DataOutputStream [here](https://www.tutorialspoint.com/java/java_dataoutputstream.htm).

I consulted StackOverflow for several questions, including information about overhead of various data types in Java.

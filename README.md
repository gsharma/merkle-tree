[![Build Status](https://img.shields.io/travis/gsharma/merkle-tree/master.svg)](https://travis-ci.org/gsharma/merkle-tree)
[![Test Coverage](https://img.shields.io/codecov/c/github/gsharma/merkle-tree/master.svg)](https://codecov.io/github/gsharma/merkle-tree?branch=master)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=com.github.merkletree:merkletree&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.github.merkletree:merkletree)
[![Licence](https://img.shields.io/hexpm/l/plug.svg)](https://github.com/gsharma/merkle-tree/blob/master/LICENSE)

# Merkle Tree

Merkle tree is a tree in which every non-leaf node is labeled with the hash of the labels of its children nodes. Hash trees are useful because they allow efficient and secure verification of the contents of large data structures. Hash trees are a generalization of hash lists and hash chains. Demonstrating that a leaf node is a part of the given hash tree requires processing an amount of data proportional to the logarithm of the number of nodes of the tree. The concept of hash trees is named after Ralph Merkle who patented it in 1979.

For the objects of interest, merkle trees are typically constructed at runtime and serialized and shipped over the wire to the other site where they are recomputed and compared with the incoming tree. Quickly identifying the diffs helps speed up and makes more efficient the delta sync between the two sites. They can also be applied to a local site - 2 large files being locally compared.

## Merkle Tree API

1. Report the tree's hashing scheme
```java
public HashingScheme getHashingScheme();
```

2. Report the tree's branching factor
```java
public BranchingFactor getBranchingFactor();
```

3. Report depth of the tree
```java
public int getDepth();
```

4. Get total count of nodes in the tree
```java
public int getNodeCount();
```

5. Print tree level-ordered. Note that it does not currently pretty print the tree
```java
public String printTree();
```

6. Get the root node of this tree
```java
public MerkleTreeNode getRoot();
```

7. Get all nodes at a given level within the tree
```java
public List<MerkleTreeNode> getNodesAtLevel(int level);
```

8. Get all nodes level-ordered starting at root
```java
public List<List<MerkleTreeNode>> getAllNodes();
```

9. Get all hashes at a given level within the tree
```java
public List<byte[]> getHashesAtLevel(int level);
```

10. Given a hash, find the matching node
```java
public MerkleTreeNode findNodeByHash(byte[] hash);
```

11. Compare hashes between two trees at the given level and return all hashes that do not match. This function returns hashes that did not match in the current tree and will not echo the passed in otherHashesToCompareWith hashes.
```java
public List<byte[]> compareHashesAtLevel(int level, List<byte[]> otherHashesToCompareWith);
```

12. Get all children of the tree node that stores the passed in hash
```java
public List<byte[]> getChildrenHashesOfHash(int level, byte[] hash);
```

## Usage Example
Let's do a step-wise walk-through to show the usage of two Merkle Trees and how they are helpful.
```java
// step-1: seed and create tree1 with 4 hashes
final List<byte[]> sourceHashes1 = new ArrayList<byte[]>(4);
sourceHashes1.add(DigestUtils.sha1("first hash"));
sourceHashes1.add(DigestUtils.sha1("second hash"));
sourceHashes1.add(DigestUtils.sha1("third hash"));
sourceHashes1.add(DigestUtils.sha1("fourth hash"));
final MerkleTreeSource source1 = new MerkleTreeByteArrayHashedSource(sourceHashes1);
final MerkleTree tree1 = MerkleTreeBuilder.newBuilder().hashingScheme(HashingScheme.SHA1)
    .branchingFactor(BranchingFactor.TWO).source(source1).build();
tree1.printTree();

// step-2: seed and create tree2 with 4 hashes, only 1 of these 4 is different from tree1's hashes
final List<byte[]> sourceHashes2 = new ArrayList<byte[]>(4);
sourceHashes2.add(DigestUtils.sha1("first hash"));
sourceHashes2.add(DigestUtils.sha1("second hash"));
sourceHashes2.add(DigestUtils.sha1("third hash"));
sourceHashes2.add(DigestUtils.sha1("fifth hash"));
final MerkleTreeSource source2 = new MerkleTreeByteArrayHashedSource(sourceHashes2);
final MerkleTree tree2 = MerkleTreeBuilder.newBuilder().hashingScheme(HashingScheme.SHA1)
    .branchingFactor(BranchingFactor.TWO).source(source2).build();
tree2.printTree();

// step-3: pull tree1's root and verify its hash
final MerkleTreeNode root1 = tree1.getRoot();
assertEquals("2D0FAF0A67399EE4140D39AC6C734297220F90B3", Hasher.hexify(root1.getHash()));

// step-4: tree1 should have been constructed with 7 nodes from the supplied 4 hashes
assertEquals(7, tree1.getNodeCount());

// step-5: tree1 depth should be 3
assertEquals(3, tree1.getDepth());

// step-6: similarly pull tree2's root and verify its hash
final MerkleTreeNode root2 = tree2.getRoot();
assertEquals("5A3FAD8EE064390E2D89CB958215B6DC0D7603E1", Hasher.hexify(root2.getHash()));

// step-7: tree2 should have been constructed with 7 nodes from the supplied 4 hashes
assertEquals(7, tree2.getNodeCount());

// step-8: tree2 depth should be 3
assertEquals(3, tree2.getDepth());

// step-9: perform a level-ordered comparison of tree1 and tree2 to quickly identify mismatches
for (int iter = 0; iter < tree1.getDepth(); iter++) {
  switch (iter) {
    case 0: // root mismatch
      assertEquals(1, tree1.compareHashesAtLevel(iter, tree2.getHashesAtLevel(iter)).size());
      assertEquals("2D0FAF0A67399EE4140D39AC6C734297220F90B3",
          Hasher.hexify(tree1.compareHashesAtLevel(iter, tree2.getHashesAtLevel(iter)).get(0)));
      break;
    case 1: // right child mismatch
      assertEquals(1, tree1.compareHashesAtLevel(iter, tree2.getHashesAtLevel(iter)).size());
      assertEquals("4119A08F514FA9A7C72C378BDA793CD54A691E94",
          Hasher.hexify(tree1.compareHashesAtLevel(iter, tree2.getHashesAtLevel(iter)).get(0)));
      break;
    case 2: // right-most leaf mismatch
      assertEquals(1, tree1.compareHashesAtLevel(iter, tree2.getHashesAtLevel(iter)).size());
      assertEquals("2DE7E90067600D00BC9B24A269390EE0F3BD5BCF",
          Hasher.hexify(tree1.compareHashesAtLevel(iter, tree2.getHashesAtLevel(iter)).get(0)));
      break;
  }
}
```

## Merkle Tree as a library
Add mvn dependency:
```xml
<dependency>
  <groupId>com.github.merkletree</groupId>
  <artifactId>merkletree</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

## Contributing
Requires Java 8 and Maven 3. To setup Eclipse and test locally:
```xml
mvn install -DdownloadSources eclipse:eclipse
```
Create issues and associated pull requests are welcome.

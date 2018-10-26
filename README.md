[![Build Status](https://api.travis-ci.org/gsharma/merkletree.png)](https://travis-ci.org/gsharma/merkletree)

# Merkle Tree

Merkle tree is a tree in which every non-leaf node is labeled with the hash of the labels of its children nodes. Hash trees are useful because they allow efficient and secure verification of the contents of large data structures. Hash trees are a generalization of hash lists and hash chains. Demonstrating that a leaf node is a part of the given hash tree requires processing an amount of data proportional to the logarithm of the number of nodes of the tree. The concept of hash trees is named after Ralph Merkle who patented it in 1979.

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

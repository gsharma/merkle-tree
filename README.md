[![Build Status](https://api.travis-ci.org/gsharma/merkletree.png)](https://travis-ci.org/gsharma/merkletree)

# Merkle Tree

Merkle tree is a tree in which every non-leaf node is labeled with the hash of the labels of its children nodes. Hash trees are useful because they allow efficient and secure verification of the contents of large data structures. Hash trees are a generalization of hash lists and hash chains. Demonstrating that a leaf node is a part of the given hash tree requires processing an amount of data proportional to the logarithm of the number of nodes of the tree. The concept of hash trees is named after Ralph Merkle who patented it in 1979.

## Merkle Tree API

### Report the tree's hashing scheme
public HashingScheme getHashingScheme();

### Report the tree's branching factor
public BranchingFactor getBranchingFactor();

### Report depth of the tree
public int getDepth();

### Get total count of nodes in the tree
public int getNodeCount();

### Print tree level-ordered. Note that it does not currently pretty print the tree.
public String printTree();

### Get the root node of this tree
public MerkleTreeNode getRoot();

### Get all nodes at a given level within the tree
public List<MerkleTreeNode> getNodesAtLevel(int level);

### Get all nodes level-ordered starting at root.
public List<List<MerkleTreeNode>> getAllNodes();

### Get all hashes at a given level within the tree
public List<byte[]> getHashesAtLevel(int level);

### Given a hash, find the matching node.
public MerkleTreeNode findNodeByHash(byte[] hash);

### Compare hashes between two trees at the given level and return all hashes that do not match. This function returns hashes that did not match in the current tree and will not echo the passed in otherHashesToCompareWith hashes.
public List<byte[]> compareHashesAtLevel(int level, List<byte[]> otherHashesToCompareWith);

### Get all children of the tree node that stores the passed in hash.
public List<byte[]> getChildrenHashesOfHash(int level, byte[] hash);


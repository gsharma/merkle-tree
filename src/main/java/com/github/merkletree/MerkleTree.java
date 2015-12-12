package com.github.merkletree;

import java.util.List;

import com.github.merkletree.MerkleTreeImpl.BranchingFactor;
import com.github.merkletree.MerkleTreeImpl.HashingScheme;
import com.github.merkletree.MerkleTreeImpl.MerkleTreeNode;

@SuppressWarnings("rawtypes")
public interface MerkleTree {

  // Report the tree's hashing scheme
  public HashingScheme getHashingScheme();

  // Report the tree's branching factor
  public BranchingFactor getBranchingFactor();

  // Report depth of the tree
  public int getDepth();

  // Get total count of nodes in the tree
  public int getNodeCount();

  // Print tree level-ordered. Note that it does not currently pretty print the tree.
  public String printTree();

  // Get the root node of this tree
  public MerkleTreeNode getRoot();

  // Get all nodes at a given level within the tree
  public List<MerkleTreeNode> getNodesAtLevel(int level);

  // Get all nodes level-ordered starting at root.
  public List<List<MerkleTreeNode>> getAllNodes();

  // Get all hashes at a given level within the tree
  public List<byte[]> getHashesAtLevel(int level);

  // Given a hash, find the matching node.
  public MerkleTreeNode findNodeByHash(byte[] hash);

  // Compare hashes between two trees at the given level and return all hashes that do not match.
  // This function returns hashes that did not match in the current tree and will not echo the
  // passed in otherHashesToCompareWith hashes.
  public List<byte[]> compareHashesAtLevel(int level, List<byte[]> otherHashesToCompareWith);

  // Get all children of the tree node that stores the passed in hash.
  public List<byte[]> getChildrenHashesOfHash(int level, byte[] hash);

}

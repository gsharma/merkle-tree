package com.github.merkletree;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import com.google.common.collect.AbstractIterator;

/**
 * The basic idea is that of a binary hash tree with leaf nodes representing first level hashes and
 * the parent node data being a concatenation or hash of hashes of its child nodes thus the
 * top-level hash represents a union of all hashes in the tree.
 * 
 * A typical usage is that of a large file on disk with the leaf nodes representing hashes (sha1?)
 * of the data blocks or the data blocks themselves. A quick comparison of 2 merkle trees
 * representing 2 different files can help diff the files even if they are arbitrarily large. Also,
 * with a carefully selected block size for hashing, it is possible to effectively compute the file
 * hash in a streaming fashion.
 * 
 * After construction, the merkle tree created is effectively immutable.
 * 
 * There are many ways to diff two trees. One option that this code supports is the iterative
 * option:<br>
 * 1. request hashes of a level starting with root via {@link #getHashesAtLevel(int)}<br>
 * 2. compare hashes returned with hashes of the local tree's corresponding level via
 * {@link #compareHashesAtLevel(int, List)}<br>
 * 3. if there's a diff in any of the nodes, request that nodes children via
 * {@link #getChildrenHashesOfHash(int, byte[])}<br>
 * 4. repeat steps 1 to 3<br>
 * 
 * @author gaurav
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class MerkleTree {
  private static final Logger logger = LoggerFactory.getLogger(MerkleTree.class.getSimpleName());

  // immutables pushed in during construction
  private final HashingScheme hashingScheme;
  private final BranchingFactor branchingFactor;
  private final MerkleTreeSource source;

  // computed fields
  private MerkleTreeNode root;
  private int nodeCount;
  private int treeDepth;
  private final List<List<MerkleTreeNode>> nodesByLevel;

  public MerkleTree(final HashingScheme hashingScheme, final BranchingFactor branchingFactor,
      final MerkleTreeSource source) {
    if (hashingScheme == null) {
      throw new IllegalArgumentException("HashingScheme cannot be null");
    }
    this.hashingScheme = hashingScheme;
    if (branchingFactor == null) {
      this.branchingFactor = BranchingFactor.TWO;
      logger.info("BranchingFactor was null, defaulting to " + this.branchingFactor);
    } else {
      this.branchingFactor = branchingFactor;
    }
    if (source == null) {
      throw new IllegalArgumentException("MerkleTreeSource cannot be null");
    }
    this.source = source;

    nodesByLevel = new ArrayList<List<MerkleTreeNode>>();
    root = curateTree();
  }

  // Get the root node of this tree
  public MerkleTreeNode getRoot() {
    return root;
  }

  // Report the tree's hashing scheme
  public HashingScheme getHashingScheme() {
    return hashingScheme;
  }

  // Report the tree's branching factor
  public BranchingFactor getBranchingFactor() {
    return branchingFactor;
  }

  // Get total count of nodes in the tree
  public int getNodeCount() {
    return nodeCount;
  }

  // Report depth of the tree
  public int getDepth() {
    return treeDepth;
  }

  // Get all hashes at a given level within the tree
  public List<byte[]> getHashesAtLevel(int level) {
    final List<byte[]> hashesAtLevel = new ArrayList<byte[]>();
    if (nodesByLevel != null && !nodesByLevel.isEmpty()) {
      List<MerkleTreeNode> nodesAtLevel = nodesByLevel.get(level);
      if (nodesAtLevel != null && !nodesAtLevel.isEmpty()) {
        for (MerkleTreeNode node : nodesAtLevel) {
          hashesAtLevel.add(node.getHash());
        }
      }
    }
    return hashesAtLevel;
  }

  // Compare hashes between two trees at the given level and return all hashes
  // that do not match. This function returns hashes that did not match in the
  // current tree and will not echo the passed in otherHashesToCompareWith
  // hashes.
  public List<byte[]> compareHashesAtLevel(int level, final List<byte[]> otherHashesToCompareWith) {
    final List<byte[]> diffs = new ArrayList<byte[]>();
    if (otherHashesToCompareWith != null && !otherHashesToCompareWith.isEmpty()) {
      final List<byte[]> currentTreeHashes = getHashesAtLevel(level);
      for (int iter = 0; iter < currentTreeHashes.size(); iter++) {
        // reached tail of otherHashesToCompareWith, append remaining
        // entries in currentTreeHashes
        if (iter == otherHashesToCompareWith.size()) {
          while (iter < currentTreeHashes.size()) {
            diffs.add(currentTreeHashes.get(iter++));
          }
        }
        if (!Arrays.equals(otherHashesToCompareWith.get(iter), currentTreeHashes.get(iter))) {
          diffs.add(currentTreeHashes.get(iter));
        }
      }
    }
    return diffs;
  }

  // Get all children of the tree node that stores the passed in hash.
  public List<byte[]> getChildrenHashesOfHash(int level, final byte[] hash) {
    final List<byte[]> hashesAtLevel = new ArrayList<byte[]>();
    if (nodesByLevel != null && !nodesByLevel.isEmpty()) {
      List<MerkleTreeNode> nodesAtLevel = nodesByLevel.get(level);
      if (nodesAtLevel != null && !nodesAtLevel.isEmpty()) {
        for (MerkleTreeNode node : nodesAtLevel) {
          if (node != null && Arrays.equals(hash, node.getHash())) {
            List<MerkleTreeNode> children = node.getChildren();
            if (children != null && !children.isEmpty()) {
              for (MerkleTreeNode child : children) {
                if (child != null)
                  hashesAtLevel.add(child.getHash());
              }
            }
          }
        }
      }
    }
    return hashesAtLevel;
  }

  // Print tree level-ordered. Note that it does not currently pretty print
  // the tree.
  public String printTree() {
    final StringBuilder builder = new StringBuilder("Printing Merkle Tree...\n");
    final LinkedList<MerkleTreeNode> queue = new LinkedList<MerkleTreeNode>();
    if (root != null) {
      queue.offerLast(root);
    }
    while (!queue.isEmpty()) {
      MerkleTreeNode current = queue.pollFirst();
      builder.append(current.printNode()).append("\n");
      final List<MerkleTreeNode> children = current.getChildren();
      if (children != null && !children.isEmpty()) {
        for (MerkleTreeNode child : children) {
          queue.offerLast(child);
        }
      }
    }
    logger.info(builder.toString());
    return builder.toString();
  }

  /**
   * Represents a node in the MerkleTree data structure. Leaf nodes are immutable after creation.
   * Non-leaf nodes need to compute their hashes via iterating over their children at creation time
   * and are immutable from that point forward.
   * 
   * The contract for creating the MerkleTreeNode is 2-step:<br>
   * 1. invoke one of the constructors<br>
   * 2. invoke the bloomNode() method {@link #bloomNode(HashingScheme, boolean)}<br>
   */
  public interface MerkleTreeNode<T> {
    // This function needs to be idempotent
    void bloomNode(HashingScheme hashingScheme, boolean alreadyHashed);

    // Report the type of this node
    NodeType getType();

    // Report the hash of this node
    byte[] getHash();

    // List all the children of this node
    List<MerkleTreeNode> getChildren();

    // Print this node
    String printNode();
  }

  /**
   * This is a type of MerkleTreeNode which deals in byte array hashes.
   */
  public static final class MerkleTreeByteArrayNode implements MerkleTreeNode<byte[]> {
    private byte[] hash;
    private final byte type;
    private List<MerkleTreeNode> children;

    // Constructor for making leaf nodes. Call bloomNode() right after.
    public MerkleTreeByteArrayNode(final byte[] hash) {
      this.type = NodeType.LEAF.type;
      this.hash = hash;
      this.children = null;
    }

    // Constructor for making non-leaf nodes. Call bloomNode() right after.
    public MerkleTreeByteArrayNode(final List<MerkleTreeNode> children) {
      this.type = NodeType.NON_LEAF.type;
      this.children = children;
    }

    @Override
    public synchronized void bloomNode(final HashingScheme hashingScheme,
        final boolean alreadyHashed) {
      if (type == NodeType.NON_LEAF.type) {
        if (hash != null) {
          logger.info("This MerkleTreeNode has already bloomed");
        } else {
          if (alreadyHashed) {
            this.hash = Hasher.computeHash(hashingScheme, children);
          } else {
            throw new IllegalArgumentException(
                "Non already hashed source data streaming is current unsupported");
          }
        }
      }
    }

    @Override
    public NodeType getType() {
      return NodeType.byteToType(type);
    }

    @Override
    public byte[] getHash() {
      return hash;
    }

    @Override
    public List<MerkleTreeNode> getChildren() {
      return children;
    }

    @Override
    public String printNode() {
      StringBuilder builder = new StringBuilder();
      builder.append("current:{").append(Hasher.hexify(hash)).append("} ");
      builder.append("children:{ ");
      if (children != null) {
        for (MerkleTreeNode child : children) {
          builder.append(Hasher.hexify(child.getHash())).append(' ');
        }
      }
      builder.append('}');
      return builder.toString();
    }
  }

  // TODO
  public static class MerkleTreeSerDe {
    public static void serialize(final MerkleTree tree) {}

    public static void deserialize() {}
  }

  /**
   * start with leaves, then roll them up the layers
   */
  private MerkleTreeNode curateTree() {
    // stream from the source and construct the tree
    final List<MerkleTreeNode> leaves = curateLeaves();

    // build non-leaf nodes
    MerkleTreeNode root = curateNonLeaves(hashingScheme, leaves);

    // flip nodes by level lists since we started creating the tree from its
    // leaves
    Collections.reverse(nodesByLevel);

    logger.info(String.format("Curated Merkle Tree with %d nodes, %d levels deep, %s root hash",
        nodeCount, treeDepth, Hasher.hexify(root.getHash())));

    return root;
  }

  /**
   * Curate leaf nodes of the merkle tree.
   */
  private List<MerkleTreeNode> curateLeaves() {
    List<MerkleTreeNode> leaves = null;
    switch (source.getType()) {
      case BYTE_ARRAY:
        leaves = new ArrayList<MerkleTreeNode>();
        final Iterator<byte[]> leafHashIterator = source.stream();
        while (leafHashIterator.hasNext()) {
          final byte[] hash = leafHashIterator.next();
          MerkleTreeNode<byte[]> leaf = new MerkleTreeByteArrayNode(hash);
          leaf.bloomNode(hashingScheme, source.alreadyHashed());
          leaves.add(leaf);
          // logger.info(leaf.printNode());
          nodeCount++;
        }
        if (nodeCount > 0) {
          treeDepth++;
        }
        break;
      default:
        throw new IllegalArgumentException(source.getType() + " is not yet supported");
    }
    nodesByLevel.add(leaves);
    return Collections.unmodifiableList(leaves);
  }

  /**
   * Curate all non-leaf nodes but recursively. It'll be nice to have an iterative implementation of
   * this behavior.
   */
  private MerkleTreeNode curateNonLeaves(final HashingScheme hashingScheme,
      final List<MerkleTreeNode> leveledNodes) {
    MerkleTreeNode rootNode = null;
    switch (source.getType()) {
      case BYTE_ARRAY:
        final List<MerkleTreeNode> parentLevelNodes = new ArrayList<MerkleTreeNode>();
        final Iterator<MerkleTreeNode> nodeIter = leveledNodes.iterator();
        int nodeIterCounter = 0;
        while (nodeIter.hasNext()) {
          final List<MerkleTreeNode> toHash = new ArrayList<MerkleTreeNode>();
          for (int iter = 0; iter < branchingFactor.getFactor(); iter++) {
            if (nodeIterCounter < leveledNodes.size()) {
              final MerkleTreeNode nextNode = nodeIter.next();
              toHash.add(nextNode);
              nodeIterCounter++;
            }
          }
          final MerkleTreeNode hashCandidate = new MerkleTreeByteArrayNode(toHash);
          hashCandidate.bloomNode(hashingScheme, source.alreadyHashed());
          parentLevelNodes.add(hashCandidate);
          // logger.info(hashCandidate.printNode());
          nodeCount++;
        }
        if (!parentLevelNodes.isEmpty()) {
          nodesByLevel.add(parentLevelNodes);
          treeDepth++;
        }
        // logger.info("parentNodeCount:" + parentLevelNodes.size());
        if (parentLevelNodes.size() > 1) {
          rootNode = curateNonLeaves(hashingScheme, parentLevelNodes);
        }
        if (parentLevelNodes.size() == 1) {
          rootNode = parentLevelNodes.get(0);
        }
        break;
      default:
        throw new IllegalArgumentException(source.getType() + " is not yet supported");
    }
    return rootNode;
  }

  /**
   * Source of data for the Merkle tree.
   */
  public interface MerkleTreeSource<T> {
    // Stream input source data
    Iterator<T> stream();

    // Report the type of data provided by this source
    SourceType getType();

    // Report if the data is already hashed or needs further hashing
    boolean alreadyHashed();
  }

  /**
   * This class acts as a source of leaf hashes.
   */
  public static final class MerkleTreeByteArrayHashedSource implements MerkleTreeSource<byte[]> {
    private final List<byte[]> hashes;

    public MerkleTreeByteArrayHashedSource(final List<byte[]> hashes) {
      this.hashes = Collections.unmodifiableList(hashes);
    }

    @Override
    public Iterator<byte[]> stream() {
      return hashes.iterator();
      // TODO:
      /*
       * return new AbstractIterator<byte[]>() {
       * 
       * @Override protected byte[] computeNext() { Iterator<byte[]> hashesIter = hashes.iterator();
       * if (!hashesIter.hasNext()) { return endOfData(); } while (hashesIter.hasNext()) { byte[]
       * hash = hashesIter.next(); if (hash != null && hash.length > 0) { return hash; } } return
       * endOfData(); } };
       */
    }

    @Override
    public SourceType getType() {
      return SourceType.BYTE_ARRAY;
    }

    @Override
    public boolean alreadyHashed() {
      return true;
    }
  }

  /**
   * This class acts as a source of leaf hashes emanating from splitting a file into many small file
   * chunks.
   */
  public static final class MerkleTreeFileHashedSource implements MerkleTreeSource<byte[]> {
    private final List<byte[]> hashes;

    // Sip from the file fileSplitBytes-sized chunk at a time, don't just
    // pull the entire file in memory, then hash the bytes, discard the
    // bytes, save the hash, rinse, repeat.
    //
    // We can also do it lazily via pushing this logic down into the
    // stream() method but the challenge is that the caller needs to be nice
    // about closing the underlying file by reading it till the very end or
    // by somehow letting us know that it's finished. This is likely to be
    // fraught with danger.
    public MerkleTreeFileHashedSource(final String fileName, int fileSplitBytes,
        final HashingScheme scheme) throws IOException {
      hashes = new ArrayList<byte[]>();
      FileChannel channel = null;
      RandomAccessFile file = null;
      try {
        file = new RandomAccessFile(fileName, "r");
        channel = file.getChannel();
        long fileSize = channel.size();
        if (fileSize <= fileSplitBytes) {
          long originalFileSplitBytes = fileSplitBytes;
          fileSplitBytes /= 4;
          logger.info(String.format(
              "Input file:%s, (size:%d <= fileSplitBytes:%d), switching fileSplitBytes to:%d",
              fileName, fileSize, originalFileSplitBytes, fileSplitBytes));
        }
        // logger.info(String.format("Reading in chunks of %d of %d bytes of file %s for hashing",
        // fileSplitBytes,
        // fileSize, fileName));
        final ByteBuffer buffer = ByteBuffer.allocate(fileSplitBytes);
        while (-1 != (channel.read(buffer))) {
          final byte[] fileChunk = buffer.array();
          if (fileChunk != null) {
            final byte[] hash = Hasher.hash(scheme, fileChunk);
            hashes.add(hash);
          }
          buffer.clear();
        }
        logger.info(String.format(
            "Prepared %d hashes from %d byte chunks from file %s using %s algorithm ",
            hashes.size(), fileSplitBytes, fileName, scheme));
      } finally {
        if (channel != null) {
          channel.close();
        }
        if (file != null) {
          file.close();
        }
      }
    }

    @Override
    public Iterator<byte[]> stream() {
      return hashes.iterator();
    }

    @Override
    public SourceType getType() {
      return SourceType.BYTE_ARRAY;
    }

    @Override
    public boolean alreadyHashed() {
      return true;
    }

    /**
     * This encapsulates a chunk of a file.
     */
    public static final class FileChunk {
      private final long startOffset;
      private final long endOffset;

      public FileChunk(final long startOffset, final long endOffset) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
      }

      public long getStartOffset() {
        return startOffset;
      }

      public long getEndOffset() {
        return endOffset;
      }
    }

  }

  /**
   * Simple stand-alone utility class that provides support for various hash functions.
   */
  public static final class Hasher {
    private static final char[] hexChars = "0123456789ABCDEF".toCharArray();

    // Hash bytes per the specified HashingScheme.
    public static byte[] hash(final HashingScheme scheme, final byte[] bytes) {
      byte[] sha1 = null;
      switch (scheme) {
        case SHA1:
          sha1 = DigestUtils.sha1(bytes);
          // logger.info(hexify(sha1));
          break;
        default:
          throw new IllegalArgumentException(scheme + " is not yet supported");
      }
      return sha1;
    }

    // Hash stream per the specified HashingScheme.
    public static byte[] hash(final HashingScheme scheme, final InputStream stream)
        throws IOException {
      byte[] sha1 = null;
      switch (scheme) {
        case SHA1:
          sha1 = DigestUtils.sha1(stream);
          break;
        default:
          throw new IllegalArgumentException(scheme + " is not yet supported");
      }
      return sha1;
    }

    // Convert bytes to hex
    public static String hexify(final byte[] bytes) {
      char[] hex = new char[bytes.length * 2];
      for (int iter = 0; iter < bytes.length; iter++) {
        int temp = bytes[iter] & 0xFF;
        hex[iter * 2] = hexChars[temp >>> 4];
        hex[iter * 2 + 1] = hexChars[temp & 0x0F];
      }
      return new String(hex);
    }

    // Hashes together node hashes by concatenating them and re-hashing the
    // concatenated hashes
    public static byte[] computeHash(final HashingScheme hashingScheme,
        final List<MerkleTreeNode> children) {
      byte[] superHash = null;
      if (children.size() == 1) {
        superHash = children.get(0).getHash();
      } else {
        switch (hashingScheme) {
          case SHA1:
            // sha1 should be 20 bytes long
            byte[] allNodeHashes = new byte[children.size() * 20];
            int byteIter = 0;
            for (final MerkleTreeNode node : children) {
              byte[] nodeHash = node.getHash();
              if (nodeHash == null || nodeHash.length != 20) {
                throw new IllegalArgumentException(
                    "MerkleTreeNode cannot have a null or empty or invalid length hash");
              }
              for (final byte hashByte : nodeHash) {
                allNodeHashes[byteIter++] = hashByte;
              }
            }
            superHash = Hasher.hash(hashingScheme, allNodeHashes);
            break;
          default:
            throw new IllegalArgumentException(hashingScheme + " is not yet supported");
        }
      }
      return superHash;
    }
  }

  // The hashing scheme to use
  public static enum HashingScheme {
    SHA1((byte) 0);

    private byte scheme;

    public byte getScheme() {
      return scheme;
    }

    public static HashingScheme byteToScheme(final byte scheme) {
      for (HashingScheme hashingScheme : values()) {
        if (hashingScheme.scheme == scheme) {
          return hashingScheme;
        }
      }
      return null;
    }

    private HashingScheme(final byte scheme) {
      this.scheme = scheme;
    }
  }

  // The type of tree node
  public static enum NodeType {
    LEAF((byte) 0), NON_LEAF((byte) 1);

    private byte type;

    public byte getType() {
      return type;
    }

    public static NodeType byteToType(final byte type) {
      for (NodeType nodeType : values()) {
        if (nodeType.type == type) {
          return nodeType;
        }
      }
      return null;
    }

    private NodeType(final byte type) {
      this.type = type;
    }
  }

  // The type of MerkleTreeSource
  public static enum SourceType {
    BYTE_ARRAY((byte) 0);

    private byte type;

    public byte getType() {
      return type;
    }

    public static SourceType byteToType(final byte type) {
      for (SourceType sourceType : values()) {
        if (sourceType.type == type) {
          return sourceType;
        }
      }
      return null;
    }

    private SourceType(final byte type) {
      this.type = type;
    }
  }

  // The maximum number of children per node
  public static enum BranchingFactor {
    TWO(2);

    private int factor;

    public int getFactor() {
      return factor;
    }

    private BranchingFactor(final int factor) {
      this.factor = factor;
    }
  }

}

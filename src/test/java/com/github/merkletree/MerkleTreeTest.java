package com.github.merkletree;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import com.github.merkletree.MerkleTree.MerkleTreeBuilder;
import com.github.merkletree.MerkleTreeImpl.BranchingFactor;
import com.github.merkletree.MerkleTreeImpl.Hasher;
import com.github.merkletree.MerkleTreeImpl.HashingScheme;
import com.github.merkletree.MerkleTreeImpl.MerkleTreeByteArrayHashedSource;
import com.github.merkletree.MerkleTreeImpl.MerkleTreeFileHashedSource;
import com.github.merkletree.MerkleTreeImpl.MerkleTreeNode;
import com.github.merkletree.MerkleTreeImpl.MerkleTreeSource;

@SuppressWarnings("rawtypes")
public class MerkleTreeTest {
  {
    System.setProperty("log4j.configurationFile", "log4j.properties");
    System.setProperty("Log4jContextSelector",
        "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");
  }

  @Test
  public void testSha1TwoChildMerkleTree() {
    final List<byte[]> sourceHashes = new ArrayList<byte[]>(4);
    sourceHashes.add(DigestUtils.sha1("first hash"));
    sourceHashes.add(DigestUtils.sha1("second hash"));
    sourceHashes.add(DigestUtils.sha1("third hash"));
    sourceHashes.add(DigestUtils.sha1("fourth hash"));
    MerkleTreeSource source = new MerkleTreeByteArrayHashedSource(sourceHashes);
    MerkleTree tree = MerkleTreeBuilder.newBuilder().hashingScheme(HashingScheme.SHA1)
        .branchingFactor(BranchingFactor.TWO).source(source).build();
    tree.printTree();

    MerkleTreeNode root = tree.getRoot();
    assertEquals("2D0FAF0A67399EE4140D39AC6C734297220F90B3", Hasher.hexify(root.getHash()));
    assertEquals(7, tree.getNodeCount());
    assertEquals(3, tree.getDepth());

    for (int iter = 0; iter < tree.getDepth(); iter++) {
      List<byte[]> hashes = tree.getHashesAtLevel(iter);
      switch (iter) {
        case 0:
          assertEquals(1, hashes.size());
          assertEquals("2D0FAF0A67399EE4140D39AC6C734297220F90B3", Hasher.hexify(hashes.get(0)));

          List<byte[]> childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(0));
          assertEquals("4D3747A82973DEA4C7A3821597973AC11D1705BA",
              Hasher.hexify(childrenHashes.get(0)));
          assertEquals("4119A08F514FA9A7C72C378BDA793CD54A691E94",
              Hasher.hexify(childrenHashes.get(1)));
          break;
        case 1:
          assertEquals(2, hashes.size());
          assertEquals("4D3747A82973DEA4C7A3821597973AC11D1705BA", Hasher.hexify(hashes.get(0)));
          assertEquals("4119A08F514FA9A7C72C378BDA793CD54A691E94", Hasher.hexify(hashes.get(1)));

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(0));
          assertEquals("DB5013029EC94CEE543B31A04F9DE19FE57B728F",
              Hasher.hexify(childrenHashes.get(0)));
          assertEquals("F9987AF47E9CE5B5FC4B912EAFE459988E753966",
              Hasher.hexify(childrenHashes.get(1)));

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(1));
          assertEquals("F4796EAAE7A7143B76B31203901D410479641D13",
              Hasher.hexify(childrenHashes.get(0)));
          assertEquals("2DE7E90067600D00BC9B24A269390EE0F3BD5BCF",
              Hasher.hexify(childrenHashes.get(1)));
          break;
        case 2:
          assertEquals(4, hashes.size());
          assertEquals("DB5013029EC94CEE543B31A04F9DE19FE57B728F", Hasher.hexify(hashes.get(0)));
          assertEquals("F9987AF47E9CE5B5FC4B912EAFE459988E753966", Hasher.hexify(hashes.get(1)));
          assertEquals("F4796EAAE7A7143B76B31203901D410479641D13", Hasher.hexify(hashes.get(2)));
          assertEquals("2DE7E90067600D00BC9B24A269390EE0F3BD5BCF", Hasher.hexify(hashes.get(3)));

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(0));
          assertTrue(childrenHashes.isEmpty());

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(1));
          assertTrue(childrenHashes.isEmpty());

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(2));
          assertTrue(childrenHashes.isEmpty());

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(3));
          assertTrue(childrenHashes.isEmpty());
          break;
      }
    }
  }

  @Test
  public void testDiffSha1TwoChildIdenticalMerkleTrees() {
    final List<byte[]> sourceHashes = new ArrayList<byte[]>(4);
    sourceHashes.add(DigestUtils.sha1("first hash"));
    sourceHashes.add(DigestUtils.sha1("second hash"));
    sourceHashes.add(DigestUtils.sha1("third hash"));
    sourceHashes.add(DigestUtils.sha1("fourth hash"));
    MerkleTreeSource source = new MerkleTreeByteArrayHashedSource(sourceHashes);

    MerkleTree tree1 = MerkleTreeBuilder.newBuilder().hashingScheme(HashingScheme.SHA1)
        .branchingFactor(BranchingFactor.TWO).source(source).build();
    tree1.printTree();

    MerkleTree tree2 = MerkleTreeBuilder.newBuilder().hashingScheme(HashingScheme.SHA1)
        .branchingFactor(BranchingFactor.TWO).source(source).build();
    tree2.printTree();

    MerkleTreeNode root1 = tree1.getRoot();
    assertEquals("2D0FAF0A67399EE4140D39AC6C734297220F90B3", Hasher.hexify(root1.getHash()));
    assertEquals(7, tree1.getNodeCount());
    assertEquals(3, tree1.getDepth());

    MerkleTreeNode root2 = tree2.getRoot();
    assertEquals("2D0FAF0A67399EE4140D39AC6C734297220F90B3", Hasher.hexify(root2.getHash()));
    assertEquals(7, tree2.getNodeCount());
    assertEquals(3, tree2.getDepth());

    // compare tree1 to tree2
    for (int iter = 0; iter < tree1.getDepth(); iter++) {
      assertTrue(tree1.compareHashesAtLevel(iter, tree2.getHashesAtLevel(iter)).isEmpty());
    }

    // compare tree2 to tree1
    for (int iter = 0; iter < tree2.getDepth(); iter++) {
      assertTrue(tree2.compareHashesAtLevel(iter, tree1.getHashesAtLevel(iter)).isEmpty());
    }
  }

  @Test
  public void testDiffSha1TwoChildNonIdenticalMerkleTrees() {
    final List<byte[]> sourceHashes1 = new ArrayList<byte[]>(4);
    sourceHashes1.add(DigestUtils.sha1("first hash"));
    sourceHashes1.add(DigestUtils.sha1("second hash"));
    sourceHashes1.add(DigestUtils.sha1("third hash"));
    sourceHashes1.add(DigestUtils.sha1("fourth hash"));
    MerkleTreeSource source1 = new MerkleTreeByteArrayHashedSource(sourceHashes1);

    MerkleTree tree1 = MerkleTreeBuilder.newBuilder().hashingScheme(HashingScheme.SHA1)
        .branchingFactor(BranchingFactor.TWO).source(source1).build();
    tree1.printTree();

    final List<byte[]> sourceHashes2 = new ArrayList<byte[]>(4);
    sourceHashes2.add(DigestUtils.sha1("first hash"));
    sourceHashes2.add(DigestUtils.sha1("second hash"));
    sourceHashes2.add(DigestUtils.sha1("third hash"));
    sourceHashes2.add(DigestUtils.sha1("fifth hash"));
    MerkleTreeSource source2 = new MerkleTreeByteArrayHashedSource(sourceHashes2);

    MerkleTree tree2 = MerkleTreeBuilder.newBuilder().hashingScheme(HashingScheme.SHA1)
        .branchingFactor(BranchingFactor.TWO).source(source2).build();
    tree2.printTree();

    MerkleTreeNode root1 = tree1.getRoot();
    assertEquals("2D0FAF0A67399EE4140D39AC6C734297220F90B3", Hasher.hexify(root1.getHash()));
    assertEquals(7, tree1.getNodeCount());
    assertEquals(3, tree1.getDepth());

    MerkleTreeNode root2 = tree2.getRoot();
    assertEquals("5A3FAD8EE064390E2D89CB958215B6DC0D7603E1", Hasher.hexify(root2.getHash()));
    assertEquals(7, tree2.getNodeCount());
    assertEquals(3, tree2.getDepth());

    // compare tree1 to tree2
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

    // compare tree2 to tree1
    for (int iter = 0; iter < tree2.getDepth(); iter++) {
      switch (iter) {
        case 0: // root mismatch
          assertEquals(1, tree2.compareHashesAtLevel(iter, tree1.getHashesAtLevel(iter)).size());
          assertEquals("5A3FAD8EE064390E2D89CB958215B6DC0D7603E1",
              Hasher.hexify(tree2.compareHashesAtLevel(iter, tree1.getHashesAtLevel(iter)).get(0)));
          break;
        case 1: // right child mismatch
          assertEquals(1, tree2.compareHashesAtLevel(iter, tree1.getHashesAtLevel(iter)).size());
          assertEquals("451D2FBBDA3EB662217E99C30C0BF0C423C870DA",
              Hasher.hexify(tree2.compareHashesAtLevel(iter, tree1.getHashesAtLevel(iter)).get(0)));
          break;
        case 2: // right-most leaf mismatch
          assertEquals(1, tree2.compareHashesAtLevel(iter, tree1.getHashesAtLevel(iter)).size());
          assertEquals("E44F10D868F68E8B92D4C6DE682F72B116CFB45D",
              Hasher.hexify(tree2.compareHashesAtLevel(iter, tree1.getHashesAtLevel(iter)).get(0)));
          break;
      }
    }
  }

  @Test
  public void testSha1TwoChildFileBackedMerkleTree() throws Exception {
    final String fileName = "src/test/resources/HipChat-3.2.1.zip";
    final int fileSplitBytes = 2 * 1024 * 1024; // 2MB
    MerkleTreeSource source =
        new MerkleTreeFileHashedSource(fileName, fileSplitBytes, HashingScheme.SHA1);
    MerkleTree tree = MerkleTreeBuilder.newBuilder().hashingScheme(HashingScheme.SHA1)
        .branchingFactor(BranchingFactor.TWO).source(source).build();
    tree.printTree();

    MerkleTreeNode root = tree.getRoot();
    assertEquals("FE001D14D2F3A0081177E9C2F4FD8A6510CC1C37", Hasher.hexify(root.getHash()));
    assertEquals(11, tree.getNodeCount());
    assertEquals(4, tree.getDepth());

    for (int iter = 0; iter < tree.getDepth(); iter++) {
      List<byte[]> hashes = tree.getHashesAtLevel(iter);
      switch (iter) {
        case 0:
          assertEquals(1, hashes.size());
          assertEquals("FE001D14D2F3A0081177E9C2F4FD8A6510CC1C37", Hasher.hexify(hashes.get(0)));

          List<byte[]> childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(0));
          assertEquals("BCB448E0A9C50EA19803F30A4B4E635314068B97",
              Hasher.hexify(childrenHashes.get(0)));
          assertEquals("AB56AF573A74D2575318E6806543FCEC1DF80A43",
              Hasher.hexify(childrenHashes.get(1)));
          break;
        case 1:
          assertEquals(2, hashes.size());
          assertEquals("BCB448E0A9C50EA19803F30A4B4E635314068B97", Hasher.hexify(hashes.get(0)));
          assertEquals("AB56AF573A74D2575318E6806543FCEC1DF80A43", Hasher.hexify(hashes.get(1)));

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(0));
          assertEquals("8DB4FC983A7CC2D3177C0BC3C3AD3BB454AAB057",
              Hasher.hexify(childrenHashes.get(0)));
          assertEquals("A8E6A6C5E35158CC1A73FA82B9A96A821208BBDF",
              Hasher.hexify(childrenHashes.get(1)));

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(1));
          assertEquals("AB56AF573A74D2575318E6806543FCEC1DF80A43",
              Hasher.hexify(childrenHashes.get(0)));
          break;
        case 2:
          assertEquals(3, hashes.size());
          assertEquals("8DB4FC983A7CC2D3177C0BC3C3AD3BB454AAB057", Hasher.hexify(hashes.get(0)));
          assertEquals("A8E6A6C5E35158CC1A73FA82B9A96A821208BBDF", Hasher.hexify(hashes.get(1)));
          assertEquals("AB56AF573A74D2575318E6806543FCEC1DF80A43", Hasher.hexify(hashes.get(2)));
          break;
        case 3:
          assertEquals(5, hashes.size());
          assertEquals("D5A765D5418C2BC12FC10F9E18EBAD98AB9AB75D", Hasher.hexify(hashes.get(0)));
          assertEquals("E78DD78994A9FA2D864DB731297D48F1DA5E5DF0", Hasher.hexify(hashes.get(1)));
          assertEquals("FDD823A0D2D11856DFDD31AD838657F10D344D92", Hasher.hexify(hashes.get(2)));
          assertEquals("3EA415B4E46111C8E34197D23455EFF5BDD7F09F", Hasher.hexify(hashes.get(3)));
          assertEquals("AB56AF573A74D2575318E6806543FCEC1DF80A43", Hasher.hexify(hashes.get(4)));

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(0));
          assertTrue(childrenHashes.isEmpty());

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(1));
          assertTrue(childrenHashes.isEmpty());

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(2));
          assertTrue(childrenHashes.isEmpty());

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(3));
          assertTrue(childrenHashes.isEmpty());

          childrenHashes = tree.getChildrenHashesOfHash(iter, hashes.get(4));
          assertTrue(childrenHashes.isEmpty());
          break;
      }
    }
  }

  @Test
  public void testNodeLookup() {
    final List<byte[]> sourceHashes = new ArrayList<byte[]>(4);
    sourceHashes.add(DigestUtils.sha1("first hash"));
    sourceHashes.add(DigestUtils.sha1("second hash"));
    sourceHashes.add(DigestUtils.sha1("third hash"));
    sourceHashes.add(DigestUtils.sha1("fourth hash"));
    MerkleTreeSource source = new MerkleTreeByteArrayHashedSource(sourceHashes);

    MerkleTree tree = MerkleTreeBuilder.newBuilder().hashingScheme(HashingScheme.SHA1)
        .branchingFactor(BranchingFactor.TWO).source(source).build();
    tree.printTree();

    MerkleTreeNode root = tree.getRoot();
    assertEquals("2D0FAF0A67399EE4140D39AC6C734297220F90B3", Hasher.hexify(root.getHash()));
    assertEquals(7, tree.getNodeCount());
    assertEquals(3, tree.getDepth());

    for (byte[] sourceHash : sourceHashes) {
      MerkleTreeNode node = tree.findNodeByHash(sourceHash);
      assertNotNull(node);
      assertTrue(Arrays.equals(sourceHash, node.getHash()));
    }

    int allNodes = 0;
    for (List<MerkleTreeNode> leveledNodes : tree.getAllNodes()) {
      for (MerkleTreeNode node : leveledNodes) {
        allNodes++;
      }
    }
    assertEquals(tree.getNodeCount(), allNodes);
  }

}

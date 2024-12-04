package com.hong.study.algo.tree;

import lombok.Data;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 树的遍历
 *
 * @author hong
 */
public class TreeTraversal {

    @Data
    public static class TreeNode {
        private String root;
        private TreeNode left;
        private TreeNode right;
    }


    /**
     * 前序遍历
     */
    public static void preOrder(TreeNode root) {

        if (root == null) {
            return;
        }

        System.out.print(root.getRoot() + " ");

        if (root.getLeft() != null) {
            preOrder(root.getLeft());
        }

        if (root.getRight() != null) {
            preOrder(root.getRight());
        }
    }

    /**
     * 中序遍历
     */
    public static void inOrder(TreeNode root) {
        if (root == null) {
            return;
        }

        if (root.getLeft() != null) {
            inOrder(root.getLeft());
        }

        System.out.print(root.getRoot() + " ");

        if (root.getRight() != null) {
            inOrder(root.getRight());
        }
    }

    /**
     * 后序遍历
     */
    public static void postOrder(TreeNode root) {
        if (root == null) {
            return;
        }

        if (root.getLeft() != null) {
            postOrder(root.getLeft());
        }

        if (root.getRight() != null) {
            postOrder(root.getRight());
        }

        System.out.print(root.getRoot() + " ");
    }

    public static void breadthFirstOrder(TreeNode root) {
        if (root == null) {
            return;
        }

        Queue<TreeNode> queue = new LinkedList<>();
        queue.offer(root);

        while (!queue.isEmpty()) {
            TreeNode current = queue.poll();
            System.out.print(current.getRoot() + " ");

            if (current.getLeft() != null) {
                queue.offer(current.getLeft());
            }
            if (current.getRight() != null) {
                queue.offer(current.getRight());
            }
        }

    }

    public static TreeNode initTree() {
        // 创建节点
        TreeNode nodeA = new TreeNode();
        nodeA.setRoot("A");

        TreeNode nodeB = new TreeNode();
        nodeB.setRoot("B");

        TreeNode nodeC = new TreeNode();
        nodeC.setRoot("C");

        TreeNode nodeD = new TreeNode();
        nodeD.setRoot("D");

        TreeNode nodeE = new TreeNode();
        nodeE.setRoot("E");

        TreeNode nodeF = new TreeNode();
        nodeF.setRoot("F");

        // 构建树结构
        nodeA.setLeft(nodeB);
        nodeA.setRight(nodeC);

        nodeB.setLeft(nodeD);
        nodeB.setRight(nodeE);

        nodeC.setRight(nodeF);

        return nodeA;
    }

    public static void main(String[] args) {
        TreeNode treeNode = initTree();

        System.out.println("前序遍历：");
        preOrder(treeNode);
        System.out.println();

        System.out.println("中序遍历：");
        inOrder(treeNode);
        System.out.println();

        System.out.println("后续遍历：");
        postOrder(treeNode);
        System.out.println();

        System.out.println("广度优先遍历：");
        breadthFirstOrder(treeNode);
        System.out.println();

    }

}

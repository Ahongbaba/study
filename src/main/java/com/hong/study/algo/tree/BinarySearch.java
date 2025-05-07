package com.hong.study.algo.tree;

/**
 * @author hong
 */
public class BinarySearch {

    public static int search(int[] arr, int target) {

        int left = 0;
        int right = arr.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (arr[mid] == target) {
                return mid;
            } else if (arr[mid] < target) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        return -1;
    }

    public static int mountainSearch(int[] arr) {

        int left = 0;
        int right = arr.length - 1;

        while (left <= right) {

            // 中点
            int mid = left + (right - left) / 2;

            // 中点是否为最大
            if ((mid == 0 || arr[mid] >= arr[mid - 1]) && (mid == arr.length - 1 || arr[mid] >= arr[mid + 1])) {
                return arr[mid];
            } else if (arr[mid] < arr[mid + 1]) {
                // 右边更大
                left = mid + 1;
            } else {
                // 左边更大
                right = mid - 1;
            }

        }

        return -1;
    }

    public static int mountainSearch1(int[] arr) {
        int low = 0;
        int high = arr.length - 1;

        while (low <= high) {
            int mid = low + (high - low) / 2;

            // 检查中间元素是否是峰值
            if ((mid == 0 || arr[mid - 1] <= arr[mid]) && (mid == arr.length - 1 || arr[mid] >= arr[mid + 1])) {
                return arr[mid];
            }
            // 如果右侧元素比中间元素大，说明峰值在右侧
            else if (arr[mid] < arr[mid + 1]) {
                low = mid + 1;
            }
            // 如果左侧元素比中间元素大，说明峰值在左侧
            else {
                high = mid - 1;
            }
        }

        return -1; // 如果没有找到最大值（理论上不会执行到这里）
    }

    public static void main(String[] args) {
        int[] arr = {1, 3, 5, 6, 7, 6, 4, 2}; // 示例数组，山脉数组
        System.out.println("Max value: " + mountainSearch(arr));

        long i  = 8456092673680312L;
        long res = i % 128;
        System.out.println(res);
    }

}

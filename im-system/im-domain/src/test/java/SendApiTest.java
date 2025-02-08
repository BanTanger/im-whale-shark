import lombok.SneakyThrows;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @author BanTanger 半糖
 * @Date 2023/4/26 17:20
 */
public class SendApiTest {


    @Test
    public void send_test() {
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 100000; i++) {

            }
        });
    }

    /**
     * 给你一个由 '1'（陆地）和 '0'（水）组成的的二维网格，请你计算网格中岛屿的数量。
     * <p>
     * 岛屿总是被水包围，并且每座岛屿只能由水平方向和/或竖直方向上相邻的陆地连接形成。
     * <p>
     * 此外，你可以假设该网格的四条边均被水包围
     * <p>
     * 1 0 1 0 0 0
     * 0 1 0 1 0 1
     * 0 1 0 1 0 0
     * <p>
     * ["1","1","0","0","0"],
     * ["1","1","0","0","0"],
     * ["0","0","1","0","0"],
     * ["0","0","0","1","1"]
     * <p>
     * 0 -> 1
     * positions = [[0,2], [2,1], [1,3], [3,0]]
     * 输出: [1,1,2,3]
     */

    int n, m;
    boolean[][] visited;

    @Test
    public void test() {
        int[][] nums = {{1, 0, 1, 0, 0, 0},
                {0, 1, 0, 1, 0, 1},
                {0, 1, 0, 1, 0, 0}};
        n = nums.length;
        m = nums[0].length;
        visited = new boolean[n][m];
        int ans = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (nums[i][j] == 1) {
                    dfs(nums, i, j);
                    ans++;
                }
            }
        }
        System.out.println(ans);
    }

    private void dfs(int[][] nums, int i, int j) {
        if (i < 0 || i >= n || j < 0 || j >= m || visited[i][j] || nums[i][j] != 1) return;
        visited[i][j] = true;
        nums[i][j] = 0; // 水蔓
        dfs(nums, i + 1, j);
        dfs(nums, i, j + 1);
        dfs(nums, i - 1, j);
        dfs(nums, i, j - 1);
    }

    /**
     * 找出数字连续的最长序列
     * 题目描述
     * 给定一个未排序的整数数组 nums ，找出数字连续的最长序列（不要求序列元素在原数组中连续）的长度。
     * 输入：nums = [100,4,101,200,1,3,2]
     * 输出：4
     * 解释：最长数字连续序列是 [1, 2, 3, 4]。它的长度为 4。
     * 54 5 55 2 56
     */
    @Test
    public void test2() {
        int[] nums = {100, 4, 200, 1, 3, 2};
        Map<Integer, Integer> map = new HashMap<>();
        int len = 1;
        int ans = 0;
        for (int num : nums) {
            map.put(num, map.getOrDefault(num, 0) + 1);
        }
        for (int i = 0; i < nums.length; i++) {
            int tmp = nums[i] + 1;
            while (map.containsKey(tmp)) {
                tmp += 1;
                len++;
            }
            ans = Math.max(ans, len);
            len = 1;
        }
        System.out.println(ans);
    }

    /**
     * O v v O O
     * 0 1 2 3 4
     *
     * 0 1 3、0 1 4
     * 0 2 3、0 2 4
     *
     * O O v O O --> 2
     *
     * O(n)
     */

//    public static void main(String[] args) throws Exception {
//        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//        StreamTokenizer in = new StreamTokenizer(br);
//        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
//        while(in.nextToken() != StreamTokenizer.TT_EOF) {
//            int n = (int) in.nval;
//            in.nextToken();
//            for(int i = 0; i < n; i ++) {
//
//            }
//            out.println();
//        }
//        out.flush();
//        out.close();
//        br.close();
//    }

    @Test
    public void test4() {
        String s = "leetcode", t = "leeto";
        System.out.println(kmp(s.toCharArray(), t.toCharArray()));
    }
    private static int kmp(char[] s, char[] t) {
        int n = s.length, m = t.length, x = 0, y = 0;
        // Om 获得 t 的 next 数组
        int[] next = getNext(t, m);
        while(x < n && y < m) {
            if(s[x] == t[y]) {
                x ++;
                y ++;
            } else if(y == 0) {
                x ++;
            } else {
                y = next[y];
            }
        }
        return y == m ? x - y : -1;
    }
    private static int[] getNext(char[] t, int m) {
        if(m == 1) return new int[] {-1};
        int[] next = new int[m];
        next[0] = -1;
        next[1] = 0;
        int i = 2, cn = 0;
        while(i < m) {
            if(t[i - 1] == t[cn]) {
                next[i ++] = ++ cn;
            } else if(cn > 0) {
                cn = next[cn]; // 往前跳跃
            } else {
                next[i ++] = 0; // 重置
            }
        }
        return next;
    }

    @Test
    public void test6() {
        init(3);
        put(1, 1);
        put(2, 2);
        put(3, 3);
        System.out.println(get(1));
        put(1, 4);
        System.out.println(get(1));
        // 触发淘汰
        put(4, 4);
        System.out.println(get(1));
        System.out.println(get(4));
    }

    // LRU , hashmap k v
    class Node {
        int key, val;
        Node prev, next;
        public Node(int k, int v) {
            key = k;
            val = v;
        }
    }

    final Node dummy = new Node(-1, -1);
    final Map<Integer, Node> map = new HashMap<>();
    int capital;

    public void init(int capital) {
        this.capital = capital;
        dummy.prev = dummy;
        dummy.next = dummy;
    }

    // 头部最新，尾部是最久的

    public void put(int k, int v) {
        Node node = getNode(k);
        if(node != null) {
            node.val = v;
            return ;
        }
        // node == null 新节点
        node = new Node(k, v);
        map.put(k, node);
        pushFront(node);
        // 如果当前 push 的 node 超过 capital
        if(map.size() > capital) {
            Node backup = dummy.prev;
            remove(backup);
            map.remove(backup.key);
        }
    }

    public int get(int k) {
        Node node = getNode(k);
        return node != null ? node.val : -1;
    }

    private Node getNode(int k) {
        if(!map.containsKey(k)) return null;
        Node node = map.get(k);
        // push
        pushFront(node);
        return node;
    }

    private void remove(Node node) {
        node.next.prev = node.prev;
        node.prev.next = node.next;
    }

    Stack<Integer> in = new Stack<>();

    private void pushFront(Node node) {
        in.isEmpty();
        node.prev = dummy;
        node.next = dummy.next;
        node.prev.next = node;
        node.next.prev = node;
    }

    public Object a = 0;

    @Test
    public void test8() {
        synchronized (a) {
            a = 1 / 0;
            System.out.println(1111);
        }
    }

    // 快速选择
    // n - terget = index
    // 二分 p idx < index >
    // p idx == index
    //   nums[p_idx]

    @Test
    public void test7() {
        // 堆排序
        int[] nums = {3, 1, 2, 5, 6};
        // 从底到顶建堆，On
        for(int i = nums.length - 1; i >= 0; i --) {
            heapify(nums, i, nums.length);
        }
        int size = nums.length;
        while(size > 1) {
            swap(nums, 0, -- size);
            heapify(nums, 0, size);
        }
        for(int num : nums) {
            System.out.print(num + " ");
        }
    }
    private void heapify(int[] nums, int root, int size) {
        int child = 2 * root + 1;
        while(child < size) {
            // 选大孩子
            if(child + 1 < size && nums[child + 1] > nums[child]) child ++;
            if(nums[root] > nums[child]) return ; // 不替换
            else {
                swap(nums, root, child);
                root = child;
                child = 2 * root + 1;
            }
        }
    }
    private void swap(int[] nums, int a, int b) {
        int tmp = nums[a];
        nums[a] = nums[b];
        nums[b] = tmp;
    }

    private static class Singleton {
        private volatile Singleton singleton;
        private Singleton() {}
        public synchronized Singleton getSingleton() {
            if(singleton == null) {
                singleton = new Singleton();
            }
            return singleton;
        }
        public Singleton getSingleton2() {
            if(singleton == null) {
                synchronized (Singleton.class) {
                    if(singleton == null) {
                        singleton = new Singleton();
                    }
                }
            }
            return singleton;
        }
        private static class SingletonHolder {
            private static final Singleton INSTANCE = new Singleton();
        }
        public Singleton getSinglenton3() {
            return SingletonHolder.INSTANCE;
        }
    }

    public static int num = 0;
//    public static Object lock = new Object();
    public static Lock lock = new ReentrantLock();
    public static Condition c1 = lock.newCondition();
    public static Condition c2 = lock.newCondition();
    public static Condition c3 = lock.newCondition();
    public static final int COUNT = 3;

    @SneakyThrows
    private static void printABC(int targetNum) {
        while(num < 100) {
            synchronized (lock) {
                while(num % COUNT != targetNum) {
                    lock.wait();
                }
//                num ++;
                System.out.println(Thread.currentThread().getName() + " - " + num ++);
                lock.notifyAll();
            }
        }
    }
    private static void printABC(int targetNum, Condition currentThread, Condition nextThread) {
        while(num < 100) {
            lock.lock();
            try {
                while(num % COUNT != targetNum) {
                    currentThread.await();
                }
                num ++;
                if (num <= 100)
                    System.out.println(Thread.currentThread().getName() + " - " + num);
                nextThread.signal();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }
    }

//    public static void main(String[] args) {
////        new Thread(() -> printABC(0), "A").start();
////        new Thread(() -> printABC(1), "B").start();
////        new Thread(() -> printABC(2), "C").start();
//        new Thread(() -> printABC(0, c1, c2), "A").start();
//        new Thread(() -> printABC(1, c2, c3), "B").start();
//        new Thread(() -> printABC(2, c3, c1), "C").start();
//    }

    static int MAXN = 100100;
//    public static void main(String[] args) throws Exception {
//        int n, m;
//        int[] ans = new int[MAXN];
//        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//        StreamTokenizer in = new StreamTokenizer(br);
//        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
//        while(in.nextToken() != StreamTokenizer.TT_EOF) {
//            n = (int) in.nval;
//            in.nextToken();
//            m = (int) in.nval;
//            ans = new int[m];
//            for(int i = 0, total, money; i < n; i ++) {
//                String[] cnt = br.readLine().split(" ");
//                total = Integer.parseInt(cnt[0]);
//                money = Integer.parseInt(cnt[1]);
//                int x = (money + total - 1) / total;
//                String[] people = br.readLine().split(" ");
//                for(int j = 0; j < people.length; j ++) {
//                    ans[Integer.parseInt(people[j]) - 1] += x;
//                }
//            }
//        }
//        for (int an : ans) {
//            out.print(an + " ");
//        }
//
//        out.flush();
//        out.close();
//        br.close();
//    }


//    public static void main(String[] args) throws Exception {
//        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
//        StreamTokenizer in = new StreamTokenizer(br);
//        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
//        while(in.nextToken() != StreamTokenizer.TT_EOF) {
//            String[] s = br.readLine().split(" ");
//            int a = Integer.parseInt(s[0]); int b = Integer.parseInt(s[1]);
//            int c = Integer.parseInt(s[2]); int d = Integer.parseInt(s[3]);
//            int n = (int) in.nval;
//            for(int i = 0; i < n; i ++) {
//                in.nextToken(); int x = (int) in.nval;
//                in.nextToken(); int y = (int) in.nval;
//
//            }
//        }
//        out.flush();
//        out.close();
//        br.close();
//    }

    static PriorityQueue<Long> pq = new PriorityQueue<>();
    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        StreamTokenizer in = new StreamTokenizer(br);
        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out));
        while(in.nextToken() != StreamTokenizer.TT_EOF) {
            long a = (long) in.nval; in.nextToken();
            long b = (long) in.nval; in.nextToken();
            long c = (long) in.nval; in.nextToken();
            long k = (long) in.nval; in.nextToken();
            pq.addAll(Arrays.asList(a, b, c));
        }
        out.flush();
        out.close();
        br.close();
    }

}
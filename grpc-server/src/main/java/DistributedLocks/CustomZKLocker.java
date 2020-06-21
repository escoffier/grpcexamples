package DistributedLocks;

import com.google.common.collect.Lists;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.LockInternals;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomZKLocker implements DLock {
    private static Logger logger = LoggerFactory.getLogger(CustomZKLocker.class);

    private CuratorFramework client;
    private String paht;
    private static final String LOCK_NAME = "lock-";

    private final Watcher watcher = new Watcher()
    {
        @Override
        public void process(WatchedEvent event)
        {
            client.postSafeNotify(CustomZKLocker.this);
        }
    };

    public CustomZKLocker(CuratorFramework curatorFramework, String path) {
        client = curatorFramework;
        this.paht = path;
        //this.lockName = "lock-";
    }

    @Override
    public boolean lock() throws Exception {
        String ourPath = client.create()
                .creatingParentContainersIfNeeded()
                .withProtection()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath("/_locknode_/lock-");
        logger.info("ourpath: {}", ourPath);
        internalLockLoop(ourPath);
        return false;
    }

    private boolean internalLockLoop(String ourPath) {
        boolean     haveTheLock = false;
        try {
            String subNmae = ourPath.substring("/_locknode_".length() + 1);
            while (client.getState() == CuratorFrameworkState.STARTED && !haveTheLock) {
                List<String> children  = client.getChildren().forPath("/_locknode_");
                List<String> sortedList = Lists.newArrayList(children);
                Collections.sort(sortedList, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return sequenceNum(o1, LOCK_NAME).compareTo(sequenceNum(o2, LOCK_NAME));
                    }
                });

                logger.info("sorted list {}", sortedList.toString());

                int ourIndex = sortedList.indexOf(subNmae);
                if (ourIndex == 0 ) {
                    haveTheLock = true;
                    logger.info("thread {} get lock", Thread.currentThread().getName());
                    break;
                } else {
                    String watchPath = "/_locknode_/" + sortedList.get(ourIndex - 1 );
                    client.getData().usingWatcher(watcher).forPath(watchPath);
                    wait();
                }
            }
        } catch ( Exception ex ){

        }

        return haveTheLock;
    }

    private String sequenceNum(String str, String lockName) {
        int index = str.lastIndexOf(lockName);
        if (index > -1) {
            index += lockName.length();
        }
        return index < str.length() ? str.substring(index):"";
    }

    @Override
    public void unLock() {

    }

//    public static void main(String[] args) {
//        final String zookeeperConnectionString = "192.168.254.132:2181";
//
//        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
//        CuratorFramework client = CuratorFrameworkFactory.newClient(zookeeperConnectionString, retryPolicy);
//        client.start();
//
////        DLock dLock = new CustomZKLocker(client, "nouse");
////        try {
////            dLock.lock();
////        } catch (Exception ex) {
////logger.info(ex.getMessage());
////        }
//
//        ExecutorService executorService = Executors.newFixedThreadPool(5);
//        for (int i = 0; i < 5; i++) {
//            executorService.submit(()->{
//                DLock lock = new CustomZKLocker(client, "nouse");
//                try {
//                    lock.lock();
//                } catch (Exception ex) {
//                    logger.info(ex.getMessage());
//                }
//            });
//        }
//
//    }
}

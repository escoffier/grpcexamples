package DistributedLocks;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ZookeeperDLock implements DLock{

    private static final Logger logger = LoggerFactory.getLogger(ZookeeperDLock.class);
    private InterProcessMutex lock;
    private final String clientName;

    public ZookeeperDLock(CuratorFramework client, String lockPath, String clientName) {
        this.clientName = clientName;
        lock = new InterProcessMutex(client, lockPath);
    }

    @Override
    public boolean lock() throws Exception{
        logger.info("trying to acquire lock by " + clientName);
        if (lock.acquire(-1, null)){
            try {
                logger.info("acquired lock  by client {}",clientName);
                Thread.sleep(20*1000);
            }
            finally {
                logger.debug("release lock by client " + clientName);
                lock.release();
            }
            return true;
        }
        return false;
    }

    @Override
    public void unLock() {

    }
}

package DistributedLocks;

public interface DLock {

    boolean lock() throws Exception;
    void unLock();
}

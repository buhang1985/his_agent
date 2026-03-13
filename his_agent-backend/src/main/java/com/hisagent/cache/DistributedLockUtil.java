package com.hisagent.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类
 * 基于 Redisson 实现分布式锁
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockUtil {

    private final RedissonClient redissonClient;

    private static final String LOCK_PREFIX = "lock:";
    private static final long DEFAULT_WAIT_TIME = 10;
    private static final long DEFAULT_LEASE_TIME = 30;

    /**
     * 执行带分布式锁的操作
     *
     * @param lockKey 锁键
     * @param action  要执行的操作
     * @param <T>     返回值类型
     * @return 操作结果
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS, action);
    }

    /**
     * 执行带分布式锁的操作
     *
     * @param lockKey     锁键
     * @param waitTime    等待时间
     * @param leaseTime   锁持有时间
     * @param timeUnit    时间单位
     * @param action      要执行的操作
     * @param <T>         返回值类型
     * @return 操作结果
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> action) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (isLocked) {
                log.debug("Lock acquired: {}", fullLockKey);
                return action.get();
            } else {
                log.warn("Failed to acquire lock: {}", fullLockKey);
                throw new RuntimeException("Failed to acquire distributed lock: " + fullLockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lock interrupted: {}", fullLockKey, e);
            throw new RuntimeException("Distributed lock interrupted", e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Lock released: {}", fullLockKey);
            }
        }
    }

    /**
     * 执行带分布式锁的操作（无返回值）
     *
     * @param lockKey 锁键
     * @param action  要执行的操作
     */
    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 尝试获取锁（非阻塞）
     *
     * @param lockKey 锁键
     * @return true 如果获取成功
     */
    public boolean tryLock(String lockKey) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        return lock.tryLock();
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁键
     */
    public void unlock(String lockKey) {
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}

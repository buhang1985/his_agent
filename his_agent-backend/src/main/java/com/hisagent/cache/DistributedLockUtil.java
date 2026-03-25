package com.hisagent.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类 - 无 Redis 版本
 * 当 RedissonClient 为 null 时，使用本地锁
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
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> action) {
        return executeWithLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS, action);
    }

    /**
     * 执行带分布式锁的操作
     */
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> action) {
        // 如果 RedissonClient 为空，使用本地锁
        if (redissonClient == null) {
            log.debug("RedissonClient 为空，使用本地锁: {}", lockKey);
            return executeWithLocalLock(lockKey, action);
        }

        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(waitTime, leaseTime, timeUnit);
            if (isLocked) {
                log.debug("分布式锁获取成功: {}", fullLockKey);
                return action.get();
            } else {
                log.warn("分布式锁获取失败: {}", fullLockKey);
                throw new RuntimeException("Failed to acquire distributed lock: " + fullLockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("分布式锁中断: {}", fullLockKey, e);
            throw new RuntimeException("Distributed lock interrupted", e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("分布式锁释放: {}", fullLockKey);
            }
        }
    }

    /**
     * 本地锁实现（当 Redis 不可用时）
     */
    private <T> T executeWithLocalLock(String lockKey, Supplier<T> action) {
        synchronized (DistributedLockUtil.class) {
            return action.get();
        }
    }

    /**
     * 执行带分布式锁的操作（无返回值）
     */
    public void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, () -> {
            action.run();
            return null;
        });
    }

    /**
     * 尝试获取锁（非阻塞）
     */
    public boolean tryLock(String lockKey) {
        if (redissonClient == null) {
            log.debug("RedissonClient 为空，本地锁已获取: {}", lockKey);
            return true;
        }
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        return lock.tryLock();
    }

    /**
     * 释放锁
     */
    public void unlock(String lockKey) {
        if (redissonClient == null) {
            log.debug("RedissonClient 为空，本地锁已释放: {}", lockKey);
            return;
        }
        String fullLockKey = LOCK_PREFIX + lockKey;
        RLock lock = redissonClient.getLock(fullLockKey);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}

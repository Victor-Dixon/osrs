package com.example.rsautomation.engine;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ActionQueue {
    private final BlockingQueue<Action> queue = new LinkedBlockingQueue<>();
    private final Set<String> pendingFingerprints = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static final long DEFAULT_COOLDOWN_MS = 1000;

    public boolean submit(Action action, boolean deduplicate) {
        String fingerprint = action.getFingerprint();

        if (deduplicate) {
            Long lastSubmit = cooldowns.get(fingerprint);
            if (lastSubmit != null && System.currentTimeMillis() - lastSubmit < DEFAULT_COOLDOWN_MS) {
                return false;
            }

            if (pendingFingerprints.contains(fingerprint)) {
                return false;
            }

            pendingFingerprints.add(fingerprint);
            cooldowns.put(fingerprint, System.currentTimeMillis());
        }

        boolean offered = queue.offer(action);
        if (!offered && deduplicate) {
            pendingFingerprints.remove(fingerprint);
        }
        return offered;
    }

    public Action take() throws InterruptedException {
        Action action = queue.take();
        if (action != null) {
            pendingFingerprints.remove(action.getFingerprint());
        }
        return action;
    }

    public Action poll(long timeout, TimeUnit unit) throws InterruptedException {
        Action action = queue.poll(timeout, unit);
        if (action != null) {
            pendingFingerprints.remove(action.getFingerprint());
        }
        return action;
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            queue.clear();
            pendingFingerprints.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int size() {
        return queue.size();
    }

    public boolean hasPending(String fingerprint) {
        return pendingFingerprints.contains(fingerprint);
    }
}

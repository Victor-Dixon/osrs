package com.example.rsautomation.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ActionQueueTest {

    @Test
    void deduplicateRejectsMatchingPendingFingerprint() {
        ActionQueue queue = new ActionQueue();
        Action first = new MineAction("first", 100, 200, null, 1);
        Action duplicate = new MineAction("second", 100, 200, null, 1);

        assertTrue(queue.submit(first, true));
        assertFalse(queue.submit(duplicate, true));
    }

    @Test
    void consumeClearsPendingFingerprint() throws InterruptedException {
        ActionQueue queue = new ActionQueue();
        Action first = new MineAction("first", 100, 200, null, 1);
        Action second = new MineAction("second", 100, 200, null, 1);

        assertTrue(queue.submit(first, true));
        assertNotNull(queue.take());
        Thread.sleep(1100);

        assertTrue(queue.submit(second, true));
    }
}

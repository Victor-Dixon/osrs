package com.example.rsautomation.state;

import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class SnapshotManager {
    private final Client client;
    private final ClientThread clientThread;
    private final AtomicInteger snapshotId = new AtomicInteger(0);
    private final AtomicReference<TickSnapshot> latestSnapshot = new AtomicReference<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile int currentVersion = 0;

    @Inject
    public SnapshotManager(Client client, ClientThread clientThread) {
        this.client = client;
        this.clientThread = clientThread;
    }

    /**
     * Called on game tick - synchronous capture on client thread.
     */
    public void onGameTick() {
        if (!client.isClientThread()) {
            clientThread.invoke(this::captureSnapshot);
            return;
        }
        captureSnapshot();
    }

    private void captureSnapshot() {
        lock.writeLock().lock();
        try {
            int id = snapshotId.incrementAndGet();

            Player local = client.getLocalPlayer();
            if (local == null) {
                return;
            }

            WorldPoint playerWp = local.getWorldLocation();
            int playerX = playerWp.getX();
            int playerY = playerWp.getY();
            int plane = client.getPlane();

            List<ObjectRecord> objects = captureWorldObjects(plane);
            List<InventoryRecord> inventory = captureInventory();
            int occupiedSlots = inventory.size();
            int freeSlots = 28 - occupiedSlots;

            TickSnapshot snapshot = new TickSnapshot(
                id,
                objects,
                inventory,
                freeSlots,
                occupiedSlots,
                playerX,
                playerY,
                plane,
                client.getEnergy(),
                System.currentTimeMillis()
            );

            latestSnapshot.set(snapshot);
            currentVersion++;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<ObjectRecord> captureWorldObjects(int plane) {
        List<ObjectRecord> objects = new ArrayList<>();
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        LocalPoint playerLoc = client.getLocalPlayer().getLocalLocation();

        for (int dx = -15; dx <= 15; dx++) {
            for (int dy = -15; dy <= 15; dy++) {
                int sceneX = playerLoc.getSceneX() + dx;
                int sceneY = playerLoc.getSceneY() + dy;
                if (sceneX < 0 || sceneX >= 104 || sceneY < 0 || sceneY >= 104) {
                    continue;
                }

                Tile tile = tiles[plane][sceneX][sceneY];
                if (tile == null) {
                    continue;
                }

                for (GameObject obj : tile.getGameObjects()) {
                    if (obj != null && obj.getId() != -1) {
                        WorldPoint wp = WorldPoint.fromLocal(client, obj.getLocalLocation());
                        String name = resolveObjectName(obj);

                        Point screen = Perspective.localToCanvas(
                            client,
                            obj.getLocalLocation(),
                            plane,
                            obj.getLogicalHeight()
                        );

                        objects.add(new ObjectRecord(
                            obj.getId(),
                            wp.getX(),
                            wp.getY(),
                            wp.getPlane(),
                            screen != null ? screen.getX() : -1,
                            screen != null ? screen.getY() : -1,
                            name,
                            screen != null
                        ));
                    }
                }
            }
        }
        return objects;
    }

    private String resolveObjectName(GameObject obj) {
        try {
            ObjectComposition comp = client.getObjectDefinition(obj.getId());
            if (comp != null) {
                String name = comp.getName();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } catch (Exception ignored) {
            // Fall through to ID-based name.
        }

        return "object_" + obj.getId();
    }

    private List<InventoryRecord> captureInventory() {
        List<InventoryRecord> inventory = new ArrayList<>();
        ItemContainer inv = client.getItemContainer(InventoryID.INVENTORY);
        if (inv != null) {
            for (Item item : inv.getItems()) {
                if (item.getId() != -1) {
                    String name = resolveItemName(item.getId());
                    inventory.add(new InventoryRecord(item.getId(), item.getQuantity(), name));
                }
            }
        }
        return inventory;
    }

    private String resolveItemName(int itemId) {
        try {
            ItemComposition comp = client.getItemDefinition(itemId);
            if (comp != null) {
                String name = comp.getName();
                if (name != null && !name.isEmpty()) {
                    return name;
                }
            }
        } catch (Exception ignored) {
            // Fall through.
        }
        return "item_" + itemId;
    }

    public SnapshotView getSnapshotView() {
        lock.readLock().lock();
        try {
            TickSnapshot snapshot = latestSnapshot.get();
            return snapshot != null ? new SnapshotView(snapshot, currentVersion) : null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getCurrentVersion() {
        return currentVersion;
    }
}

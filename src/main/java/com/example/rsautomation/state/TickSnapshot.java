package com.example.rsautomation.state;

import java.util.List;

public final class TickSnapshot {
    private final int id;
    private final List<ObjectRecord> objects;
    private final List<InventoryRecord> inventory;
    private final int freeInventorySlots;
    private final int occupiedInventorySlots;
    private final int playerX;
    private final int playerY;
    private final int plane;
    private final int runEnergy;
    private final long timestamp;

    public TickSnapshot(
        int id,
        List<ObjectRecord> objects,
        List<InventoryRecord> inventory,
        int freeInventorySlots,
        int occupiedInventorySlots,
        int playerX,
        int playerY,
        int plane,
        int runEnergy,
        long timestamp
    ) {
        this.id = id;
        this.objects = List.copyOf(objects);
        this.inventory = List.copyOf(inventory);
        this.freeInventorySlots = freeInventorySlots;
        this.occupiedInventorySlots = occupiedInventorySlots;
        this.playerX = playerX;
        this.playerY = playerY;
        this.plane = plane;
        this.runEnergy = runEnergy;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public List<ObjectRecord> getObjects() {
        return objects;
    }

    public List<InventoryRecord> getInventory() {
        return inventory;
    }

    public int getFreeInventorySlots() {
        return freeInventorySlots;
    }

    public int getOccupiedInventorySlots() {
        return occupiedInventorySlots;
    }

    public int getPlayerX() {
        return playerX;
    }

    public int getPlayerY() {
        return playerY;
    }

    public int getPlane() {
        return plane;
    }

    public int getRunEnergy() {
        return runEnergy;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

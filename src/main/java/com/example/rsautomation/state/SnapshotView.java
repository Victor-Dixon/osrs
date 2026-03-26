package com.example.rsautomation.state;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Immutable view of a snapshot with version tracking.
 */
public class SnapshotView {
    private final TickSnapshot snapshot;
    private final int version;

    public SnapshotView(TickSnapshot snapshot, int version) {
        this.snapshot = snapshot;
        this.version = version;
    }

    public int getId() {
        return snapshot.getId();
    }

    public int getVersion() {
        return version;
    }

    public List<ObjectRecord> getObjects() {
        return snapshot.getObjects();
    }

    public List<InventoryRecord> getInventory() {
        return snapshot.getInventory();
    }

    public int getFreeInventorySlots() {
        return snapshot.getFreeInventorySlots();
    }

    public int getOccupiedInventorySlots() {
        return snapshot.getOccupiedInventorySlots();
    }

    public int getPlayerX() {
        return snapshot.getPlayerX();
    }

    public int getPlayerY() {
        return snapshot.getPlayerY();
    }

    public int getPlane() {
        return snapshot.getPlane();
    }

    public int getRunEnergy() {
        return snapshot.getRunEnergy();
    }

    public long getTimestamp() {
        return snapshot.getTimestamp();
    }

    public List<ObjectRecord> getInteractableObjects() {
        return snapshot.getObjects().stream()
            .filter(ObjectRecord::isInteractable)
            .collect(Collectors.toList());
    }

    public boolean hasInventorySpace() {
        return snapshot.getFreeInventorySlots() > 0;
    }

    public boolean isSnapshotVersion(int expectedVersion) {
        return version == expectedVersion;
    }
}

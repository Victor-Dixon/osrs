package com.example.rsautomation.state;

import java.util.Objects;

public final class ObjectRecord {
    private final int id;
    private final int worldX;
    private final int worldY;
    private final int plane;
    private final int screenX;
    private final int screenY;
    private final String name;
    private final boolean interactable;

    public ObjectRecord(
        int id,
        int worldX,
        int worldY,
        int plane,
        int screenX,
        int screenY,
        String name,
        boolean interactable
    ) {
        this.id = id;
        this.worldX = worldX;
        this.worldY = worldY;
        this.plane = plane;
        this.screenX = screenX;
        this.screenY = screenY;
        this.name = name;
        this.interactable = interactable;
    }

    public int getId() {
        return id;
    }

    public int getWorldX() {
        return worldX;
    }

    public int getWorldY() {
        return worldY;
    }

    public int getPlane() {
        return plane;
    }

    public int getScreenX() {
        return screenX;
    }

    public int getScreenY() {
        return screenY;
    }

    public String getName() {
        return name;
    }

    public boolean isInteractable() {
        return interactable;
    }

    public boolean hasScreenCoordinates() {
        return screenX != -1 && screenY != -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ObjectRecord)) {
            return false;
        }
        ObjectRecord that = (ObjectRecord) o;
        return id == that.id && worldX == that.worldX && worldY == that.worldY && plane == that.plane;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, worldX, worldY, plane);
    }
}

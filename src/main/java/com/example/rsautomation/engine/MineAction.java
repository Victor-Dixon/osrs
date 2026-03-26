package com.example.rsautomation.engine;

import com.example.rsautomation.state.ObjectRecord;
import com.example.rsautomation.state.SnapshotView;

public class MineAction extends Action {
    private final ObjectRecord target;

    public MineAction(String actionId, int snapshotId, int snapshotVersion, ObjectRecord target, int maxStaleness) {
        super(actionId, snapshotId, snapshotVersion, maxStaleness);
        this.target = target;
    }

    public ObjectRecord getTarget() {
        return target;
    }

    @Override
    public ValidationResult validate(SnapshotView current) {
        if (current.getId() - getOriginatingSnapshotId() > getMaxStaleness()) {
            return ValidationResult.invalid(String.format(
                "STALE: age=%d, max=%d",
                current.getId() - getOriginatingSnapshotId(),
                getMaxStaleness()
            ));
        }

        if (current.getVersion() < getOriginatingSnapshotVersion()) {
            return ValidationResult.invalid("VERSION_REGRESSION");
        }

        boolean targetExists = current.getObjects().stream()
            .anyMatch(obj ->
                obj.getWorldX() == target.getWorldX()
                    && obj.getWorldY() == target.getWorldY()
                    && obj.getPlane() == target.getPlane()
                    && obj.getId() == target.getId());
        if (!targetExists) {
            return ValidationResult.invalid("TARGET_GONE");
        }

        if (!current.hasInventorySpace()) {
            return ValidationResult.invalid("INVENTORY_FULL");
        }

        return ValidationResult.valid();
    }

    @Override
    public String getFingerprint() {
        return String.format("mine:%d:%d:%d", target.getId(), target.getWorldX(), target.getWorldY());
    }
}

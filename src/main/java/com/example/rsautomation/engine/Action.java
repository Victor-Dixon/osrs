package com.example.rsautomation.engine;

import com.example.rsautomation.state.SnapshotView;

/**
 * Immutable action intent - no side effects, no dependencies on execution context.
 */
public abstract class Action {
    private final String actionId;
    private final int originatingSnapshotId;
    private final int originatingSnapshotVersion;
    private final int maxStaleness;
    private final long createdAt;

    protected Action(String actionId, int originatingSnapshotId, int originatingSnapshotVersion, int maxStaleness) {
        this.actionId = actionId;
        this.originatingSnapshotId = originatingSnapshotId;
        this.originatingSnapshotVersion = originatingSnapshotVersion;
        this.maxStaleness = maxStaleness;
        this.createdAt = System.currentTimeMillis();
    }

    public String getActionId() {
        return actionId;
    }

    public int getOriginatingSnapshotId() {
        return originatingSnapshotId;
    }

    public int getOriginatingSnapshotVersion() {
        return originatingSnapshotVersion;
    }

    public int getMaxStaleness() {
        return maxStaleness;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Validate action against current snapshot.
     */
    public abstract ValidationResult validate(SnapshotView currentSnapshot);

    /**
     * Get unique fingerprint for deduplication.
     */
    public abstract String getFingerprint();

    public static class ValidationResult {
        private final boolean valid;
        private final String reason;

        public ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason);
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }
    }
}

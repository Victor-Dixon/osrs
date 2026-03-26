package com.example.rsautomation.engine;

import com.example.rsautomation.execution.JnaInput;
import com.example.rsautomation.execution.Kinematics;
import com.example.rsautomation.persistence.ReplayLogger;
import com.example.rsautomation.state.ObjectRecord;
import com.example.rsautomation.state.SnapshotManager;
import com.example.rsautomation.state.SnapshotView;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class ActionExecutor {
    private final SnapshotManager snapshotManager;
    private final ActionQueue actionQueue;
    private final JnaInput input;
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicReference<ExecutingAction> currentAction = new AtomicReference<>();

    @Inject
    public ActionExecutor(SnapshotManager snapshotManager, ActionQueue actionQueue, JnaInput input) {
        this.snapshotManager = snapshotManager;
        this.actionQueue = actionQueue;
        this.input = input;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ActionExecutor");
            t.setDaemon(true);
            return t;
        });
        this.executor.submit(this::runLoop);
    }

    private void runLoop() {
        while (running.get()) {
            try {
                Action action = actionQueue.take();

                SnapshotView snapshot = snapshotManager.getSnapshotView();
                if (snapshot == null) {
                    ReplayLogger.logAbort(action, "NO_SNAPSHOT");
                    continue;
                }

                Action.ValidationResult validation = action.validate(snapshot);
                if (!validation.isValid()) {
                    ReplayLogger.logAbort(action, validation.getReason());
                    continue;
                }

                currentAction.set(new ExecutingAction(action));
                executeAction(action);
                currentAction.set(null);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                ReplayLogger.logError("Action execution error", e);
            }
        }
    }

    private void executeAction(Action action) {
        if (action instanceof MineAction) {
            executeMine((MineAction) action);
        }
    }

    private void executeMine(MineAction action) {
        ObjectRecord target = action.getTarget();
        if (!target.hasScreenCoordinates()) {
            ReplayLogger.logAbort(action, "NO_SCREEN_COORDINATES");
            return;
        }

        ReplayLogger.logExecution(action, "Moving to " + target.getName());

        boolean success = Kinematics.moveTo(
            input,
            target.getScreenX(),
            target.getScreenY(),
            20,
            () -> {
                ExecutingAction executing = currentAction.get();
                return executing != null && executing.isAborted();
            }
        );

        if (success) {
            ReplayLogger.logExecution(action, "Completed mining");
        } else {
            ReplayLogger.logAbort(action, "ABORTED_DURING_EXECUTION");
        }
    }

    public boolean abortAction(String actionId) {
        ExecutingAction executing = currentAction.get();
        if (executing != null && executing.getAction().getActionId().equals(actionId)) {
            executing.abort();
            return true;
        }
        return false;
    }

    public void shutdown() {
        running.set(false);
        executor.shutdownNow();
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static class ExecutingAction {
        private final Action action;
        private final AtomicBoolean aborted = new AtomicBoolean(false);

        private ExecutingAction(Action action) {
            this.action = action;
        }

        public Action getAction() {
            return action;
        }

        public void abort() {
            aborted.set(true);
        }

        public boolean isAborted() {
            return aborted.get();
        }
    }
}

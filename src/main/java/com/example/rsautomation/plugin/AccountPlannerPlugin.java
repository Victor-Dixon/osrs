package com.example.rsautomation.plugin;

import com.example.rsautomation.engine.Action;
import com.example.rsautomation.engine.ActionExecutor;
import com.example.rsautomation.engine.ActionQueue;
import com.example.rsautomation.engine.Planner;
import com.example.rsautomation.execution.JnaInput;
import com.example.rsautomation.persistence.GoalStack;
import com.example.rsautomation.persistence.ReplayLogger;
import com.example.rsautomation.state.SnapshotManager;
import com.example.rsautomation.state.SnapshotView;
import com.google.inject.Inject;
import com.google.inject.Provides;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(name = "RS Formal Automation")
public class AccountPlannerPlugin extends Plugin {
    @Inject
    private SnapshotManager snapshotManager;

    @Inject
    private ActionQueue actionQueue;

    @Inject
    private ActionExecutor actionExecutor;

    @Inject
    private JnaInput jnaInput;

    @Inject
    private GoalStack goalStack;

    private int lastPlannedSnapshotVersion = -1;
    private String lastPlannedFingerprint = null;

    @Override
    protected void startUp() {
        ReplayLogger.initialize();
        ReplayLogger.logInfo("Plugin started");
    }

    @Override
    protected void shutDown() {
        ReplayLogger.logInfo("Plugin shutting down");
        actionExecutor.shutdown();
        ReplayLogger.shutdown();
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        try {
            snapshotManager.onGameTick();

            SnapshotView snapshot = snapshotManager.getSnapshotView();
            if (snapshot == null) {
                return;
            }

            if (snapshot.getVersion() == lastPlannedSnapshotVersion) {
                return;
            }
            lastPlannedSnapshotVersion = snapshot.getVersion();

            Action action = Planner.plan(snapshot, goalStack, jnaInput);
            if (action != null) {
                String fingerprint = action.getFingerprint();
                if (!fingerprint.equals(lastPlannedFingerprint) || !actionQueue.hasPending(fingerprint)) {

                    if (actionQueue.submit(action, true)) {
                        ReplayLogger.logExecution(action, "Submitted to queue");
                        lastPlannedFingerprint = fingerprint;
                    } else {
                        ReplayLogger.logInfo("Action rejected: " + fingerprint + " already queued or on cooldown");
                    }
                }
            }
        } catch (Exception e) {
            ReplayLogger.logError("Error in game tick handler", e);
        }
    }

    @Provides
    JnaInput provideJnaInput() {
        return new JnaInput();
    }

    @Provides
    GoalStack provideGoalStack() {
        return new GoalStack();
    }
}

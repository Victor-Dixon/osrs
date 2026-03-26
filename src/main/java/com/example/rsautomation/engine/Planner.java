package com.example.rsautomation.engine;

import com.example.rsautomation.execution.JnaInput;
import com.example.rsautomation.persistence.GoalStack;
import com.example.rsautomation.state.ObjectRecord;
import com.example.rsautomation.state.SnapshotView;

import java.util.Comparator;
import java.util.UUID;

public class Planner {
    public static Action plan(SnapshotView snapshot, GoalStack goalStack, JnaInput input) {
        String currentGoal = goalStack.peekGoal();
        if (currentGoal != null && currentGoal.startsWith("mine_")) {
            String oreType = currentGoal.substring(5);
            int targetOreId = getOreId(oreType);

            ObjectRecord target = snapshot.getInteractableObjects().stream()
                .filter(obj -> obj.getId() == targetOreId)
                .min(Comparator.comparingDouble(obj ->
                    Math.hypot(obj.getWorldX() - snapshot.getPlayerX(), obj.getWorldY() - snapshot.getPlayerY())))
                .orElse(null);

            if (target != null && snapshot.hasInventorySpace()) {
                String actionId = UUID.randomUUID().toString();
                return new MineAction(actionId, snapshot.getId(), snapshot.getVersion(), target, 5);
            }
        }

        ObjectRecord target = snapshot.getInteractableObjects().stream()
            .filter(obj -> obj.getId() == 11364)
            .min(Comparator.comparingDouble(obj ->
                Math.hypot(obj.getWorldX() - snapshot.getPlayerX(), obj.getWorldY() - snapshot.getPlayerY())))
            .orElse(null);

        if (target != null && snapshot.hasInventorySpace()) {
            String actionId = UUID.randomUUID().toString();
            return new MineAction(actionId, snapshot.getId(), snapshot.getVersion(), target, 5);
        }

        return null;
    }

    private static int getOreId(String oreType) {
        switch (oreType.toLowerCase()) {
            case "iron":
                return 11364;
            case "copper":
                return 10943;
            case "tin":
                return 10944;
            default:
                return 11364;
        }
    }
}

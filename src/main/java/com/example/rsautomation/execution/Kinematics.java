package com.example.rsautomation.execution;

import java.util.Random;
import java.util.function.BooleanSupplier;

public class Kinematics {
    private static final Random RNG = new Random();

    public static boolean moveTo(JnaInput input, int targetX, int targetY, double targetWidth, BooleanSupplier abort) {
        int startX = input.getCursorX();
        int startY = input.getCursorY();
        double distance = Math.hypot(targetX - startX, targetY - startY);

        if (distance < 1.0) {
            input.click();
            return true;
        }

        double id = Math.max(0.1, Math.log(distance / targetWidth + 1) / Math.log(2));
        double totalTimeMs = 300 + 150 * id;
        totalTimeMs = Math.min(800, Math.max(150, totalTimeMs));

        double[] ctrl = generateControlPoints(startX, startY, targetX, targetY, distance);
        int steps = Math.max(2, (int) Math.round(totalTimeMs / 16.67));
        double[] times = new double[steps];
        double[] velocities = logNormalVelocity(steps);
        double cum = 0;
        for (int i = 0; i < steps; i++) {
            cum += velocities[i] * totalTimeMs;
            times[i] = cum;
        }

        long start = System.nanoTime();
        for (int i = 0; i < steps; i++) {
            if (abort.getAsBoolean()) {
                return false;
            }

            long now = System.nanoTime();
            double elapsed = (now - start) / 1_000_000.0;
            double targetTime = times[i];
            if (elapsed < targetTime) {
                try {
                    long sleepMs = (long) (targetTime - elapsed);
                    if (sleepMs > 0) {
                        Thread.sleep(sleepMs);
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            double t = (steps > 1) ? (double) i / (steps - 1) : 0.0;
            double t1 = 1 - t;
            double x = t1 * t1 * t1 * startX + 3 * t1 * t1 * t * ctrl[0] + 3 * t1 * t * t * ctrl[2] + t * t * t * targetX;
            double y = t1 * t1 * t1 * startY + 3 * t1 * t1 * t * ctrl[1] + 3 * t1 * t * t * ctrl[3] + t * t * t * targetY;
            input.moveTo((int) Math.round(x), (int) Math.round(y));
        }

        if (!abort.getAsBoolean()) {
            input.moveTo(targetX + RNG.nextInt(3) - 1, targetY + RNG.nextInt(3) - 1);
            try {
                Thread.sleep(RNG.nextInt(30) + 20);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                return false;
            }
            input.moveTo(targetX, targetY);
            input.click();
            return true;
        }

        return false;
    }

    private static double[] generateControlPoints(int x1, int y1, int x2, int y2, double dist) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double perpX = -dy / dist;
        double perpY = dx / dist;
        double ctrlDist = dist * (0.1 + RNG.nextDouble() * 0.2);
        double side = RNG.nextBoolean() ? 1 : -1;
        double c1x = x1 + dx * 0.25 + side * perpX * ctrlDist;
        double c1y = y1 + dy * 0.25 + side * perpY * ctrlDist;
        double c2x = x1 + dx * 0.75 + side * perpX * ctrlDist;
        double c2y = y1 + dy * 0.75 + side * perpY * ctrlDist;
        return new double[]{c1x, c1y, c2x, c2y};
    }

    private static double[] logNormalVelocity(int steps) {
        double[] vel = new double[steps];
        double mu = 0.0;
        double sigma = 0.5;
        double sum = 0;
        for (int i = 0; i < steps; i++) {
            double t = (i + 0.5) / steps;
            double x = t + 0.01;
            double logTerm = Math.log(x);
            double exponent = -(logTerm - mu) * (logTerm - mu) / (2 * sigma * sigma);
            vel[i] = (1.0 / (x * sigma * Math.sqrt(2 * Math.PI))) * Math.exp(exponent);
            sum += vel[i];
        }
        for (int i = 0; i < steps; i++) {
            vel[i] /= sum;
        }
        return vel;
    }
}

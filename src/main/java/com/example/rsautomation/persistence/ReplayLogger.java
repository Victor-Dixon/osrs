package com.example.rsautomation.persistence;

import com.example.rsautomation.engine.Action;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class ReplayLogger {
    private static PrintWriter writer;
    private static final ReentrantLock lock = new ReentrantLock();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static volatile boolean initialized = false;

    public static void initialize() {
        if (initialized) {
            return;
        }

        lock.lock();
        try {
            if (initialized) {
                return;
            }

            File logDir = new File(System.getProperty("user.home"), ".rsbot/logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }

            String logFile = new File(
                logDir,
                "replay_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".log"
            ).getAbsolutePath();
            writer = new PrintWriter(new FileWriter(logFile, true), true);
            initialized = true;

            Runtime.getRuntime().addShutdownHook(new Thread(ReplayLogger::shutdown));

            logInfo("Logger initialized");
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
            writer = new PrintWriter(System.out, true);
            initialized = true;
        } finally {
            lock.unlock();
        }
    }

    public static void shutdown() {
        lock.lock();
        try {
            if (writer != null) {
                logInfo("Logger shutting down");
                writer.flush();
                writer.close();
                writer = null;
            }
        } finally {
            lock.unlock();
        }
    }

    public static void logExecution(Action action, String details) {
        log("EXECUTE", action.getClass().getSimpleName(), "id=" + action.getActionId() + " | " + details);
    }

    public static void logAbort(Action action, String reason) {
        log("ABORT", action.getClass().getSimpleName(), "id=" + action.getActionId() + " | " + reason);
    }

    public static void logInfo(String message) {
        log("INFO", "System", message);
    }

    public static void logError(String message, Throwable error) {
        log("ERROR", "System", message + " | " + error.getMessage());
        if (writer != null) {
            lock.lock();
            try {
                error.printStackTrace(writer);
                writer.flush();
            } finally {
                lock.unlock();
            }
        }
    }

    private static void log(String level, String component, String message) {
        if (!initialized) {
            initialize();
        }

        lock.lock();
        try {
            if (writer != null) {
                String timestamp = dateFormat.format(new Date());
                writer.printf("[%s] %s %s: %s%n", timestamp, level, component, message);
                writer.flush();
            }
        } finally {
            lock.unlock();
        }
    }
}

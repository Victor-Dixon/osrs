package com.example.rsautomation.persistence;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Stack of goals with persistence.
 * JSON order: top of stack is first element in list.
 */
public class GoalStack {
    private final Deque<String> goals = new ArrayDeque<>();
    private final File saveFile;
    private final Gson gson;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public GoalStack() {
        this.saveFile = new File(System.getProperty("user.home"), ".rsbot/goals.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        ensureDirectoryExists();
        load();
    }

    private void ensureDirectoryExists() {
        File dir = saveFile.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }
    }

    public void pushGoal(String goal) {
        lock.writeLock().lock();
        try {
            goals.push(goal);
            save();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String peekGoal() {
        lock.readLock().lock();
        try {
            return goals.peek();
        } finally {
            lock.readLock().unlock();
        }
    }

    public String popGoal() {
        lock.writeLock().lock();
        try {
            String goal = goals.poll();
            if (goal != null) {
                save();
            }
            return goal;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void save() {
        List<String> list = new ArrayList<>(goals);
        try (Writer w = new FileWriter(saveFile)) {
            gson.toJson(list, w);
        } catch (IOException e) {
            System.err.println("Failed to save goals: " + e.getMessage());
        }
    }

    private void load() {
        if (!saveFile.exists()) {
            return;
        }

        lock.writeLock().lock();
        try (Reader r = new FileReader(saveFile)) {
            Type type = new TypeToken<ArrayList<String>>() {}.getType();
            List<String> loaded = gson.fromJson(r, type);
            if (loaded != null) {
                goals.clear();
                for (String goal : loaded) {
                    goals.addLast(goal);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading goals: " + e.getMessage());
            backupCorruptedFile();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void backupCorruptedFile() {
        try {
            File backup = new File(saveFile.getParent(), "goals_corrupted_" + System.currentTimeMillis() + ".json");
            if (saveFile.renameTo(backup)) {
                System.err.println("Backed up corrupted file to: " + backup);
            }
        } catch (Exception e) {
            System.err.println("Failed to backup corrupted file: " + e.getMessage());
        }
    }
}

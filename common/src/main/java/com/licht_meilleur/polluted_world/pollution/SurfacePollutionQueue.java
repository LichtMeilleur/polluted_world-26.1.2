package com.licht_meilleur.polluted_world.pollution;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public final class SurfacePollutionQueue {

    private static final Queue<Task> TASKS = new ArrayDeque<>();
    private static final Set<Long> QUEUED_CHUNKS = new HashSet<>();

    private static final int COLUMNS_PER_TICK = 24;
    private static final int MAX_QUEUE_SIZE = 4096;

    private SurfacePollutionQueue() {
    }

    public static void enqueue(ServerLevel level, LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        long key = chunkPos.pack();

        if (QUEUED_CHUNKS.contains(key)) {
            return;
        }

        if (TASKS.size() >= MAX_QUEUE_SIZE) {
            return;
        }

        QUEUED_CHUNKS.add(key);
        TASKS.add(new Task(level, chunk, key));
    }

    public static void tick() {
        int budget = COLUMNS_PER_TICK;

        while (budget > 0 && !TASKS.isEmpty()) {
            Task task = TASKS.peek();

            int used = SurfacePollutionTransformer.transformSomeColumns(
                    task.level,
                    task.chunk,
                    task.nextColumn,
                    budget
            );

            task.nextColumn += used;
            budget -= used;

            if (task.nextColumn >= 256 || used <= 0) {
                TASKS.poll();
                QUEUED_CHUNKS.remove(task.chunkKey);
            }
        }
    }

    private static final class Task {
        private final ServerLevel level;
        private final LevelChunk chunk;
        private final long chunkKey;
        private int nextColumn;

        private Task(ServerLevel level, LevelChunk chunk, long chunkKey) {
            this.level = level;
            this.chunk = chunk;
            this.chunkKey = chunkKey;
            this.nextColumn = 0;
        }
    }
}
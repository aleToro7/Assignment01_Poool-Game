package pcd.assignment01.controller;

import pcd.assignment01.controller.tasks.BallUpdateTask;
import pcd.assignment01.controller.tasks.CollisionTask;
import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Incapsula tutti i dettagli del Java Executor Framework.
 * Il GameLoop (master) non vede ExecutorService, Future, né invokeAll.
 */
public class WorkerPool {

    private final ExecutorService executor;
    private final int nWorkers;

    public WorkerPool() {
        this.nWorkers = Runtime.getRuntime().availableProcessors() * 2;
        this.executor = Executors.newFixedThreadPool(nWorkers);
    }

    public void parallelBallUpdate(List<Ball> balls, long dt, Board board)
            throws InterruptedException, ExecutionException {

        int n = balls.size();
        int chunkSize = Math.max(1, n / nWorkers);
        var tasks = new ArrayList<BallUpdateTask>();

        for (int i = 0; i < n; i += chunkSize) {
            tasks.add(new BallUpdateTask(balls, i, Math.min(i + chunkSize, n), dt, board));
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures)
            f.get();
    }

    public void parallelCollisionDetection(List<Ball> balls,
            ConcurrentHashMap<Ball, Integer> lastTouchedBy)
            throws InterruptedException, ExecutionException {

        int n = balls.size();
        int chunkSize = Math.max(1, n / nWorkers);
        var tasks = new ArrayList<CollisionTask>();

        for (int i = 0; i < n; i += chunkSize) {
            tasks.add(new CollisionTask(balls, i, Math.min(i + chunkSize, n), lastTouchedBy));
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures)
            f.get();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
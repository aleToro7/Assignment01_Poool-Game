package pcd.assignment01.controller;

import pcd.assignment01.controller.tasks.BallUpdateTask;
import pcd.assignment01.controller.tasks.CollisionTask;
import pcd.assignment01.controller.tasks.PlayerCollisionTask;
import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Board;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Incapsula tutti i dettagli del Java Executor Framework.
 * Il GameLoop (master) non vede ExecutorService, Future, o invokeAll.
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
        // Il lavoro i-vs-j è triangolare: si usa una suddivisione a passo
        // (interleaved) invece che a range contigui, così ogni task riceve
        // un mix bilanciato di indici "pesanti" e "leggeri".
        int workers = Math.min(nWorkers, Math.max(1, n));
        var tasks = new ArrayList<CollisionTask>();

        for (int start = 0; start < workers; start++) {
            tasks.add(new CollisionTask(balls, start, workers, lastTouchedBy));
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures)
            f.get();
    }

    public void parallelPlayerCollision(List<Ball> balls,
            Ball player1, Ball player2,
            ConcurrentHashMap<Ball, Integer> lastTouchedBy)
            throws InterruptedException, ExecutionException {

        int n = balls.size();
        int chunkSize = Math.max(1, n / nWorkers);
        var tasks = new ArrayList<PlayerCollisionTask>();

        for (int i = 0; i < n; i += chunkSize) {
            tasks.add(new PlayerCollisionTask(balls, i, Math.min(i + chunkSize, n),
                    player1, player2, lastTouchedBy));
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures)
            f.get();
    }

    public void shutdown() {
        executor.shutdown();
    }
}
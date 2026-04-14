package pcd.assignment01.concurrent;

import pcd.assignment01.concurrent.tasks.BallUpdateTask;
import pcd.assignment01.concurrent.tasks.CollisionTask;
import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Board;
import pcd.assignment01.model.V2d;
import pcd.assignment01.util.BoardConf;
import pcd.assignment01.view.View;
import pcd.assignment01.view.ViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class GameLoop extends Thread {

    private final Board board;
    private final ViewModel viewModel;
    private final View view;
    private final ExecutorService executor;
    private final int nWorkers;

    // Input asincrono dal giocatore (tastiera → questo thread)
    private final BlockingQueue<V2d> inputQueue = new LinkedBlockingQueue<>();

    public GameLoop(BoardConf conf, ViewModel viewModel, View view) {
        this.nWorkers  = Runtime.getRuntime().availableProcessors();
        this.executor  = Executors.newFixedThreadPool(nWorkers);
        this.board     = new Board();
        this.board.init(conf);
        this.viewModel = viewModel;
        this.view      = view;
    }

    /** Chiamato dal KeyListener sulla EDT */
    public void notifyInput(V2d impulse) {
        inputQueue.offer(impulse);  // non-blocking, thread-safe
    }

    @Override
    public void run() {
        long lastUpdateTime = System.currentTimeMillis();
        long t0             = System.currentTimeMillis();
        int  nFrames        = 0;

        var rand = new Random(2);
        long lastKickTimeP2 = t0;

        viewModel.update(board, 0);
        view.render();
        var  p2 = board.getPlayer2();

        while (!board.isGameOver()) {
            // --- Input giocatore (non-blocking poll) ---
            V2d impulse = inputQueue.poll();
            if (impulse != null && board.getPlayer1() != null) {
                board.getPlayer1().kick(impulse);
            }

            // --- Bot ---
            if (p2 != null && p2.getVel().abs() < 0.05
                    && System.currentTimeMillis() - lastKickTimeP2 > 2000) {
                double angle = rand.nextDouble() * Math.PI * 0.25 + Math.PI * 0.75;
                p2.kick(new V2d(Math.cos(angle), Math.sin(angle)).mul(1.5));
                lastKickTimeP2 = System.currentTimeMillis();
            }

            long elapsed    = System.currentTimeMillis() - lastUpdateTime;
            lastUpdateTime  = System.currentTimeMillis();

            try {
                // ============================================================
                // FASE 1: aggiornamento stato palline (parallelo)
                // ============================================================
                parallelBallUpdate(board.getBalls(), elapsed);

                // Aggiorna anche i player ball (sequenziale, solo 2 oggetti)
                if (board.getPlayer1() != null) board.getPlayer1().updateState(elapsed, board);
                if (board.getPlayer2() != null) board.getPlayer2().updateState(elapsed, board);

                // ============================================================
                // FASE 2: collisioni ball-ball (parallelo con partizione per righe)
                // ============================================================
                parallelCollisionDetection(board.getBalls());

                // ============================================================
                // FASE 3: collisioni player-ball e buche (sequenziale)
                // ============================================================
                board.resolvePlayerCollisionsAndHoles();

            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                break;
            }

            // --- Render ---
            nFrames++;
            long dt = System.currentTimeMillis() - t0;
            int fps = dt > 0 ? (int)(nFrames * 1000 / dt) : 0;

            viewModel.update(board, fps);
            view.render();
        }

        executor.shutdown();
    }

    // ------------------------------------------------------------------

    private void parallelBallUpdate(List<Ball> balls, long dt)
            throws InterruptedException, ExecutionException {

        int n         = balls.size();
        int chunkSize = Math.max(1, n / nWorkers);
        var tasks     = new ArrayList<BallUpdateTask>();

        for (int i = 0; i < n; i += chunkSize) {
            int to = Math.min(i + chunkSize, n);
            tasks.add(new BallUpdateTask(balls, i, to, dt, board));
        }

        // invokeAll blocca finché tutti i task non sono completati
        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures) f.get(); // propaga eventuali eccezioni
    }

    private void parallelCollisionDetection(List<Ball> balls)
            throws InterruptedException, ExecutionException {

        int n         = balls.size();
        int chunkSize = Math.max(1, n / nWorkers);
        var tasks     = new ArrayList<CollisionTask>();

        for (int i = 0; i < n; i += chunkSize) {
            int to = Math.min(i + chunkSize, n);
            // Passa la ConcurrentHashMap al task
            tasks.add(new CollisionTask(balls, i, to, board.getLastTouchedBy()));
        }

        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> f : futures) f.get();
    }

    public void stopLoop() {
        executor.shutdownNow();
        this.interrupt();
    }
}
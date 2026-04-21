package pcd.assignment01.controller;

import pcd.assignment01.model.Board;
import pcd.assignment01.model.V2d;
import pcd.assignment01.util.BoardConf;
import pcd.assignment01.view.View;
import pcd.assignment01.view.ViewModel;

import java.util.Random;
import java.util.concurrent.*;

public class GameLoop extends Thread {

    private final Board board;
    private final ViewModel viewModel;
    private final View view;
    private final WorkerPool workerPool; // ← solo questa astrazione

    // Nessun import di java.util.concurrent.* nel master
    private final BlockingQueue<V2d> inputQueue = new LinkedBlockingQueue<>();

    public GameLoop(BoardConf conf, ViewModel viewModel, View view) {
        this.workerPool = new WorkerPool(); // ← non sa che è un thread pool
        this.board      = new Board();
        this.board.init(conf);
        this.viewModel  = viewModel;
        this.view       = view;
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
                // FASE 1 — il master delega senza sapere come
                workerPool.parallelBallUpdate(board.getBalls(), elapsed, board);

                if (board.getPlayer1() != null) board.getPlayer1().updateState(elapsed, board);
                if (board.getPlayer2() != null) board.getPlayer2().updateState(elapsed, board);

                // FASE 2
                workerPool.parallelCollisionDetection(board.getBalls(), board.getLastTouchedBy());

                // FASE 3 — sequenziale, rimane nel master
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
    }

    public void stopLoop() {
        workerPool.shutdown();
        this.interrupt();
    }
}
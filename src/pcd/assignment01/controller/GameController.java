package pcd.assignment01.controller;

import javax.swing.SwingUtilities;
import pcd.assignment01.model.Board;
import pcd.assignment01.view.ViewModel;
import pcd.assignment01.view.View;

/**
 * GameController class to manage the game logic in a multithreaded way.
 * This class handles the main game loop, bot behavior, and coordination of threads.
 */
public class GameController {

    private Board board;
    private ViewModel viewModel;
    private View view;
    private volatile boolean running = true;

    // Threads
    private Thread updateThread;
    private Thread botThread;

    public GameController(Board board, ViewModel viewModel, View view) {
        this.board = board;
        this.viewModel = viewModel;
        this.view = view;
    }

    /**
     * Starts the game by initializing and starting the threads.
     */
    public void startGame() {
        // Initialize update thread for board updates
        updateThread = new Thread(this::updateLoop, "UpdateThread");
        updateThread.setDaemon(true);
        // Initialize bot thread
        botThread = new Thread(this::botLoop, "BotThread");
        botThread.setDaemon(true);

        updateThread.start();
        botThread.start();
    }

    public void stopGame() {
        running = false;
        if (updateThread != null) {
            updateThread.interrupt();
        }
        if (botThread != null) {
            botThread.interrupt();
        }
    }

    public Thread getUpdateThread() {
        return updateThread;
    }

    /**
     * Main update loop running in a separate thread.
     */
    private void updateLoop() {
        long lastUpdateTime = System.currentTimeMillis();
        int nFrames = 0;
        long t0 = System.currentTimeMillis();

        while (running) {
            long elapsed = System.currentTimeMillis() - lastUpdateTime;
            lastUpdateTime = System.currentTimeMillis();
            board.updateState(elapsed);

            // Render
            nFrames++;
            int framePerSec = 0;
            long dt = (System.currentTimeMillis() - t0);
            if (dt > 0) {
                framePerSec = (int)(nFrames * 1000 / dt);
            }

            viewModel.update(board, framePerSec);
            view.render();

            if (board.isGameOver()) {
                running = false;
                break;
            }

            // Sleep a bit to control frame rate
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Bot behavior loop running in a separate thread.
     */
    private void botLoop() {
        var rand = new java.util.Random(2);
        long lastKickTimeP2 = System.currentTimeMillis();

        synchronized (board.getBotMonitor()) {
            while (running) {
                var p2 = board.getPlayer2();
                if (p2 != null && p2.getVel().abs() < 0.05 && System.currentTimeMillis() - lastKickTimeP2 >= 2000) {
                    var angle = rand.nextDouble() * Math.PI * 0.25 + Math.PI * 0.75;
                    var v = new pcd.assignment01.model.V2d(Math.cos(angle), Math.sin(angle)).mul(1.5);
                    board.kickPlayer2(v);
                    lastKickTimeP2 = System.currentTimeMillis();
                } else {
                    try {
                        board.getBotMonitor().wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                if (board.isGameOver()) {
                    running = false;
                    break;
                }
            }
        }
    }
}
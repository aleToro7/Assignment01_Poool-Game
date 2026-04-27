package pcd.assignment01.controller;

import pcd.assignment01.model.Board;
import pcd.assignment01.view.ViewModel;
import pcd.assignment01.view.View;

public class GameController {

    private Board board;
    private ViewModel viewModel;
    private View view;
    
    // JPF traccia perfettamente i metodi synchronized, a differenza degli Atomic
    private boolean running = true;

    private synchronized boolean isRunning() {
        return running;
    }

    private synchronized void setRunning(boolean val) {
        this.running = val;
    }

    private Thread updateThread;
    private Thread botThread;

    public GameController(Board board, ViewModel viewModel, View view) {
        this.board = board;
        this.viewModel = viewModel;
        this.view = view;
    }

    public void startGame() {
        updateThread = new Thread(this::updateLoop, "UpdateThread");
        updateThread.setDaemon(true);
        botThread = new Thread(this::botLoop, "BotThread");
        botThread.setDaemon(true);

        updateThread.start();
        botThread.start();
    }

    public void stopGame() {
        setRunning(false);
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

    private void updateLoop() {
        long lastUpdateTime = System.currentTimeMillis();
        int nFrames = 0;
        long t0 = System.currentTimeMillis();

        while (isRunning()) {
            if (view == null) {
                board.updateState(16); 
                setRunning(false);     
                break;
            }

            long elapsed = System.currentTimeMillis() - lastUpdateTime;
            lastUpdateTime = System.currentTimeMillis();
            board.updateState(elapsed);

            nFrames++;
            int framePerSec = 0;
            long dt = (System.currentTimeMillis() - t0);
            if (dt > 0) {
                framePerSec = (int)(nFrames * 1000 / dt);
            }

            viewModel.update(board, framePerSec);
            view.render();

            if (board.isGameOver()) {
                setRunning(false);
                break;
            }

            /* 
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } */
        }
    }

    private void botLoop() {
        if (view == null) {
            return; 
        }

        var rand = new java.util.Random(2);
        long lastKickTimeP2 = System.currentTimeMillis();

        synchronized (board.getBotMonitor()) {
            while (isRunning()) {
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
                    setRunning(false);
                    break;
                }
            }
        }
    }
}
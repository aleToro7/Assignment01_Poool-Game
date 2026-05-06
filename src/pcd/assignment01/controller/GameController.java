package pcd.assignment01.controller;

import pcd.assignment01.model.Board;
import pcd.assignment01.model.Board.Winner;
import pcd.assignment01.view.View;
import pcd.assignment01.view.ViewModel;

/**
 * GameController gestisce il game loop principale e coordina i sotto-controller.
 *
 * Responsabilità:
 *  - updateLoop: tick della fisica, rendering, segnalazione al BotController
 *  - handleGameOver: costruisce il messaggio di fine partita e lo passa al ViewModel
 *  - buildWinnerMessage: traduce Winner enum → stringa UI (responsabilità del controller)
 *
 * NON contiene: logica del bot (→ BotController), input tastiera (→ InputHandler)
 */
public class GameController {

    private static final int  TARGET_FPS = 60;
    private static final long FRAME_MS   = 1000 / TARGET_FPS;

    private final Board         board;
    private final ViewModel     viewModel;
    private final View          view;
    private final BotController botController;

    private volatile boolean running = false;
    private Thread updateThread;

    public GameController(Board board, ViewModel viewModel, View view) {
        this.board         = board;
        this.viewModel     = viewModel;
        this.view          = view;
        this.botController = new BotController(board);
    }

    // -------------------------------------------------------------------------
    // Ciclo di vita
    // -------------------------------------------------------------------------

    public void startGame() {
        running      = true;
        updateThread = new Thread(this::updateLoop, "UpdateThread");
        updateThread.setDaemon(true);
        updateThread.start();
        botController.start();
    }

    public void stopGame() {
        running = false;
        botController.stop();
        if (updateThread != null) updateThread.interrupt();
    }

    public Thread getUpdateThread() { return updateThread; }

    // -------------------------------------------------------------------------
    // Update loop (~60 FPS)
    // -------------------------------------------------------------------------

    private void updateLoop() {
        long lastTime = System.currentTimeMillis();
        long t0       = lastTime;
        int  nFrames  = 0;

        while (running) {
            long now     = System.currentTimeMillis();
            long elapsed = now - lastTime;
            lastTime     = now;

            board.updateState(elapsed);

            // Segnala al BotController che la fisica è avanzata
            botController.signal();

            // FPS
            nFrames++;
            long dt  = now - t0;
            int  fps = (dt > 0) ? (int)(nFrames * 1000 / dt) : 0;

            viewModel.update(board, fps);
            view.render();

            if (board.isGameOver()) {
                handleGameOver();
                break;
            }

            // Cap a ~60 FPS
            long sleepMs = FRAME_MS - (System.currentTimeMillis() - now);
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        running = false;
    }

    // -------------------------------------------------------------------------
    // Fine partita
    // -------------------------------------------------------------------------

    private void handleGameOver() {
        running = false;
        botController.stop();
        String message = buildWinnerMessage(board.getWinner());
        viewModel.setGameOverMessage(message);
        view.render();
    }

    /**
     * Traduce l'enum Winner in una stringa leggibile per la UI.
     * La formattazione vive nel controller, non nel model.
     */
    private String buildWinnerMessage(Winner winner) {
        int s1 = board.getScore1();
        int s2 = board.getScore2();
        return switch (winner) {
            case PLAYER1 -> "Vittoria Giocatore H (Punti: %d a %d)".formatted(s1, s2);
            case PLAYER2 -> "Vittoria Giocatore B (Punti: %d a %d)".formatted(s2, s1);
            case DRAW    -> "Pareggio! (%d a %d)".formatted(s1, s2);
            case NONE    -> "";
        };
    }
}
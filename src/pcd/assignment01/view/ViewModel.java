package pcd.assignment01.view;

import pcd.assignment01.model.Board;
import pcd.assignment01.model.P2d;
import java.util.ArrayList;

record BallViewInfo(P2d pos, double radius) {}

public class ViewModel {

    private ArrayList<BallViewInfo> balls;
    private BallViewInfo player1;
    private BallViewInfo player2;
    private int score1;
    private int score2;
    private int framePerSec;

    private boolean isGameOver;
    private String  winnerMessage;

    public ViewModel() {
        balls         = new ArrayList<>();
        framePerSec   = 0;
        score1        = 0;
        score2        = 0;
        isGameOver    = false;
        winnerMessage = "";
    }

    /**
     * Aggiornamento chiamato ad ogni tick dall'updateLoop.
     * Copia lo stato fisico del Board nello snapshot della view.
     *
     * NON legge più winnerMessage da Board: quella stringa non esiste più
     * nel model. Il messaggio di fine partita viene impostato dal controller
     * tramite setGameOverMessage(), chiamato una sola volta a game over.
     */
    public synchronized void update(Board board, int framePerSec) {
        balls.clear();
        for (var b : board.getBalls()) {
            balls.add(new BallViewInfo(b.getPos(), b.getRadius()));
        }
        this.framePerSec = framePerSec;

        var p1 = board.getPlayer1();
        var p2 = board.getPlayer2();

        // null se il giocatore è caduto in buca
        player1 = (p1 != null) ? new BallViewInfo(p1.getPos(), p1.getRadius()) : null;
        player2 = (p2 != null) ? new BallViewInfo(p2.getPos(), p2.getRadius()) : null;

        this.score1     = board.getScore1();
        this.score2     = board.getScore2();
        this.isGameOver = board.isGameOver();
        // winnerMessage NON viene toccato qui
    }

    /**
     * Chiamato da GameController.handleGameOver() una sola volta a fine partita.
     * La stringa è già formattata dal controller (non dal model).
     */
    public synchronized void setGameOverMessage(String message) {
        this.winnerMessage = message;
        this.isGameOver    = true;
    }

    // -------------------------------------------------------------------------
    // Getters (tutti synchronized: letti dal thread Swing, scritti dall'updateLoop)
    // -------------------------------------------------------------------------

    public synchronized ArrayList<BallViewInfo> getBalls() {
        return new ArrayList<>(balls);
    }

    public synchronized BallViewInfo getPlayer1()   { return player1; }
    public synchronized BallViewInfo getPlayer2()   { return player2; }
    public synchronized int getScore1()             { return score1; }
    public synchronized int getScore2()             { return score2; }
    public synchronized int getFramePerSec()        { return framePerSec; }
    public synchronized boolean isGameOver()        { return isGameOver; }
    public synchronized String getWinnerMessage()   { return winnerMessage; }
}
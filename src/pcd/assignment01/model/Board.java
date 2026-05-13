package pcd.assignment01.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import pcd.assignment01.util.*;

public class Board {

    private List<Ball> balls;
    private Ball player1; // Giocatore H (sinistra)
    private Ball player2; // Giocatore B (destra)
    private Boundary bounds;

    private int score1;
    private int score2;

    // Variabili per lo stato della partita
    private boolean isGameOver;
    private String winnerMessage;

    /*
    * lastTouchedBy è una Map<Ball, Integer> dove la chiave è la pallina
    * e il valore è un intero che identifica l'ultimo giocatore che l'ha toccata
    * (0 = nessuno, 1 = player1, 2 = player2)
    *
    * player1 tocca pallina  → lastTouchedBy[b] = 1
    * player2 tocca pallina  → lastTouchedBy[b] = 2
    * pallina tocca pallina  → lastTouchedBy[b1] = 0, lastTouchedBy[b2] = 0
    * pallina entra buca1 → lastTouchedBy[b] == 1 ? score1++ : niente
    * pallina entra buca2 → lastTouchedBy[b] == 2 ? score2++
    * */
    private ConcurrentHashMap<Ball, Integer> lastTouchedBy;
    private static final double HOLE_RADIUS = 0.25;

    public Board(){
        score1 = 0;
        score2 = 0;
        isGameOver = false;
        winnerMessage = "";
        lastTouchedBy = new ConcurrentHashMap<>();
    }

    public void init(BoardConf conf) {
        balls = new ArrayList<>(conf.getSmallBalls());
        player1 = conf.getPlayer1();
        player2 = conf.getPlayer2();
        bounds = conf.getBoardBoundary();
    }

    private void checkHoles() {
        P2d hole1Pos = new P2d(bounds.x0(), bounds.y1());
        P2d hole2Pos = new P2d(bounds.x1(), bounds.y1());

        // Controllo se uno dei giocatori è caduto in una buca (in QUALSIASI buca)
        if (player1 != null) {
            double p1d1 = Math.hypot(player1.getPos().x() - hole1Pos.x(), player1.getPos().y() - hole1Pos.y());
            double p1d2 = Math.hypot(player1.getPos().x() - hole2Pos.x(), player1.getPos().y() - hole2Pos.y());
            if (p1d1 < HOLE_RADIUS || p1d2 < HOLE_RADIUS) {
                isGameOver = true;
                winnerMessage = "Vittoria Giocatore B! (Giocatore H è caduto in buca)";
                player1 = null; // Rimuove la palla H dal campo
                return; // Ferma gli altri controlli
            }
        }

        if (player2 != null) {
            double p2d1 = Math.hypot(player2.getPos().x() - hole1Pos.x(), player2.getPos().y() - hole1Pos.y());
            double p2d2 = Math.hypot(player2.getPos().x() - hole2Pos.x(), player2.getPos().y() - hole2Pos.y());
            if (p2d1 < HOLE_RADIUS || p2d2 < HOLE_RADIUS) {
                isGameOver = true;
                winnerMessage = "Vittoria Giocatore H! (Giocatore B è caduto in buca)";
                player2 = null; // Rimuove la palla B dal campo
                return;
            }
        }

        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            Ball b = it.next();
            P2d p = b.getPos();

            double dist1 = Math.hypot(p.x() - hole1Pos.x(), p.y() - hole1Pos.y());
            if (dist1 < HOLE_RADIUS) {
                int lastTouch = lastTouchedBy.getOrDefault(b, 0);
                if (lastTouch == 1) incrementScore1();
                it.remove();
                lastTouchedBy.remove(b);
                continue;
            }

            double dist2 = Math.hypot(p.x() - hole2Pos.x(), p.y() - hole2Pos.y());
            if (dist2 < HOLE_RADIUS) {
                int lastTouch = lastTouchedBy.getOrDefault(b, 0);
                if (lastTouch == 2) incrementScore2();
                it.remove();
                lastTouchedBy.remove(b);
            }
        }
    }

    /**
     * Metodo pubblico per controllare buche e fine partita.
     * Usato da GameLoop dopo la parallelizzazione della collisione player-palle.
     */
    public void checkHolesAndEndGame() {
        // FASE B: Collisione player-player (sequenziale, operazione singola)
        if (player1 != null && player2 != null) {
            Ball.resolveCollision(player1, player2);
        }
        
        // Controllo buche e rimozione palle (sequenziale)
        checkHoles();
        
        // Verifica fine partita (sequenziale)
        checkEndGame();
    }

    private void checkEndGame() {
        if (balls.isEmpty() && !isGameOver) {
            isGameOver = true;
            if (score1 > score2) {
                winnerMessage = "Vittoria Giocatore H (Punti: " + score1 + " a " + score2 + ")";
            } else if (score2 > score1) {
                winnerMessage = "Vittoria Giocatore B (Punti: " + score2 + " a " + score1 + ")";
            } else {
                winnerMessage = "Pareggio! (" + score1 + " a " + score2 + ")";
            }
        }
    }

    public List<Ball> getBalls(){
        return Collections.unmodifiableList(balls);
    }
    public Ball getPlayer1() { return player1; }
    public Ball getPlayer2() { return player2; }
    public int getScore1() { return score1; }
    public int getScore2() { return score2; }
    public Boundary getBounds(){ return bounds; }
    public void incrementScore1() { this.score1++; }
    public void incrementScore2() { this.score2++; }

    // Getter per lo stato della partita
    public boolean isGameOver() { return isGameOver; }
    public String getWinnerMessage() { return winnerMessage; }
    public ConcurrentHashMap<Ball, Integer> getLastTouchedBy() {
        return lastTouchedBy;
    }
}
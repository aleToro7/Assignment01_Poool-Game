package pcd.assignment01.model;

import java.util.*;
import pcd.assignment01.util.*;

public class Board {

    public enum Winner { PLAYER1, PLAYER2, DRAW, NONE }

    private List<Ball> balls;
    private Ball player1; // Giocatore H (sinistra)
    private Ball player2; // Giocatore B (destra)
    private Boundary bounds;

    private int score1;
    private int score2;

    private boolean isGameOver;

    /*
     * lastTouchedBy è una Map<Ball, Integer> dove la chiave è la pallina
     * e il valore è un intero che identifica l'ultimo giocatore che l'ha toccata
     * (0 = nessuno, 1 = player1, 2 = player2)
     *
     * player1 tocca pallina  → lastTouchedBy[b] = 1
     * player2 tocca pallina  → lastTouchedBy[b] = 2
     * pallina tocca pallina  → lastTouchedBy[b1] = 0, lastTouchedBy[b2] = 0
     * pallina entra buca1    → lastTouchedBy[b] == 1 ? score1++ : niente
     * pallina entra buca2    → lastTouchedBy[b] == 2 ? score2++ : niente
     */
    // ConcurrentHashMap: acceduta sia dai WorkerThread (resolveBallCollision)
    // sia dall'updateLoop (resolvePlayerCollisions, checkHoles) senza lock annidati.
    // Elimina il deadlock: synchronized(ball) → synchronized(board) non può
    // incrociarsi con synchronized(board) → accesso a ball.
    private Map<Ball, Integer> lastTouchedBy;

    private static final double HOLE_RADIUS = 0.25;
    private static final double STILL_THRESHOLD = 0.05;

    public Board() {
        score1 = 0;
        score2 = 0;
        isGameOver = false;
        lastTouchedBy = new java.util.concurrent.ConcurrentHashMap<>();
    }

    public synchronized void init(BoardConf conf) {
        balls = new ArrayList<>(conf.getSmallBalls());
        player1 = conf.getPlayer1();
        player2 = conf.getPlayer2();
        bounds = conf.getBoardBoundary();
    }

    /**
     * Aggiorna posizioni di player1 e player2.
     * Chiamato dal GameController prima di avviare i worker (sequenziale).
     */
    public synchronized void updatePlayers(long dt) {
        if (isGameOver) return;
        if (player1 != null) player1.updateState(dt, this);
        if (player2 != null) player2.updateState(dt, this);
    }

    /**
     * Chiamato dai WorkerThread in fase 2 (sotto lock su entrambe le Ball).
     * Risolve la collisione fisica e resetta lastTouchedBy per entrambe:
     * se la velocità cambia significa che c'è stato un impatto reale,
     * e nessun giocatore può reclamare il punto su una pallina rimbalzata.
     */
    public void resolveBallCollision(Ball a, Ball b) {
        V2d velA = a.getVel();
        V2d velB = b.getVel();
        Ball.resolveCollision(a, b);
        // ConcurrentHashMap: thread-safe senza synchronized(this) annidato.
        // Eliminato il rischio deadlock: worker tiene lock(a)+lock(b) e
        // non deve più acquisire lock(board) per aggiornare lastTouchedBy.
        if (!a.getVel().equals(velA)) lastTouchedBy.remove(a);
        if (!b.getVel().equals(velB)) lastTouchedBy.remove(b);
    }

    /**
     * collisioni player vs palline e player vs player.
     * Aggiorna lastTouchedBy per il sistema di punteggio.
     */
    public synchronized void resolvePlayerCollisions() {
        if (isGameOver) return;
        for (var b : balls) {
            if (player1 != null) {
                V2d vPrima = b.getVel();
                Ball.resolveCollision(player1, b);
                if (!b.getVel().equals(vPrima)) lastTouchedBy.put(b, 1);
            }
            if (player2 != null) {
                V2d vPrima = b.getVel();
                Ball.resolveCollision(player2, b);
                if (!b.getVel().equals(vPrima)) lastTouchedBy.put(b, 2);
            }
        }
        if (player1 != null && player2 != null) {
            Ball.resolveCollision(player1, player2);
        }
    }

    /**
     * Fase sequenziale finale: controlla buche e aggiorna stato game over.
     * Chiamato dal GameController dopo resolvePlayerCollisions().
     */
    public synchronized void checkHolesAndGameOver() {
        if (isGameOver) return;
        checkHoles();
        if (balls.isEmpty()) {
            isGameOver = true;
        }
    }

    private synchronized void checkHoles() {
        P2d hole1Pos = new P2d(bounds.x0(), bounds.y1());
        P2d hole2Pos = new P2d(bounds.x1(), bounds.y1());

        if (player1 != null) {
            double p1d1 = Math.hypot(player1.getPos().x() - hole1Pos.x(), player1.getPos().y() - hole1Pos.y());
            double p1d2 = Math.hypot(player1.getPos().x() - hole2Pos.x(), player1.getPos().y() - hole2Pos.y());
            if (p1d1 < HOLE_RADIUS || p1d2 < HOLE_RADIUS) {
                isGameOver = true;
                player1 = null;
                return;
            }
        }

        if (player2 != null) {
            double p2d1 = Math.hypot(player2.getPos().x() - hole1Pos.x(), player2.getPos().y() - hole1Pos.y());
            double p2d2 = Math.hypot(player2.getPos().x() - hole2Pos.x(), player2.getPos().y() - hole2Pos.y());
            if (p2d1 < HOLE_RADIUS || p2d2 < HOLE_RADIUS) {
                isGameOver = true;
                player2 = null;
                return;
            }
        }

        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            Ball b = it.next();
            P2d p = b.getPos();
            double dist1 = Math.hypot(p.x() - hole1Pos.x(), p.y() - hole1Pos.y());
            double dist2 = Math.hypot(p.x() - hole2Pos.x(), p.y() - hole2Pos.y());
            if (dist1 < HOLE_RADIUS || dist2 < HOLE_RADIUS) {
                int lastTouch = lastTouchedBy.getOrDefault(b, 0);
                if (lastTouch == 1) incrementScore1();
                else if (lastTouch == 2) incrementScore2();
                it.remove();
                lastTouchedBy.remove(b);
            }
        }
    }

    // --- Query di dominio ---

    /**
     * Restituisce chi ha vinto la partita.
     * NONE se la partita è ancora in corso.
     * Quando un giocatore cade in buca: player1==null → PLAYER2 vince, e viceversa.
     */
    public synchronized Winner getWinner() {
        if (!isGameOver) return Winner.NONE;
        if (player1 == null) return Winner.PLAYER2;
        if (player2 == null) return Winner.PLAYER1;
        if (score1 > score2) return Winner.PLAYER1;
        if (score2 > score1) return Winner.PLAYER2;
        return Winner.DRAW;
    }

    /**
     * True se player2 è fermo (utile al BotController per sapere quando agire).
     */
    public synchronized boolean isPlayer2Still() {
        return player2 != null && player2.getVel().abs() < STILL_THRESHOLD;
    }

    // --- Getters ---

    public synchronized List<Ball> getBalls()    { return new ArrayList<>(balls); }
    public synchronized Ball getPlayer1()        { return player1; }
    public synchronized Ball getPlayer2()        { return player2; }
    public synchronized int getScore1()          { return score1; }
    public synchronized int getScore2()          { return score2; }
    public synchronized Boundary getBounds()     { return bounds; }
    public synchronized boolean isGameOver()     { return isGameOver; }

    public synchronized Map<Ball, Integer> getLastTouchedBy() {
        return Collections.unmodifiableMap(lastTouchedBy);
    }

    // --- Azioni ---

    private synchronized void incrementScore1() { this.score1++; }
    private synchronized void incrementScore2() { this.score2++; }

    public synchronized void kickPlayer1(V2d impulse) {
        if (player1 != null) player1.kick(impulse);
    }

    public synchronized void kickPlayer2(V2d impulse) {
        if (player2 != null) player2.kick(impulse);
    }
}
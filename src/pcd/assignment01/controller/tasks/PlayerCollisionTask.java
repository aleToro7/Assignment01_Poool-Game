package pcd.assignment01.controller.tasks;

import pcd.assignment01.model.Ball;
import pcd.assignment01.model.V2d;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task parallelo per calcolare le collisioni tra palle e giocatori.
 * Ogni task elabora un range di palle, verificando collisioni con player1 e player2.
 * 
 * Thread-safety:
 * - Ogni task legge/scrive palle diverse (range [from, to)), nessuna race su quelle
 * - player1 e player2 sono invece condivisi da TUTTI i task: l'accesso a ciascuno
 *   è protetto da synchronized(player) per evitare race condition sulle loro
 *   posizioni/velocità (mai entrambi i lock insieme -> nessun rischio di deadlock)
 * - lastTouchedBy è ConcurrentHashMap (thread-safe)
 */
public class PlayerCollisionTask implements Callable<Void> {

    private final List<Ball> balls;
    private final int from;
    private final int to;
    private final Ball player1;
    private final Ball player2;
    private final ConcurrentHashMap<Ball, Integer> lastTouchedBy;

    public PlayerCollisionTask(List<Ball> balls, int from, int to,
                               Ball player1, Ball player2,
                               ConcurrentHashMap<Ball, Integer> lastTouchedBy) {
        this.balls = balls;
        this.from = from;
        this.to = to;
        this.player1 = player1;
        this.player2 = player2;
        this.lastTouchedBy = lastTouchedBy;
    }

    @Override
    public Void call() {
        for (int i = from; i < to; i++) {
            Ball b = balls.get(i);

            // Collisione con player1 - player1 è condiviso da tutti i task,
            // il lock serializza gli accessi concorrenti alla sua pos/vel
            if (player1 != null) {
                synchronized (player1) {
                    V2d vPrima = b.getVel();
                    Ball.resolveCollision(player1, b);
                    if (!b.getVel().equals(vPrima)) {
                        lastTouchedBy.put(b, 1);
                    }
                }
            }

            // Collisione con player2 - stesso discorso di player1
            if (player2 != null) {
                synchronized (player2) {
                    V2d vPrima = b.getVel();
                    Ball.resolveCollision(player2, b);
                    if (!b.getVel().equals(vPrima)) {
                        lastTouchedBy.put(b, 2);
                    }
                }
            }
        }
        return null;
    }
}

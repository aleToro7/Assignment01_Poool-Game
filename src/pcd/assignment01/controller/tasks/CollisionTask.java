package pcd.assignment01.controller.tasks;

import pcd.assignment01.model.Ball;
import pcd.assignment01.model.V2d;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Il carico del confronto i vs j (j > i) è triangolare: la pallina i=0 va
 * confrontata con quasi tutte le altre, la pallina i=n-1 con nessuna.
 * Per bilanciare il lavoro tra i task, ogni task non riceve un range
 * contiguo di indici ma un sottoinsieme "a passo" (start, start+step,
 * start+2*step, ...): così ogni task riceve un mix di indici bassi (molto
 * lavoro) e alti (poco lavoro), pareggiando il totale tra i worker.
 */
public class CollisionTask implements Callable<Void> {

    private final List<Ball> balls;
    private final int start;
    private final int step;
    private final ConcurrentHashMap<Ball, Integer> lastTouchedBy;

    public CollisionTask(List<Ball> balls, int start, int step,
                         ConcurrentHashMap<Ball, Integer> lastTouchedBy) {
        this.balls         = balls;
        this.start         = start;
        this.step          = step;
        this.lastTouchedBy = lastTouchedBy;
    }

    @Override
    public Void call() {
        int n = balls.size();
        for (int i = start; i < n; i += step) {
            for (int j = i + 1; j < n; j++) {
                Ball a = balls.get(i);
                Ball b = balls.get(j);

                Ball first  = System.identityHashCode(a) <= System.identityHashCode(b) ? a : b;
                Ball second = (first == a) ? b : a;

                synchronized (first) {
                    synchronized (second) {
                        V2d vAPrima = a.getVel();
                        V2d vBPrima = b.getVel();
                        Ball.resolveCollision(a, b);
                        // Reset last touch se c'è stata una collisione reale
                        if (!a.getVel().equals(vAPrima) || !b.getVel().equals(vBPrima)) {
                            lastTouchedBy.put(a, 0);
                            lastTouchedBy.put(b, 0);
                        }
                    }
                }
            }
        }
        return null;
    }
}
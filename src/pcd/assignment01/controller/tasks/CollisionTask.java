package pcd.assignment01.controller.tasks;

import pcd.assignment01.model.Ball;
import pcd.assignment01.model.V2d;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class CollisionTask implements Callable<Void> {

    private final List<Ball> balls;
    private final int from;
    private final int to;
    private final ConcurrentHashMap<Ball, Integer> lastTouchedBy;

    public CollisionTask(List<Ball> balls, int from, int to,
                         ConcurrentHashMap<Ball, Integer> lastTouchedBy) {
        this.balls         = balls;
        this.from          = from;
        this.to            = to;
        this.lastTouchedBy = lastTouchedBy;
    }

    @Override
    public Void call() {
        int n = balls.size();
        for (int i = from; i < to; i++) {
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
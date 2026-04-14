package pcd.assignment01.concurrent.tasks;

import pcd.assignment01.model.Ball;
import java.util.List;
import java.util.concurrent.Callable;

public class CollisionTask implements Callable<Void> {

    private final List<Ball> balls;
    private final int from;
    private final int to;

    public CollisionTask(List<Ball> balls, int from, int to) {
        this.balls = balls;
        this.from  = from;
        this.to    = to;
    }

    @Override
    public Void call() {
        int n = balls.size();
        for (int i = from; i < to; i++) {
            for (int j = i + 1; j < n; j++) {
                Ball a = balls.get(i);
                Ball b = balls.get(j);

                // Ordine canonico dei lock basato sull'identità dell'oggetto
                // per evitare deadlock tra task che accedono alle stesse coppie
                Ball first  = System.identityHashCode(a) <= System.identityHashCode(b) ? a : b;
                Ball second = first == a ? b : a;

                synchronized (first) {
                    synchronized (second) {
                        Ball.resolveCollision(a, b);
                    }
                }
            }
        }
        return null;
    }
}
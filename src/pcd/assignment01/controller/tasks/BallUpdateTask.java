package pcd.assignment01.controller.tasks;

import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Board;
import java.util.List;
import java.util.concurrent.Callable;

public class BallUpdateTask implements Callable<Void> {

    private final List<Ball> balls;
    private final int from;
    private final int to;
    private final long dt;
    private final Board board;

    public BallUpdateTask(List<Ball> balls, int from, int to, long dt, Board board) {
        this.balls = balls;
        this.from  = from;
        this.to    = to;
        this.dt    = dt;
        this.board = board;
    }

    @Override
    public Void call() {
        for (int i = from; i < to; i++) {
            balls.get(i).updateState(dt, board);
        }
        return null;
    }
}

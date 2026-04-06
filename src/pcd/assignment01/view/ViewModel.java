package pcd.assignment01.view;

import pcd.assignment01.model.Board;
import pcd.assignment01.model.P2d;
import java.util.ArrayList;

record BallViewInfo(P2d pos, double radius) {}

public class ViewModel {

	private ArrayList<BallViewInfo> balls;
	private BallViewInfo player1; // Giocatore di sinistra (H)
	private BallViewInfo player2; // Giocatore di destra (B)
	private int score1; // Punteggio giocatore di sinistra
	private int score2; // Punteggio giocatore di destra
	private int framePerSec;

	public ViewModel() {
		balls = new ArrayList<BallViewInfo>();
		framePerSec = 0;
		score1 = 0;
		score2 = 0;
	}

	public synchronized void update(Board board, int framePerSec) {
		balls.clear();
		for (var b: board.getBalls()) {
			balls.add(new BallViewInfo(b.getPos(), b.getRadius()));
		}
		this.framePerSec = framePerSec;

		// Aggiorniamo i due giocatori
		var p1 = board.getPlayer1();
		var p2 = board.getPlayer2();

		if (p1 != null) player1 = new BallViewInfo(p1.getPos(), p1.getRadius());
		if (p2 != null) player2 = new BallViewInfo(p2.getPos(), p2.getRadius());

		// Aggiorniamo i punteggi
		this.score1 = board.getScore1();
		this.score2 = board.getScore2();
	}

	public synchronized ArrayList<BallViewInfo> getBalls(){
		var copy = new ArrayList<BallViewInfo>();
		copy.addAll(balls);
		return copy;
	}

	public synchronized int getFramePerSec() {
		return framePerSec;
	}

	public synchronized BallViewInfo getPlayer1() { return player1; }
	public synchronized BallViewInfo getPlayer2() { return player2; }
	public synchronized int getScore1() { return score1; }
	public synchronized int getScore2() { return score2; }
}
package pcd.assignment01.util;

import java.util.List;
import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Boundary;

public interface BoardConf {

	Boundary getBoardBoundary();

	// Sostituiamo getPlayerBall() con i due giocatori
	Ball getPlayer1();
	Ball getPlayer2();

	List<Ball> getSmallBalls();
}
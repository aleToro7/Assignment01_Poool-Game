package pcd.assignment01.util;

import java.util.List;
import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Boundary;

public interface BoardConf {

	Boundary getBoardBoundary();
	
	Ball getPlayerBall();
	
	List<Ball> getSmallBalls();
}

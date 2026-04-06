package pcd.assignment01;

import pcd.assignment01.model.Board;
import pcd.assignment01.model.V2d;
import pcd.assignment01.util.MinimalBoardConf;
import pcd.assignment01.util.LargeBoardConf;
// import pcd.assignment01.util.MassiveBoardConf;
import pcd.assignment01.view.View;
import pcd.assignment01.view.ViewModel;

import java.util.Random;

public class Sketch01 {
	public static void main(String[] argv) {

		/* * Different board configs to try:
		 * - minimal: 2 small balls
		 * - large: 400 small balls
		 * - massive: 4500 small balls
		 */

		var boardConf = new LargeBoardConf();

		Board board = new Board();
		board.init(boardConf);

		ViewModel viewModel = new ViewModel();
		View view = new View(viewModel, 1200, 800);

		viewModel.update(board, 0);
		view.render();
		waitAbit();

		int nFrames = 0;
		long t0 = System.currentTimeMillis();
		long lastUpdateTime = System.currentTimeMillis();

		var p1 = board.getPlayer1();
		var p2 = board.getPlayer2();

		var rand = new Random(2);

		// Variabili per tenere traccia dell'ultimo "calcio" di ogni giocatore
		long lastKickTimeP1 = t0;
		long lastKickTimeP2 = t0;

		/* main simulation loop */

		while (true){

			/* Se la palla del giocatore 1 è ferma e sono passati 2 secondi, diamo un calcio */
			if (p1 != null && p1.getVel().abs() < 0.05 && System.currentTimeMillis() - lastKickTimeP1 > 2000) {
				var angle = rand.nextDouble()*Math.PI*0.25; // Angolo verso l'alto a destra
				var v = new V2d(Math.cos(angle),Math.sin(angle)).mul(1.5);
				p1.kick(v);
				lastKickTimeP1 = System.currentTimeMillis();
			}

			/* Facciamo la stessa cosa per il giocatore 2 */
			if (p2 != null && p2.getVel().abs() < 0.05 && System.currentTimeMillis() - lastKickTimeP2 > 2000) {
				// Facciamo tirare il giocatore 2 in una direzione leggermente diversa
				var angle = rand.nextDouble()*Math.PI*0.25 + Math.PI*0.75;
				var v = new V2d(Math.cos(angle),Math.sin(angle)).mul(1.5);
				p2.kick(v);
				lastKickTimeP2 = System.currentTimeMillis();
			}

			/* update board state */

			long elapsed = System.currentTimeMillis() - lastUpdateTime;
			lastUpdateTime = System.currentTimeMillis();
			board.updateState(elapsed);

			/* render */

			nFrames++;
			int framePerSec = 0;
			long dt = (System.currentTimeMillis() - t0);
			if (dt > 0) {
				framePerSec = (int)(nFrames*1000/dt);
			}

			viewModel.update(board, framePerSec);
			view.render();

		}
	}

	private static void waitAbit() {
		try {
			Thread.sleep(2000);
		} catch (Exception ex) {}
	}

}
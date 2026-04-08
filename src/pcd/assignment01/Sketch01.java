package pcd.assignment01;

import pcd.assignment01.model.Board;
import pcd.assignment01.model.V2d;
//import pcd.assignment01.util.MinimalBoardConf;
import pcd.assignment01.util.LargeBoardConf;
// import pcd.assignment01.util.MassiveBoardConf;
import pcd.assignment01.view.View;
import pcd.assignment01.view.ViewModel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;

public class Sketch01 {
	public static void main(String[] argv) {

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

		// Ci serve solo il timer del bot (Giocatore 2)
		long lastKickTimeP2 = t0;

		// Controlli da tastiera per il Giocatore (H) --> quello controllato da noi
		view.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (p1 != null) {
					double speed = 1.5; // potenza tiro

					// Controlliamo quale freccia è stata premuta
					switch (e.getKeyCode()) {
						case KeyEvent.VK_UP:
							p1.kick(new V2d(0, speed)); // Impulso verso l'alto
							break;
						case KeyEvent.VK_DOWN:
							p1.kick(new V2d(0, -speed)); // Impulso verso il basso
							break;
						case KeyEvent.VK_LEFT:
							p1.kick(new V2d(-speed, 0)); // Impulso a sinistra
							break;
						case KeyEvent.VK_RIGHT:
							p1.kick(new V2d(speed, 0)); // Impulso a destra
							break;
					}
				}
			}
		});

		/* main simulation loop */

		while (true){

			/* Il Giocatore 1 (H) è controllato.
			 * Manteniamo il timer automatico per far muovere il Giocatore 2 / Bot (B) */
			if (p2 != null && p2.getVel().abs() < 0.05 && System.currentTimeMillis() - lastKickTimeP2 > 2000) {
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
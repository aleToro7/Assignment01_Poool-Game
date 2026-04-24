package pcd.assignment01;

import pcd.assignment01.controller.GameController;
import pcd.assignment01.model.Board;
import pcd.assignment01.model.V2d;
//import pcd.assignment01.util.MinimalBoardConf;
import pcd.assignment01.util.LargeBoardConf;
// import pcd.assignment01.util.MassiveBoardConf;
import pcd.assignment01.view.View;
import pcd.assignment01.view.ViewModel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.SwingUtilities;

public class Sketch01 {
	public static void main(String[] argv) {

		var boardConf = new LargeBoardConf();

		Board board = new Board();
		board.init(boardConf);

		ViewModel viewModel = new ViewModel();

		SwingUtilities.invokeLater(() -> {
			View view = new View(viewModel, 1200, 800);
			viewModel.update(board, 0);
			view.render();

			// Controlli da tastiera per il Giocatore (H) --> quello controllato da noi
			view.addKeyListener(new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					double speed = 1.5; // potenza tiro

					// Controlliamo quale freccia è stata premuta
					switch (e.getKeyCode()) {
						case KeyEvent.VK_UP:
							board.kickPlayer1(new V2d(0, speed)); // Impulso verso l'alto
							break;
						case KeyEvent.VK_DOWN:
							board.kickPlayer1(new V2d(0, -speed)); // Impulso verso il basso
							break;
						case KeyEvent.VK_LEFT:
							board.kickPlayer1(new V2d(-speed, 0)); // Impulso a sinistra
							break;
						case KeyEvent.VK_RIGHT:
							board.kickPlayer1(new V2d(speed, 0)); // Impulso a destra
							break;
					}
				}
			});

			// Create and start the game controller
			GameController gameController = new GameController(board, viewModel, view);
			gameController.startGame();
		});
	}
}
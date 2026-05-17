package pcd.assignment01;
 
import pcd.assignment01.controller.GameController;
import pcd.assignment01.controller.InputHandler;
import pcd.assignment01.model.Board;
import pcd.assignment01.util.LargeBoardConf;
// import pcd.assignment01.util.MinimalBoardConf;
// import pcd.assignment01.util.MassiveBoardConf;
import pcd.assignment01.view.View;
import pcd.assignment01.view.ViewModel;
 
import javax.swing.SwingUtilities;

public class Main {
	public static void main(String[] argv) {
 
        // Model
        var board = new Board();
        board.init(new LargeBoardConf());
 
        // View
        var viewModel = new ViewModel();
 
        SwingUtilities.invokeLater(() -> {
            var view = new View(viewModel, 1200, 800);
            viewModel.update(board, 0);
            view.render();
 
            // Controller
            int nWorkers        = Runtime.getRuntime().availableProcessors() +  1;
            var inputHandler    = new InputHandler(board);
            var gameController  = new GameController(board, viewModel, view, nWorkers);
 
            inputHandler.attach(view);
            gameController.startGame();
        });
    }
}
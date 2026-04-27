package pcd.assignment01.controller;

import pcd.assignment01.model.Board;
import pcd.assignment01.util.MinimalBoardConf;
import pcd.assignment01.view.ViewModel;

/**
 * Main di test per JPF (Java PathFinder).
 * Disabilita la View per evitare crash con AWT/Swing non supportate da JPF.
 */
public class MainJPF {
    public static void main(String[] args) {
        // Configurazioni minime per evitare lo State Space Explosion
        var boardConf = new MinimalBoardConf(); 
        
        Board board = new Board();
        board.init(boardConf);
        
        // Il ViewModel funge da monitor sincronizzato tra thread
        ViewModel viewModel = new ViewModel();
        
        // Passiamo il Board (non boardConf), null alla View: il GameLoop non deve chiamare render()
        GameController loop = new GameController(board, viewModel, null);
        
        loop.startGame();

        // Facciamo girare la simulazione per un tempo controllato
        try {
            loop.getUpdateThread().join(); // Attende 5 secondi o fino a quando il thread termina
            loop.stopGame();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
package pcd.assignment01;

import pcd.assignment01.controller.GameController;
import pcd.assignment01.model.Board;
import pcd.assignment01.util.MinimalBoardConf;

public class MainJPF {

    public static void main(String[] argv) throws InterruptedException {

        Board board = new Board();
        board.init(new MinimalBoardConf()); // 2 palline, pochi stati

        int nWorkers = 2; // fisso e piccolo per JPF

        // null per view e viewModel: GameController deve gestirli senza crash
        GameController gc = new GameController(board, null, null, nWorkers);

        // Avvia i thread (worker + bot)
        gc.startWorkers();

        // Esegue un numero finito di tick invece del loop infinito.
        // 3 tick bastano per verificare le proprietà di sincronizzazione
        // (barrier, lock ordering, assenza di deadlock) senza far esplodere
        // lo stato space di JPF.
        for (int i = 0; i < 3; i++) {
            gc.doTick();
        }

        // Ferma tutti i thread in modo pulito
        gc.stopGame();
    }
}
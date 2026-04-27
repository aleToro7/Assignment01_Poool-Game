package pcd.assignment01.controller;

import pcd.assignment01.model.Board;
import pcd.assignment01.util.MinimalBoardConf;
import pcd.assignment01.view.ViewModel;

public class MainJPF {
    public static void main(String[] args) {
        // REGOLA N.1 DI JPF: USA SEMPRE LA CONFIGURAZIONE MINIMA!
        var boardConf = new MinimalBoardConf(); 
        
        Board board = new Board();
        board.init(boardConf);
        
        ViewModel viewModel = new ViewModel();
        
        // Passiamo 'null' al posto della View per disabilitare la grafica
        GameLoop loop = new GameLoop(boardConf, viewModel, null);
        
        loop.start();

        // Facciamo girare la simulazione solo per una frazione di secondo.
        // È sufficiente a JPF per testare migliaia di incroci tra i thread 
        // ed esplorare le collisioni.
        try {
            Thread.sleep(50);
            loop.stopLoop(); // Assicurati di avere questo metodo nel tuo GameLoop
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
package pcd.assignment01.concurrent;

import pcd.assignment01.model.V2d;
import pcd.assignment01.util.LargeBoardConf;
import pcd.assignment01.view.View;
import pcd.assignment01.view.ViewModel;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MainConcurrent {
    public static void main(String[] args) {

        var boardConf = new LargeBoardConf();
        var viewModel = new ViewModel();
        var view      = new View(viewModel, 1200, 800);

        var gameLoop  = new GameLoop(boardConf, viewModel, view);

        // La tastiera delega al GameLoop tramite la inputQueue thread-safe
        // NON si tocca direttamente la ball qui: thread-safety garantita
        view.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                double speed = 1.5;
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP    -> gameLoop.notifyInput(new V2d(0,  speed));
                    case KeyEvent.VK_DOWN  -> gameLoop.notifyInput(new V2d(0, -speed));
                    case KeyEvent.VK_LEFT  -> gameLoop.notifyInput(new V2d(-speed, 0));
                    case KeyEvent.VK_RIGHT -> gameLoop.notifyInput(new V2d( speed, 0));
                }
            }
        });

        gameLoop.start();
    }
}
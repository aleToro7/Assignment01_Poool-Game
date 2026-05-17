package pcd.assignment01.controller;

import pcd.assignment01.model.Board;
import pcd.assignment01.model.V2d;
import pcd.assignment01.view.View;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;


public class InputHandler {

    private static final double KICK_SPEED = 1.5;

    private final Board board;

    public InputHandler(Board board) {
        this.board = board;
    }

    /**
     * Registra il KeyListener sulla view.
     * Da chiamare sull'Event Dispatch Thread (dentro SwingUtilities.invokeLater).
     */
    public void attach(View view) {
        view.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (board.isGameOver()) return; // ignora input a partita finita

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP    -> board.kickPlayer1(new V2d(0,  KICK_SPEED));
                    case KeyEvent.VK_DOWN  -> board.kickPlayer1(new V2d(0, -KICK_SPEED));
                    case KeyEvent.VK_LEFT  -> board.kickPlayer1(new V2d(-KICK_SPEED, 0));
                    case KeyEvent.VK_RIGHT -> board.kickPlayer1(new V2d( KICK_SPEED, 0));
                }
            }
        });
    }
}
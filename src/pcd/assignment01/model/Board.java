package pcd.assignment01.model;

import java.util.*;
import pcd.assignment01.util.*;

public class Board {

    private List<Ball> balls;
    private Ball player1; // Giocatore H (sinistra)
    private Ball player2; // Giocatore B (destra)
    private Boundary bounds;

    // Aggiungiamo i punteggi
    private int score1;
    private int score2;

    public Board(){
        // Inizializziamo i punteggi a 0
        this.score1 = 0;
        this.score2 = 0;
    }

    public void init(BoardConf conf) {
        this.balls = conf.getSmallBalls();

        this.player1 = conf.getPlayer1();
        this.player2 = conf.getPlayer2();

        this.bounds = conf.getBoardBoundary();
    }

    public void updateState(long dt) {

        // Aggiorniamo lo stato di entrambi i giocatori
        if (player1 != null) player1.updateState(dt, this);
        if (player2 != null) player2.updateState(dt, this);

        for (var b: balls) {
            b.updateState(dt, this);
        }

        // Risoluzione collisioni tra le palline piccole
        for (int i = 0; i < balls.size() - 1; i++) {
            for (int j = i + 1; j < balls.size(); j++) {
                Ball.resolveCollision(balls.get(i), balls.get(j));
            }
        }

        // Risoluzione collisioni tra i giocatori e le palline piccole
        for (var b: balls) {
            if (player1 != null) Ball.resolveCollision(player1, b);
            if (player2 != null) Ball.resolveCollision(player2, b);
        }

        // Risoluzione collisione tra i due giocatori stessi
        if (player1 != null && player2 != null) {
            Ball.resolveCollision(player1, player2);
        }

        /* * TODO: LOGICA DELLE BUCHE E DEL PUNTEGGIO
         * Qui dentro in futuro dovremo aggiungere un controllo per verificare
         * se le palline piccole (b) sono finite dentro le buche agli angoli.
         * Se una pallina finisce nella buca di sinistra -> score1++; si rimuove la pallina
         * Se una pallina finisce nella buca di destra -> score2++; si rimuove la pallina
         */
    }

    public List<Ball> getBalls(){ return balls; }

    // Nuovi getter per i giocatori e i punteggi
    public Ball getPlayer1() { return player1; }
    public Ball getPlayer2() { return player2; }

    public int getScore1() { return score1; }
    public int getScore2() { return score2; }

    public Boundary getBounds(){ return bounds; }

    // Metodi di supporto per incrementare i punti
    public void incrementScore1() { this.score1++; }
    public void incrementScore2() { this.score2++; }
}
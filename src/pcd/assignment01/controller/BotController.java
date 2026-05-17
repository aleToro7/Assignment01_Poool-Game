package pcd.assignment01.controller;
 
import pcd.assignment01.model.Board;
import pcd.assignment01.model.V2d;
 
import java.util.Random;
 
/**
 * BotController gestisce il comportamento del bot (player2) in un thread dedicato.
 *
 * Ciclo di vita:
 *  - start()  → avvia il thread interno
 *  - stop()   → segnala terminazione e sveglia il thread se in wait
 *  - signal() → chiamato dall'updateLoop ogni tick per svegliare il bot
 *               se la pallina è ferma
 */
public class BotController {
 
    private static final long   KICK_COOLDOWN_MS = 2000;
    private static final double KICK_SPEED       = 1.5;
 
    private final Board  board;
    private final Object botMonitor = new Object();
 
    private volatile boolean running = false;
    private Thread botThread;
 
    public BotController(Board board) {
        this.board = board;
    }
 
    // -------------------------------------------------------------------------
    // Ciclo di vita
    // -------------------------------------------------------------------------
 
    public void start() {
        running   = true;
        botThread = new Thread(this::botLoop, "BotThread");
        botThread.setDaemon(true);
        botThread.start();
    }
 
    public void stop() {
        running = false;
        
        synchronized (botMonitor) {
            botMonitor.notifyAll();
        }
    }
 
    public void signal() {
        if (board.isPlayer2Still()) {
            synchronized (botMonitor) {
                botMonitor.notifyAll();
            }
        }
    }
 
    // -------------------------------------------------------------------------
    // Bot loop
    // -------------------------------------------------------------------------
 
    private void botLoop() {
        var  rand        = new Random(2);
        long lastKickTime = 0;
 
        while (running && !board.isGameOver()) {
 
            // Attendi che player2 sia fermo
            synchronized (botMonitor) {
                while (running && !board.isPlayer2Still() && !board.isGameOver()) {
                    try {
                        botMonitor.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
 
            if (!running || board.isGameOver()) return;
 
            long now = System.currentTimeMillis();
            if (now - lastKickTime >= KICK_COOLDOWN_MS) {
                double angle   = rand.nextDouble() * Math.PI * 0.25 + Math.PI * 0.75;
                V2d    impulse = new V2d(Math.cos(angle), Math.sin(angle)).mul(KICK_SPEED);
                board.kickPlayer2(impulse);
                lastKickTime = now;
            }
        }
    }
}
package pcd.assignment01.controller;
 
import pcd.assignment01.model.Board;
import pcd.assignment01.model.V2d;
 
/**
 * BotController gestisce il comportamento del bot (player2) in un thread dedicato.
 *
 * Ciclo di vita:
 *  - start()  → avvia il thread interno
 *  - stop()   → segnala terminazione e sveglia il thread se in wait
 *  - signal() → chiamato dall'updateLoop ogni tick per svegliare il bot
 *               se la pallina è ferma
 *
 * Il botMonitor vive qui: è un meccanismo di coordinamento tra thread,
 * non stato di dominio del gioco.
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
        signal(); // sveglia il thread se è bloccato in wait
    }
 
    /**
     * Chiamato dall'updateLoop ad ogni tick.
     * Sveglia il botLoop solo se player2 è fermo, evitando notifiche inutili.
     */
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
        // java.util.Random non è supportato da JPF 8 su Java 11+
        // (JPF_java_util_Random accede a jdk.internal.misc.Unsafe non esportato).
        // Usiamo un generatore LCG deterministico minimale che JPF sa modellare.
        long seed = 2L;
        long lastKickTime = 0;

        while (running && !board.isGameOver()) {

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
                // LCG: next = (a*seed + c) mod m  — costanti di Knuth
                seed = (seed * 6364136223846793005L + 1442695040888963407L);
                // Mappa seed in [0.0, 1.0)
                double randVal = (double)(seed & 0x7FFFFFFFFFFFFFFFL) / (double)Long.MAX_VALUE;
                double angle   = randVal * Math.PI * 0.25 + Math.PI * 0.75;
                V2d    impulse = new V2d(Math.cos(angle), Math.sin(angle)).mul(KICK_SPEED);
                board.kickPlayer2(impulse);
                lastKickTime = now;
            }
        }
    }
}
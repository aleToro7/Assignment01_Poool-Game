package pcd.assignment01.controller;

import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Board;
import pcd.assignment01.util.CyclicBarrier;

import java.util.List;

/**
 * Ad ogni tick esegue due fasi sulla propria partizione di palline:
 *
 *   Fase 1 — aggiorna le posizioni (updateState su ogni pallina)
 *             poi aspetta alla barrier che tutti i worker abbiano finito
 *
 *   Fase 2 — calcola le collisioni ball-ball nella propria partizione
 *             con lock per pallina in ordine canonico (evita deadlock)
 *             poi aspetta alla barrier che tutti i worker abbiano finito
 *
 * Dopo la fase 2, il GameController esegue in sequenza (thread singolo):
 *   - collisioni player1/player2 vs palline
 *   - checkHoles()
 */
public class WorkerThread extends Thread {

    private final Board         board;
    private final CyclicBarrier barrierPositions;   // fine fase 1
    private final CyclicBarrier barrierCollisions;  // fine fase 2

    private volatile List<Ball> partition;
    private volatile List<Ball> allBalls;
    private volatile int        partitionStart; // indice di partition.get(0) in allBalls
    private volatile long       dt;

    private final Object tickMonitor = new Object();
    private volatile boolean tickReady = false;
    private volatile boolean running   = true;

    public WorkerThread(int id, Board board,
                        CyclicBarrier barrierPositions,
                        CyclicBarrier barrierCollisions) {
        super("WorkerThread-" + id);
        setDaemon(true);
        this.board               = board;
        this.barrierPositions    = barrierPositions;
        this.barrierCollisions   = barrierCollisions;
    }

    public void startTick(List<Ball> partition, List<Ball> allBalls, int partitionStart, long dt) {
        synchronized (tickMonitor) {
            this.partition      = partition;
            this.allBalls       = allBalls;
            this.partitionStart = partitionStart;
            this.dt             = dt;
            this.tickReady      = true;
            tickMonitor.notifyAll();
        }
    }

    public void stopWorker() {
        running = false;
        synchronized (tickMonitor) {
            tickMonitor.notifyAll(); // sveglia se bloccato in wait
        }
    }

    // -------------------------------------------------------------------------
    // Loop principale
    // -------------------------------------------------------------------------

    @Override
    public void run() {
        while (running) {

            // Aspetta il segnale di inizio tick dal GameController
            synchronized (tickMonitor) {
                while (running && !tickReady) {
                    try { tickMonitor.wait(); }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                tickReady = false;
            }

            // FASE 1: aggiorna posizioni
            // Sicuro senza lock: le partizioni sono disgiunte, ogni worker
            // scrive solo sulle sue Ball e non legge quelle degli altri.
            for (var b : partition) {
                b.updateState(dt, board);
            }

            try { barrierPositions.await(); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // FASE 2: collisioni ball-ball
            // Ogni coppia (a, b) viene processata UNA SOLA VOLTA:
            // solo dal worker la cui partizione contiene 'a' con indice < indice di 'b'.
            // L'indice nella lista condivisa allBalls è l'ordine canonico:
            // è strettamente unico (a differenza di identityHashCode che può collidere),
            // quindi garantisce assenza di doppio processing E assenza di deadlock.
            for (int i = 0; i < partition.size(); i++) {
                Ball a    = partition.get(i);
                int  idxA = partitionStart + i; // O(1), nessuna ricerca lineare
                for (int j = idxA + 1; j < allBalls.size(); j++) {
                    Ball b = allBalls.get(j);
                    // Lock in ordine canonico: idxA < j per costruzione → no deadlock
                    synchronized (a) {
                        synchronized (b) {
                            board.resolveBallCollision(a, b);
                        }
                    }
                }
            }

            try { barrierCollisions.await(); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
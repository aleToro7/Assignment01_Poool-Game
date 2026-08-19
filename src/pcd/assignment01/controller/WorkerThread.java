package pcd.assignment01.controller;

import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Board;
import pcd.assignment01.util.CyclicBarrier;
import pcd.assignment01.util.ReusableLatch;
import pcd.assignment01.util.SpatialGrid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkerThread è un thread persistente (non viene mai ricreato).
 * Ad ogni tick esegue tre fasi sulla propria partizione di palline:
 *
 *   Fase 1 — aggiorna le posizioni (updateState su ogni pallina)
 *             poi aspetta alla barrierPositions (sincronizzazione SIMMETRICA:
 *             tutti i thread si aspettano a vicenda)
 *
 *   Fase G — solo Worker-0 (isGridBuilder==true): costruisce la SpatialGrid.
 *             Gli altri chiamano gridLatch.await() e si bloccano.
 *             Worker-0 chiama gridLatch.open() quando ha finito.
 *             Sincronizzazione ASIMMETRICA: un produttore sblocca N consumatori.
 *
 *   Fase 2 — ogni worker risolve le collisioni ball-ball usando la griglia,
 *             poi aspetta alla barrierCollisions (SIMMETRICA).
 */
public class WorkerThread extends Thread {

    private final Board         board;
    private final CyclicBarrier barrierPositions;
    private final ReusableLatch gridLatch;          // sostituisce barrierGrid
    private final CyclicBarrier barrierCollisions;
    private final SpatialGrid   grid;
    private final boolean       isGridBuilder;

    private volatile List<Ball>       partition;
    private volatile List<Ball>       allBalls;
    private volatile int              partitionStart;
    private volatile long             dt;
    // Mappa Ball → indice in allBalls per lookup O(1) durante la fase 2
    private volatile Map<Ball, Integer> ballIndex;

    private final Object tickMonitor = new Object();
    private volatile boolean tickReady = false;
    private volatile boolean running   = true;

    /**
     * @param id            indice del worker (0 = grid builder)
     * @param board         riferimento al model
     * @param barrierPositions barrier fine fase 1
     * @param barrierGrid      barrier fine fase G
     * @param barrierCollisions barrier fine fase 2
     * @param grid          griglia spaziale condivisa tra tutti i worker
     */
    public WorkerThread(int id, Board board,
                        CyclicBarrier barrierPositions,
                        ReusableLatch gridLatch,
                        CyclicBarrier barrierCollisions,
                        SpatialGrid grid) {
        super("WorkerThread-" + id);
        setDaemon(true);
        this.board              = board;
        this.barrierPositions   = barrierPositions;
        this.gridLatch          = gridLatch;
        this.barrierCollisions  = barrierCollisions;
        this.grid               = grid;
        this.isGridBuilder      = (id == 0);
    }

    // -------------------------------------------------------------------------
    // API chiamata dal GameController ogni tick
    // -------------------------------------------------------------------------

    public void startTick(List<Ball> partition, List<Ball> allBalls, int partitionStart, long dt) {
        synchronized (tickMonitor) {
            this.partition      = partition;
            this.allBalls       = allBalls;
            this.partitionStart = partitionStart;
            this.dt             = dt;
            // Costruisce la mappa indice una volta per tick: O(n) qui,
            // ma elimina indexOf O(n) per ogni coppia in fase 2.
            Map<Ball, Integer> idx = new HashMap<>(allBalls.size() * 2);
            for (int i = 0; i < allBalls.size(); i++) {
                idx.put(allBalls.get(i), i);
            }
            this.ballIndex = idx;
            this.tickReady = true;
            tickMonitor.notifyAll();
        }
    }

    public void stopWorker() {
        running = false;
        synchronized (tickMonitor) {
            tickMonitor.notifyAll();
        }
    }

    // -------------------------------------------------------------------------
    // Loop principale
    // -------------------------------------------------------------------------

    @Override
    public void run() {
        while (running) {

            // Aspetta il segnale di inizio tick
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

            // NON uscire qui: se abbiamo ricevuto il tick dobbiamo completare
            // tutte e tre le barrier, altrimenti il GameController si blocca.

            // ---- FASE 1: aggiorna posizioni ----
            // Sicuro senza lock: partizioni disgiunte.
            for (Ball b : partition) {
                b.updateState(dt, board);
            }

            try { barrierPositions.await(); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // ---- FASE G: costruzione griglia ----
            // Sincronizzazione ASIMMETRICA con ReusableLatch:
            // Worker-0 produce la griglia e chiama open(),
            // tutti gli altri chiamano await() e si bloccano finché
            // la griglia non è pronta. Semanticamente più corretto di
            // una CyclicBarrier, che implicherebbe lavoro parallelo.
            if (isGridBuilder) {
                grid.build(allBalls);
                gridLatch.open();   // sblocca tutti i consumatori
            } else {
                try { gridLatch.await(); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            // ---- FASE 2: collisioni ball-ball con SpatialGrid ----
            // Per ogni pallina della partizione, recupera i candidati dalle
            // 9 celle vicine invece di scorrere allBalls per intero.
            // Filtro con indice canonico: processa la coppia (a, candidate)
            // solo se idx(a) < idx(candidate), garantendo che ogni coppia
            // venga processata UNA SOLA VOLTA su tutti i worker.
            // Lock in ordine canonico (indice crescente) → no deadlock.
            for (int i = 0; i < partition.size(); i++) {
                Ball a    = partition.get(i);
                int  idxA = partitionStart + i;

                for (Ball b : grid.getCandidates(a)) {
                    if (b == a) continue;
                    Integer idxB = ballIndex.get(b);
                    if (idxB == null || idxB <= idxA) continue;

                    // Lock in ordine canonico per evitare deadlock
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
package pcd.assignment01.controller;

import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Board;
import pcd.assignment01.util.CyclicBarrier;
import pcd.assignment01.util.SpatialGrid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WorkerThread è un thread persistente (non viene mai ricreato).
 * Ad ogni tick esegue tre fasi sulla propria partizione di palline:
 *
 *   Fase 1 — aggiorna le posizioni (updateState su ogni pallina)
 *             poi aspetta alla barrierPositions
 *
 *   Fase G — solo Worker-0 (isGridBuilder==true): costruisce la SpatialGrid
 *             dalle posizioni aggiornate in fase 1.
 *             Gli altri worker aspettano alla barrierGrid senza fare nulla.
 *             Una volta che Worker-0 chiama barrierGrid.await(), tutti ripartono.
 *
 *   Fase 2 — ogni worker risolve le collisioni ball-ball usando la griglia:
 *             per ogni pallina della partizione recupera i candidati dalle
 *             9 celle vicine (O(1) per pallina invece di O(n)), filtrandosi
 *             con l'indice canonico per processare ogni coppia una sola volta.
 *             Lock in ordine canonico per evitare deadlock.
 *             poi aspetta alla barrierCollisions.
 */
public class WorkerThread extends Thread {

    private final Board         board;
    private final CyclicBarrier barrierPositions;
    private final CyclicBarrier barrierGrid;        // separa build griglia da fase 2
    private final CyclicBarrier barrierCollisions;
    private final SpatialGrid   grid;
    private final boolean       isGridBuilder;      // true solo per Worker-0

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
                        CyclicBarrier barrierGrid,
                        CyclicBarrier barrierCollisions,
                        SpatialGrid grid) {
        super("WorkerThread-" + id);
        setDaemon(true);
        this.board              = board;
        this.barrierPositions   = barrierPositions;
        this.barrierGrid        = barrierGrid;
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

            // ---- FASE G: costruzione griglia (solo Worker-0) ----
            // Worker-0 ricostruisce la griglia sulle posizioni appena aggiornate.
            // Gli altri worker chiamano barrierGrid.await() immediatamente.
            // Quando barrierGrid scatta, la griglia è consistente e leggibile
            // da tutti i worker in fase 2 senza ulteriore sincronizzazione
            // (la barrier stabilisce il happens-before necessario).
            if (isGridBuilder) {
                grid.build(allBalls);
            }

            try { barrierGrid.await(); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
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
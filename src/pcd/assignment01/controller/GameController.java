package pcd.assignment01.controller;

import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Board;
import pcd.assignment01.model.Board.Winner;
import pcd.assignment01.util.CyclicBarrier;
import pcd.assignment01.util.SpatialGrid;
import pcd.assignment01.view.View;
import pcd.assignment01.view.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * GameController gestisce il game loop principale.
 *
 * Multithreading:
 *  - N WorkerThread persistenti lavorano in parallelo ogni tick
 *  - Tre CyclicBarrier sincronizzano le fasi:
 *      posizioni → build griglia (Worker-0) → collisioni
 *  - Il GameController partecipa a tutte e tre le barrier come N+1-esimo party
 *
 * Flusso per tick:
 *   1. GameController divide balls in N partizioni e chiama startTick()
 *   2. barrierPositions: tutti finiscono fase 1 (posizioni)
 *   3. Worker-0 costruisce SpatialGrid; gli altri aspettano a barrierGrid
 *   4. barrierGrid: griglia pronta, tutti entrano in fase 2
 *   5. barrierCollisions: tutti finiscono fase 2 (collisioni con griglia)
 *   6. GameController esegue sequenzialmente: player collisions, checkHoles
 */
public class GameController {

    private static final int  TARGET_FPS = 60;
    private static final long FRAME_MS   = 1000 / TARGET_FPS;

    private final Board         board;
    private final ViewModel     viewModel;
    private final View          view;
    private final BotController botController;

    // Worker pool
    private final int               nWorkers;
    private final List<WorkerThread> workers;
    private final CyclicBarrier     barrierPositions;
    private final CyclicBarrier     barrierGrid;       // nuovo: separa build griglia
    private final CyclicBarrier     barrierCollisions;
    private final SpatialGrid       grid;

    private volatile boolean running = false;
    private Thread updateThread;

    // Cache partizioni: ricalcolate solo quando cambia il numero di palline
    private List<Ball>       cachedBalls;
    private List<List<Ball>> cachedPartitions;

    /**
     * @param nWorkers numero di WorkerThread (availableProcessors()+1 per convenzione).
     *                 Le CyclicBarrier vengono costruite con parties = nWorkers+1
     *                 per includere anche il GameController stesso.
     */
    public GameController(Board board, ViewModel viewModel, View view, int nWorkers) {
        this.board         = board;
        this.viewModel     = viewModel;
        this.view          = view;
        this.botController = new BotController(board);
        this.nWorkers      = nWorkers;

        // parties = nWorkers + 1 (GameController partecipa a tutte le barrier)
        this.barrierPositions  = new CyclicBarrier(nWorkers + 1);
        this.barrierGrid       = new CyclicBarrier(nWorkers + 1);
        this.barrierCollisions = new CyclicBarrier(nWorkers + 1);

        // La griglia viene costruita con i parametri di LargeBoardConf/MassiveBoardConf:
        // ballRadius = 0.01 per le palline piccole.
        // getBounds() è chiamato qui dopo board.init() — il campo è già inizializzato.
        this.grid = new SpatialGrid(board.getBounds(), 0.01);

        this.workers = new ArrayList<>(nWorkers);
        for (int i = 0; i < nWorkers; i++) {
            workers.add(new WorkerThread(i, board,
                    barrierPositions, barrierGrid, barrierCollisions, grid));
        }
    }

    // -------------------------------------------------------------------------
    // Ciclo di vita
    // -------------------------------------------------------------------------

    public void startGame() {
        running = true;
        workers.forEach(Thread::start);

        updateThread = new Thread(this::updateLoop, "UpdateThread");
        updateThread.setDaemon(true);
        updateThread.start();

        botController.start();
    }

    /**
     * Avvia solo i worker e il bot, senza l'updateLoop.
     * Usato da MainJPF: il loop viene sostituito da chiamate esplicite a doTick().
     */
    public void startWorkers() {
        running = true;
        workers.forEach(Thread::start);
        // BotController NON avviato: in modalità JPF il bot introduce
        // System.currentTimeMillis() e java.util.Random che JPF non modella
        // correttamente su Java 11+. Le proprietà rilevanti (barrier, lock)
        // sono verificabili senza il bot.
    }

    /**
     * Esegue un singolo tick della fisica in modo sincrono.
     * Usato da MainJPF per controllare il numero di tick esplorati da JPF.
     * Non deve essere chiamato insieme all'updateLoop.
     */
    public void doTick() {
        if (!board.isGameOver()) {
            doParallelUpdate(16); // dt fisso = 16ms (equivalente a 60fps)
        }
    }

    public void stopGame() {
        running = false;
        workers.forEach(WorkerThread::stopWorker);
        botController.stop();
        if (updateThread != null) updateThread.interrupt();
    }

    public Thread getUpdateThread() { return updateThread; }

    // -------------------------------------------------------------------------
    // Update loop
    // -------------------------------------------------------------------------

    private void updateLoop() {
        long lastTime = System.currentTimeMillis();
        long t0       = lastTime;
        int  nFrames  = 0;

        while (running) {
            long now     = System.currentTimeMillis();
            long elapsed = now - lastTime;
            lastTime     = now;

            if (!board.isGameOver()) {
                doParallelUpdate(elapsed);
            }

            // Fix 4: controlla game over subito dopo la fisica, prima del render.
            // checkHolesAndGameOver() può aver settato isGameOver=true in questo tick:
            // in quel caso handleGameOver() setta il messaggio e poi renderizziamo
            // una sola volta con lo stato finale corretto.
            if (board.isGameOver()) {
                handleGameOver();
                break;
            }

            botController.signal();

            nFrames++;
            long dt  = now - t0;
            int  fps = (dt > 0) ? (int)(nFrames * 1000 / dt) : 0;

            viewModel.update(board, fps);
            view.render();

            long sleepMs = FRAME_MS - (System.currentTimeMillis() - now);
            if (sleepMs > 0) {
                try { Thread.sleep(sleepMs); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        running = false;
    }

    // -------------------------------------------------------------------------
    // Update parallelo
    // -------------------------------------------------------------------------

    private void doParallelUpdate(long dt) {
        // Fix 3: snapshot atomico della lista palline PRIMA di qualsiasi operazione
        // parallela. getBalls() è synchronized su Board → nessuna race con checkHoles().
        // La lista snapshot è immutabile durante tutto il tick: i worker la leggono
        // in sola lettura per le collisioni cross-boundary.
        List<Ball> balls = board.getBalls();

        // Aggiorna player1 e player2 (sequenziale, solo 2 oggetti)
        board.updatePlayers(dt);

        // Fix 7: ricalcola le partizioni solo se il numero di palline è cambiato
        // (succede quando una pallina entra in buca). Evita riallocazione ogni tick.
        if (cachedBalls == null || cachedBalls.size() != balls.size()) {
            cachedPartitions = partition(balls, nWorkers);
            cachedBalls      = balls;
        } else {
            // Aggiorna i riferimenti alle Ball (la lista è nuova ad ogni getBalls())
            // mantenendo la stessa struttura di partizione
            cachedBalls = balls;
            int idx = 0;
            for (List<Ball> part : cachedPartitions) {
                int size = part.size();
                part.clear();
                part.addAll(balls.subList(idx, idx + size));
                idx += size;
            }
        }

        // Segnala inizio tick a tutti i worker, passando l'indice di partenza.
        // Se le partizioni sono meno dei worker (poche palline), i worker extra
        // ricevono una lista vuota: partecipano comunque alle barrier senza fare lavoro.
        int startIdx = 0;
        for (int i = 0; i < nWorkers; i++) {
            List<Ball> part = (i < cachedPartitions.size())
                    ? cachedPartitions.get(i)
                    : new ArrayList<>();
            workers.get(i).startTick(part, cachedBalls, startIdx, dt);
            startIdx += part.size();
        }

        // GameController partecipa alla barrier di fine fase 1 (posizioni)
        try { barrierPositions.await(); }
        catch (InterruptedException e) {
            // Se il GC viene interrotto, i worker sono già in await() sulla stessa barrier.
            // Dobbiamo svegliarli prima di uscire, altrimenti restano bloccati per sempre.
            workers.forEach(WorkerThread::stopWorker);
            Thread.currentThread().interrupt();
            return;
        }

        // GameController partecipa a barrierGrid come N+1-esimo party.
        // Worker-0 sta costruendo la griglia; il GameController non fa nulla
        // in questa fase ma deve chiamare await() per far scattare la barrier.
        try { barrierGrid.await(); }
        catch (InterruptedException e) {
            workers.forEach(WorkerThread::stopWorker);
            Thread.currentThread().interrupt();
            return;
        }

        // GameController partecipa alla barrier di fine fase 2 (collisioni ball-ball)
        try { barrierCollisions.await(); }
        catch (InterruptedException e) {
            workers.forEach(WorkerThread::stopWorker);
            Thread.currentThread().interrupt();
            return;
        }

        // Fase 3 sequenziale: collisioni player vs palline e player vs player
        board.resolvePlayerCollisions();

        // Fase 4 sequenziale: checkHoles + game over
        board.checkHolesAndGameOver();
    }

    // -------------------------------------------------------------------------
    // Utility: partiziona una lista in n sottoliste bilanciate
    // -------------------------------------------------------------------------

    private static <T> List<List<T>> partition(List<T> list, int n) {
        // Con poche palline (es. MinimalBoardConf) n potrebbe superare list.size():
        // limitiamo per evitare partizioni vuote che non aggiungono parallelismo.
        n = Math.min(n, Math.max(1, list.size()));

        List<List<T>> result = new ArrayList<>(n);
        int size  = list.size();
        int base  = size / n;
        int extra = size % n;
        int idx   = 0;

        for (int i = 0; i < n; i++) {
            int chunkSize = base + (i < extra ? 1 : 0);
            result.add(new ArrayList<>(list.subList(idx, idx + chunkSize)));
            idx += chunkSize;
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Fine partita
    // -------------------------------------------------------------------------

    private void handleGameOver() {
        running = false;
        workers.forEach(WorkerThread::stopWorker);
        botController.stop();
        viewModel.setGameOverMessage(buildWinnerMessage(board.getWinner()));
        view.render();
    }

    private String buildWinnerMessage(Winner winner) {
        int s1 = board.getScore1();
        int s2 = board.getScore2();
        if (winner == Winner.PLAYER1) {
            return String.format("Vittoria Giocatore H (Punti: %d a %d)", s1, s2);
        } else if (winner == Winner.PLAYER2) {
            return String.format("Vittoria Giocatore B (Punti: %d a %d)", s2, s1);
        } else if (winner == Winner.DRAW) {
            return String.format("Pareggio! (%d a %d)", s1, s2);
        } else {
            return "";
        }
    }
}
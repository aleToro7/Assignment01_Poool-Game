package pcd.assignment01.controller;

import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Board;
import pcd.assignment01.model.Board.Winner;
import pcd.assignment01.util.CyclicBarrier;
import pcd.assignment01.view.View;
import pcd.assignment01.view.ViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * GameController gestisce il game loop principale.
 *
 * Multithreading:
 *  - N WorkerThread persistenti (configurabili) lavorano in parallelo ogni tick
 *  - Due CyclicBarrier sincronizzano le fasi: posizioni → collisioni
 *  - Il GameController partecipa alle barrier come N+1-esimo "party"
 *    così può eseguire le operazioni sequenziali (player collisions, checkHoles)
 *    subito dopo la fase 2 senza ulteriore sincronizzazione
 *
 * Flusso per tick:
 *   1. GameController divide balls in N partizioni
 *   2. Chiama startTick() su ogni worker  → worker entrano in fase 1
 *   3. GameController chiama barrierPositions.await() insieme ai worker
 *   4. Worker entrano in fase 2 (collisioni con lock)
 *   5. GameController chiama barrierCollisions.await() insieme ai worker
 *   6. GameController esegue: player vs balls, player vs player, checkHoles
 */
public class GameController {

    private static final int  TARGET_FPS = 60;
    private static final long FRAME_MS   = 1000 / TARGET_FPS;

    private final Board         board;
    private final ViewModel     viewModel;
    private final View          view;
    private final BotController botController;

    // Worker pool
    private final int              nWorkers;
    private final List<WorkerThread> workers;
    private final CyclicBarrier    barrierPositions;
    private final CyclicBarrier    barrierCollisions;

    private volatile boolean running = false;
    private Thread updateThread;

    // Cache partizioni: ricalcolate solo quando cambia il numero di palline (fix 7)
    private List<Ball>       cachedBalls;
    private List<List<Ball>> cachedPartitions;

    /**
     * @param nWorkers numero di WorkerThread — tipicamente availableProcessors()
     */
    public GameController(Board board, ViewModel viewModel, View view, int nWorkers) {
        this.board         = board;
        this.viewModel     = viewModel;
        this.view          = view;
        this.botController = new BotController(board);
        this.nWorkers      = nWorkers;

        // +1 perché anche il GameController partecipa alle barrier
        this.barrierPositions  = new CyclicBarrier(nWorkers + 1);
        this.barrierCollisions = new CyclicBarrier(nWorkers + 1);

        this.workers = new ArrayList<>(nWorkers);
        for (int i = 0; i < nWorkers; i++) {
            workers.add(new WorkerThread(i, board, barrierPositions, barrierCollisions));
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
        return switch (winner) {
            case PLAYER1 -> "Vittoria Giocatore H (Punti: %d a %d)".formatted(s1, s2);
            case PLAYER2 -> "Vittoria Giocatore B (Punti: %d a %d)".formatted(s2, s1);
            case DRAW    -> "Pareggio! (%d a %d)".formatted(s1, s2);
            case NONE    -> "";
        };
    }
}
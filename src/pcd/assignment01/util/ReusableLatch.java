package pcd.assignment01.util;

/**
 * ReusableLatch — primitivo di sincronizzazione asimmetrico.
 *
 * Modella una dipendenza produttore-consumatori:
 *  - un singolo thread chiama {@code open()} quando il risultato è pronto
 *  - N thread chiamano {@code await()} e si bloccano finché il latch non è aperto
 *
 * A differenza di {@code java.util.concurrent.CountDownLatch}, questo latch
 * è riusabile: dopo che tutti i consumatori sono passati, il latch si richiude
 * automaticamente tramite {@code reset()}, pronto per il tick successivo.
 *
 * Differenza con CyclicBarrier:
 *  - CyclicBarrier: sincronizzazione SIMMETRICA — N thread si aspettano a vicenda
 *  - ReusableLatch:  sincronizzazione ASIMMETRICA — 1 produttore sblocca N consumatori
 *
 * Usato per la costruzione della SpatialGrid: Worker-0 chiama open() dopo
 * grid.build(), tutti gli altri chiamano await() prima di entrare in fase 2.
 */
public class ReusableLatch {

    private boolean open;   // true quando il produttore ha completato il lavoro

    public ReusableLatch() {
        this.open = false;
    }

    /**
     * Chiamato dal produttore (Worker-0) quando il lavoro è completato.
     * Sveglia tutti i thread in attesa.
     */
    public synchronized void open() {
        open = true;
        notifyAll();
    }

    /**
     * Chiamato dai consumatori (Worker-1..N, UpdateThread).
     * Si blocca finché il produttore non chiama {@code open()}.
     *
     * @throws InterruptedException se il thread viene interrotto mentre aspetta
     */
    public synchronized void await() throws InterruptedException {
        while (!open) {
            wait();
        }
    }

    /**
     * Richiude il latch per il tick successivo.
     * Deve essere chiamato dal GameController all'inizio di ogni tick,
     * prima di avviare i worker, quando nessun thread è in await().
     */
    public synchronized void reset() {
        open = false;
    }
}

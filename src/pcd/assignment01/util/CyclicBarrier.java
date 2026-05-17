package pcd.assignment01.util;

/**
 * CyclicBarrier implementata manualmente con monitor Java (wait/notifyAll).
 *
 * Permette a N thread di sincronizzarsi in un punto:
 * tutti chiamano await(), l'ultimo che arriva sveglia gli altri,
 * e tutti ripartono insieme.
 *
 * "Cyclic" significa che può essere riusata dopo ogni scatto:
 * il contatore interno si azzera automaticamente, pronta per il tick successivo.
 */
public class CyclicBarrier {

    private final int parties;
    private int count;
    private int generation;
    private boolean broken = false; // true se un thread è stato interrotto durante wait

    public CyclicBarrier(int parties) {
        if (parties <= 0) throw new IllegalArgumentException("parties must be > 0");
        this.parties    = parties;
        this.count      = parties;
        this.generation = 0;
    }

    /**
     * Blocca il thread chiamante finché tutti i {parties} thread non hanno chiamato await().
     * L'ultimo thread che arriva sveglia tutti e resetta la barrier per il ciclo successivo.
     *
     * Se un thread viene interrotto durante l'attesa, la barrier viene marcata come
     * "broken": tutti i thread in attesa vengono svegliati e ricevono InterruptedException.
     * Questo evita che count venga corrotto da ripristini multipli concorrenti.
     *
     * @throws InterruptedException se il thread viene interrotto o la barrier è broken
     */
    public synchronized void await() throws InterruptedException {
        if (broken) throw new InterruptedException("Barrier is broken");

        int myGeneration = generation;
        count--;

        if (count == 0) {
            // Sono l'ultimo: resetto e sveglio tutti
            broken     = false;
            count      = parties;
            generation++;
            notifyAll();
        } else {
            while (myGeneration == generation && !broken) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // Marco la barrier come rotta e sveglio tutti gli altri.
                    // Non ripristino count: broken impedisce usi futuri finché
                    // non viene resettata (al prossimo scatto dell'ultimo thread).
                    broken = true;
                    notifyAll();
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
            // Se svegliato perché broken (non perché la barrier è scattata)
            if (broken && myGeneration == generation) {
                throw new InterruptedException("Barrier is broken");
            }
        }
    }
}
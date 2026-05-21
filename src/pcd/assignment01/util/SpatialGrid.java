package pcd.assignment01.util;

import pcd.assignment01.model.Ball;
import pcd.assignment01.model.Boundary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SpatialGrid partiziona il campo in celle quadrate di lato {@code cellSize}.
 *
 * Ogni pallina viene inserita nella cella corrispondente alla sua posizione.
 * Per trovare i candidati alla collisione di una pallina A, si controllano
 * solo le palline nelle 9 celle intorno ad A (3x3), invece di tutte le N
 * palline — riducendo la complessità media da O(n²) a O(n).
 *
 * La dimensione della cella è {@code 2 * ballRadius}: due palline in celle
 * non adiacenti hanno distanza > cellSize > 2*radius, quindi non possono
 * collidere. Questo garantisce che nessuna coppia collidente venga persa.
 *
 * La griglia viene ricostruita interamente ogni tick da Worker-0, dopo
 * barrierPositions e prima di barrierCollisions.
 */
public class SpatialGrid {

    /**
     * Chiave di cella: coppia (col, row) di interi.
     * Implementa equals/hashCode per uso come chiave in HashMap.
     */
    private static final class CellKey {
        final int col, row;

        CellKey(int col, int row) {
            this.col = col;
            this.row = row;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CellKey)) return false;
            CellKey k = (CellKey) o;
            return col == k.col && row == k.row;
        }

        @Override
        public int hashCode() {
            // Cantor pairing — unico per ogni coppia (col, row) nei range attesi
            return 31 * col + row;
        }
    }

    private final double cellSize;
    private final double originX;   // bounds.x0()
    private final double originY;   // bounds.y0()

    // Mappa cella → lista di palline in quella cella
    // Ricostruita completamente ad ogni tick da build()
    private Map<CellKey, List<Ball>> grid;

    /**
     * @param bounds    boundary del campo (usato come origine della griglia)
     * @param ballRadius raggio delle palline piccole
     */
    public SpatialGrid(Boundary bounds, double ballRadius) {
        // La cella deve essere >= 2*radius per garantire che due palline
        // in celle non adiacenti non possano collidere.
        // Usiamo 2*radius + piccolo margine per robustezza numerica.
        this.cellSize = 2.0 * ballRadius + 1e-9;
        this.originX  = bounds.x0();
        this.originY  = bounds.y0();
        this.grid     = new HashMap<>();
    }

    /**
     * Ricostruisce la griglia dalle posizioni correnti delle palline.
     * Chiamato da Worker-0 tra barrierPositions e barrierCollisions.
     *
     * @param balls lista completa delle palline piccole (snapshot del tick)
     */
    public void build(List<Ball> balls) {
        grid.clear();
        for (Ball b : balls) {
            CellKey key = cellOf(b);
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(b);
        }
    }

    /**
     * Restituisce i candidati alla collisione per la pallina {@code a}:
     * tutte le palline nelle 9 celle intorno alla cella di {@code a}.
     * La lista include {@code a} stessa — il chiamante deve escluderla.
     *
     * @param a pallina di cui cercare i candidati
     * @return lista (possibilmente vuota) di candidati
     */
    public List<Ball> getCandidates(Ball a) {
        CellKey center = cellOf(a);
        List<Ball> candidates = new ArrayList<>();
        for (int dc = -1; dc <= 1; dc++) {
            for (int dr = -1; dr <= 1; dr++) {
                CellKey neighbor = new CellKey(center.col + dc, center.row + dr);
                List<Ball> cell = grid.get(neighbor);
                if (cell != null) {
                    candidates.addAll(cell);
                }
            }
        }
        return candidates;
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private CellKey cellOf(Ball b) {
        int col = (int) Math.floor((b.getPos().x() - originX) / cellSize);
        int row = (int) Math.floor((b.getPos().y() - originY) / cellSize);
        return new CellKey(col, row);
    }
}
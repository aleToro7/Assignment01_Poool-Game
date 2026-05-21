package pcd.assignment01.model;

public final class V2d {

    private final double x;
    private final double y;

    public V2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double x() { return x; }
    public double y() { return y; }

    public V2d sum(V2d v) {
        return new V2d(x + v.x, y + v.y);
    }

    public double abs() {
        return Math.sqrt(x * x + y * y);
    }

    public V2d getNormalized() {
        double module = Math.sqrt(x * x + y * y);
        return new V2d(x / module, y / module);
    }

    public V2d mul(double fact) {
        return new V2d(x * fact, y * fact);
    }

    public V2d getSwappedX() { return new V2d(-x, y); }
    public V2d getSwappedY() { return new V2d(x, -y); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof V2d)) return false;
        V2d v = (V2d) o;
        return Double.compare(v.x, x) == 0 && Double.compare(v.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "V2d(" + x + "," + y + ")";
    }
}
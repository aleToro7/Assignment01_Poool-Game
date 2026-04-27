package pcd.assignment01.model;

public class Boundary {
    
    private final double x0;
    private final double y0;
    private final double x1;
    private final double y1;
    
    public Boundary(double x0, double y0, double x1, double y1) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
    }
    
    public double x0() {
        return x0;
    }
    
    public double y0() {
        return y0;
    }
    
    public double x1() {
        return x1;
    }
    
    public double y1() {
        return y1;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Boundary boundary = (Boundary) o;
        return Double.compare(boundary.x0, x0) == 0 && 
               Double.compare(boundary.y0, y0) == 0 && 
               Double.compare(boundary.x1, x1) == 0 && 
               Double.compare(boundary.y1, y1) == 0;
    }
    
    @Override
    public int hashCode() {
        long temp;
        int result;
        temp = Double.doubleToLongBits(x0);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y0);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(x1);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y1);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
    
    @Override
    public String toString() {
        return "Boundary(x0=" + x0 + ", y0=" + y0 + ", x1=" + x1 + ", y1=" + y1 + ")";
    }
}

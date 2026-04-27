package pcd.assignment01.model;

public class P2d {
    
    private final double x;
    private final double y;
    
    public P2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public P2d sum(V2d v){
        return new P2d(x+v.x(),y+v.y());
    }

    public V2d sub(P2d v){
        return new V2d(x-v.x(),y-v.y());
    }
    
    @Override
    public String toString(){
        return "P2d("+x+","+y+")";
    }

    public double x() {
    	return x;
    }

    public double y() {
    	return y;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        P2d p2d = (P2d) o;
        return Double.compare(p2d.x, x) == 0 && Double.compare(p2d.y, y) == 0;
    }
    
    @Override
    public int hashCode() {
        long temp;
        temp = Double.doubleToLongBits(x);
        int result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}


package pcd.assignment01.model;


public class V2d {
    
    private final double x;
    private final double y;
    
    public V2d(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public double x() {
        return x;
    }
    
    public double y() {
        return y;
    }

    public V2d sum(V2d v){
        return new V2d(x+v.x,y+v.y);
    }

    public double abs(){
        return (double)Math.sqrt(x*x+y*y);
    }

    public V2d getNormalized(){
        double module=(double)Math.sqrt(x*x+y*y);
        return new V2d(x/module,y/module);
    }

    public V2d mul(double fact){
        return new V2d(x*fact,y*fact);
    }

    public V2d getSwappedX() {
    	return new V2d(-x, y);
    }

    public V2d getSwappedY() {
    	return new V2d(x, -y);
    }

    @Override
    public String toString(){
        return "V2d("+x+","+y+")";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        V2d v2d = (V2d) o;
        return Double.compare(v2d.x, x) == 0 && Double.compare(v2d.y, y) == 0;
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

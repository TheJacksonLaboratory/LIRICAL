package util;


/**
 * Yet another class for storing Pairs
 * @param <X>
 * @param <Y>
 */
public class Pair<X,Y> {

    public final X first;
    public final Y second;
    public Pair(X x, Y y) {
        first=x;
        second=y;
    }

    @Override
    public boolean equals(Object o) {
        if (o==null) return false;
        if (!Pair.class.isAssignableFrom(o.getClass())) {
            return false;
        }
        Pair other = (Pair)o;
        return first.equals(other.first) && second.equals(other.second);
    }


    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.first != null ? this.first.hashCode() : 0);
        hash = 53 * hash + (this.second!=null ? this.second.hashCode() : 0);
        return hash;
    }

}


package be.kuleuven.stgp.core.util;

public class Pair<A extends Comparable<? super A>, B> implements Comparable<Pair<A, B>> {

    public final A first;
    public final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public int compareTo(Pair<A, B> pair) {
        return first.compareTo(pair.first);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Pair<?, ?>))
            return false;
        if (object == this)
            return true;

        Pair<?, ?> pairInt = ( Pair<?, ?> ) object;
        return first.equals(pairInt.first) && second.equals(pairInt.second);
    }

    @Override
    public int hashCode() {
        int result = first != null ? first.hashCode() : 0;
        result = 31 * result + (second != null ? second.hashCode() : 0);
        return result;
    }
}

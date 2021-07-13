package be.kuleuven.stgp.core.util;

public class PairInt implements Comparable<PairInt> {

    private int first, second;


    public PairInt(int first, int second) {
        this.first = first;
        this.second = second;
    }

    
    public int hashCode() {
        int result = first;
        result = 31 * result + second;
        return result;
    }

    public int getFirst() {
        return first;
    }

    public void setFirst(int first) {
        this.first = first;
    }

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }


    @Override
    public int compareTo(PairInt pairInt) {
        if (pairInt.getFirst() == first && getSecond() == second)
            return 0;

        int value = Integer.compare(first, pairInt.getFirst());
        if (value != 0)
            return value;

        return Integer.compare(second, pairInt.getSecond());
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof PairInt))
            return false;
        if (object == this)
            return true;

        PairInt pairInt = ( PairInt ) object;
        return pairInt.first == first && pairInt.second == second;
    }
}

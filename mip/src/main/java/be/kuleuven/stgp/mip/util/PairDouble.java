package be.kuleuven.stgp.mip.util;

public class PairDouble<Z> implements Comparable<PairDouble<Z>> {

    public double fst;
    public Z snd;

    public PairDouble(Double fst, Z snd) {
        this.fst = fst;
        this.snd = snd;
    }

    public int compareTo(PairDouble<Z> pair) {
        return Double.compare(fst, pair.fst);
    }
}

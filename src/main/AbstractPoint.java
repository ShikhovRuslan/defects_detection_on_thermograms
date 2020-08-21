package main;


/**
 * Содержит точку двумерной системы координат.
 */
public class AbstractPoint {
    /**
     * Абсцисса точки.
     */
    protected final int i;
    /**
     * Ордината точки.
     */
    protected final int j;

    public AbstractPoint(int i, int j) {
        this.i = i;
        this.j = j;
    }

    public AbstractPoint(double iD, double jD) {
        this((int) Math.round(iD), (int) Math.round(jD));
    }

    public int getI() {
        return i;
    }

    public int getJ() {
        return j;
    }

    @Override
    public String toString() {
        return "(" + i + ", " + j + ")";
    }
}
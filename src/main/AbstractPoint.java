package main;


import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Содержит точку двумерной системы координат.
 */
public abstract class AbstractPoint {
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

    /**
     * Определяет принадлежность текущего пикселя треугольнику {@code triangle}.
     */
    public <T extends AbstractPoint> boolean isInTriangle(Polygon<T> triangle) {
        int[] sgn = new int[3];
        for (int k = 0; k < 3; k++) {
            if (isInLine(triangle.getVertices().get(k), triangle.getVertices().get(k + 1 < 3 ? k + 1 : 0)))
                return true;
            sgn[k] = (triangle.getVertices().get(k).getI() - i) * (triangle.getVertices().get(k + 1 < 3 ? k + 1 : 0).getJ() - triangle.getVertices().get(k).getJ()) -
                    (triangle.getVertices().get(k).getJ() - j) * (triangle.getVertices().get(k + 1 < 3 ? k + 1 : 0).getI() - triangle.getVertices().get(k).getI());
        }
        return (sgn[0] > 0 && sgn[1] > 0 && sgn[2] > 0) || (sgn[0] < 0 && sgn[1] < 0 && sgn[2] < 0);
    }

    /**
     * Определяет принадлежность текущей точки отрезку {@code line}.
     */
    public <T extends AbstractPoint> boolean isInLine(T p1, T p2) {
        if (p1.getI() == p2.getI())
            return i == p1.getI() && min(p1.getJ(), p2.getJ()) <= j && j <= max(p1.getJ(), p2.getJ());
        // Прямая, содержащая отрезок (в случае невертикального отрезка): y = a*x + b.
        double a = (p1.getJ() - p2.getJ()) / (p1.getI() - p2.getI() + 0.);
        double b = p1.getJ() - a * p1.getI();
        return j == a * i + b && min(p1.getI(), p2.getI()) <= i && i <= max(p1.getI(), p2.getI());
    }

    @Override
    public String toString() {
        return getClass().getName() + "(" + i + ", " + j + ")";
    }
}
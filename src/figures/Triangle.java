package figures;

import java.util.List;


/**
 * Содержит треугольник. Число вершин равно 3. Без петель (например, ABBC) и вершин, являющихся вершинами развёрнутого
 * угла.
 *
 * @param <T> тип вершин
 */
public class Triangle<T extends AbstractPoint> extends Polygon<T> {
    public Triangle(List<T> vertices, double focalLength) {
        super(vertices, focalLength);
        if (vertices.size() != 3)
            throw new IllegalArgumentException("Число вершин в треугольнике не равно 3.");
    }

    @Override
    public boolean contains(T point, double focalLength, double eps) {
        int[] sgn = new int[3];
        for (int k = 0; k < 3; k++) {
            if (point.isInLine(getVertices().get(k), getVertices().get(k + 1 < 3 ? k + 1 : 0), eps))
                return true;
            sgn[k] = (getVertices().get(k).getI() - point.getI()) *
                    (getVertices().get(k + 1 < 3 ? k + 1 : 0).getJ() - getVertices().get(k).getJ()) -
                    (getVertices().get(k).getJ() - point.getJ()) *
                            (getVertices().get(k + 1 < 3 ? k + 1 : 0).getI() - getVertices().get(k).getI());
        }
        return (sgn[0] > 0 && sgn[1] > 0 && sgn[2] > 0) || (sgn[0] < 0 && sgn[1] < 0 && sgn[2] < 0);
    }

    @Override
    public double square(double focalLength) {
        return 0.5 * Math.abs((getVertices().get(2).getI() - getVertices().get(0).getI()) *
                (getVertices().get(1).getJ() - getVertices().get(0).getJ()) -
                (getVertices().get(2).getJ() - getVertices().get(0).getJ()) *
                        (getVertices().get(1).getI() - getVertices().get(0).getI()));
    }
}
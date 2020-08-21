package main;

import java.util.ArrayList;
import java.util.List;


/**
 * Содержит прямоугольник, стороны которого параллельны координатным осям и который задаётся двумя вершинами: нижней
 * левой и верхней правой.
 */
public class RectanglePixel {
    /**
     * Нижняя левая вершина прямоугольника.
     */
    private final Pixel lowerLeft;
    /**
     * Верхняя правая вершина прямоугольника.
     */
    private final Pixel upperRight;

    public RectanglePixel(Pixel lowerLeft, Pixel upperRight) {
        this.lowerLeft = lowerLeft;
        this.upperRight = upperRight;
    }

    Pixel getLowerLeft() {
        return lowerLeft;
    }

    Pixel getUpperRight() {
        return upperRight;
    }

    /**
     * Возвращает площадь текущего прямоугольника.
     */
    public double square() {
        return (upperRight.getI() - lowerLeft.getI()) * (upperRight.getJ() - lowerLeft.getJ());
    }

    /**
     * Определяет принадлежность пикселя {@code pixel} текущему прямоугольнику.
     */
    public boolean containsPixel(Pixel pixel) {
        return (lowerLeft.getI() <= pixel.getI() && pixel.getI() <= upperRight.getI()) &&
                (lowerLeft.getJ() <= pixel.getJ() && pixel.getJ() <= upperRight.getJ());
    }

    /**
     * Возвращает список вершин многоугольника {@code polygon}, которые принадлежат текущему прямоугольнику.
     */
    public List<Pixel> verticesFrom(PolygonPixel polygon) {
        List<Pixel> res = new ArrayList<>();
        for (Pixel vertex : polygon.getVertices())
            if (containsPixel(vertex))
                res.add(vertex);
        return res;
    }
}
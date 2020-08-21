package main;

import java.util.ArrayList;
import java.util.List;

/**
 * Стороны прямоугольника параллельны координатным осям, и он задаётся двумя вершинами: нижней левой и верхней правой.
 */
public class Rectangle {
    /**
     * Нижняя левая вершина прямоугольника.
     */
    private final Pixel lowerLeft;
    /**
     * Верхняя правая вершина прямоугольника.
     */
    private final Pixel upperRight;

    public Rectangle(Pixel lowerLeft, Pixel upperRight) {
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
    public double squareRectangle() {
        return (upperRight.getI() - lowerLeft.getI()) * (upperRight.getJ() - lowerLeft.getJ());
    }

    /**
     * Определяет принадлежность пикселя {@code pixel} текущему прямоугольнику.
     */
    public boolean isInRectangle(Pixel pixel) {
        return (lowerLeft.getI() <= pixel.getI() && pixel.getI() <= upperRight.getI()) &&
                (lowerLeft.getJ() <= pixel.getJ() && pixel.getJ() <= upperRight.getJ());
    }

    /**
     * Возвращает список вершин многоугольника {@code polygon}, которые принадлежат текущему прямоугольнику.
     */
    public List<Pixel> inRectangle(PolygonPixel polygon) {
        List<Pixel> res = new ArrayList<>();
        for (Pixel vertex : polygon.getVertices())
            if (isInRectangle(vertex))
                res.add(vertex);
        return res;
    }
}
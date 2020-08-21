package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Содержит многоугольник, который задаётся списком упорядоченных вершин.
 */
public class PolygonPixel {
    /**
     * Список вершин многоугольника.
     */
    private final List<Pixel> vertices;

    public PolygonPixel(List<Pixel> vertices) {
        this.vertices = vertices;
    }

    public List<Pixel> getVertices() {
        return vertices;
    }

    /**
     * Возвращает список треугольников, из которых состоит текущий многоугольник.
     */
    public List<PolygonPixel> toTriangles() {
        List<PolygonPixel> triangles = new ArrayList<>();
        for (int k = 1; k < vertices.size() - 1; k++)
            triangles.add(new PolygonPixel(Arrays.asList(vertices.get(0), vertices.get(k), vertices.get(k + 1))));
        return triangles;
    }

    /**
     * Возвращает список вершин многоугольника {@code polygon}, которые принадлежат текущему многоугольнику.
     */
    public List<Pixel> verticesFrom(PolygonPixel polygon) {
        List<Pixel> res = new ArrayList<>();
        for (Pixel vertex : polygon.getVertices())
            if (containsPixel(vertex))
                res.add(vertex);
        return res;
    }

    /**
     * Определяет принадлежность пикселя {@code pixel} текущему многоугольнику.
     */
    public boolean containsPixel(Pixel pixel) {
        for (PolygonPixel triangle : toTriangles())
            if (pixel.isInTriangle(triangle))
                return true;
        return false;
    }

    /**
     * Преобразует прямоугольник {@code rectangle} в многоугольник, создавая список его вершин, начиная с нижней левой
     * вершины и заканчивая верхней левой.
     */
    public static PolygonPixel toPolygon(RectanglePixel rectangle) {
        List<Pixel> vertices = new ArrayList<>();
        vertices.add(rectangle.getLowerLeft());
        vertices.add(new Pixel(rectangle.getUpperRight().getI(), rectangle.getLowerLeft().getJ()));
        vertices.add(rectangle.getUpperRight());
        vertices.add(new Pixel(rectangle.getLowerLeft().getI(), rectangle.getUpperRight().getJ()));
        return new PolygonPixel(vertices);
    }

    @Override
    public String toString() {
        return Arrays.toString(vertices.toArray());
    }
}
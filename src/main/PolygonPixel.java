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
            triangles.add(new PolygonPixel(Arrays.asList(vertices.get(0), vertices.get(k), vertices.get(k+1))));
        return triangles;
    }

    @Override
    public String toString() {
        return Arrays.toString(vertices.toArray());
    }
}
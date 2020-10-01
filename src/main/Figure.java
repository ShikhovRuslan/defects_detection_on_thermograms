package main;

import java.util.ArrayList;
import java.util.List;


/**
 * Содержит методы, которые связаны с абстрактной фигурой.
 */
public interface Figure<T extends AbstractPoint> {
    /**
     * Возвращает список вершин многоугольника {@code polygon}, которые принадлежат текущей фигуре.
     */
    default List<T> verticesFrom(Polygon<T> polygon, double focalLength) {
        List<T> res = new ArrayList<>();
        for (T vertex : polygon.getVertices())
            if (contains(vertex, focalLength))
                res.add(vertex);
        return res;
    }

    /**
     * Преобразует прямоугольник {@code rectangle} в многоугольник, создавая список его вершин, которые упорядочены
     * против часовой стрелки.
     */
    static <T extends AbstractPoint> Polygon<T> toPolygon(Rectangle<T> rectangle, double squareRectangleWithoutOverlap, double height, double focalLength) {
        List<T> vertices = new ArrayList<>();
        vertices.add(rectangle.getLeft());
        vertices.add((T) rectangle.getLeft().create(rectangle.getRight().getI(), rectangle.getLeft().getJ()));
        vertices.add(rectangle.getRight());
        vertices.add((T) rectangle.getLeft().create(rectangle.getLeft().getI(), rectangle.getRight().getJ()));
        return new Polygon<>(vertices, squareRectangleWithoutOverlap, height, focalLength);
    }

    /**
     * Определяет принадлежность точки {@code point} текущей фигуре.
     */
    boolean contains(T point, double focalLength);

    /**
     * Возвращает площадь (в кв. пикселях) текущей фигуры.
     */
    double square(double focalLength);
}
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
    default List<T> verticesFrom(Polygon<T> polygon) {
        List<T> res = new ArrayList<>();
        for (T vertex : polygon.getVertices())
            if (contains(vertex))
                res.add(vertex);
        return res;
    }

    /**
     * Преобразует прямоугольник {@code rectangle} в многоугольник, создавая список его вершин, которые упорядочены
     * против часовой стрелки.
     */
    static <T extends AbstractPoint> Polygon<T> toPolygon(Rectangle<T> rectangle, double squareRectangleWithoutOverlap) {
        List<T> vertices = new ArrayList<>();
        vertices.add(rectangle.getLeft());
        vertices.add((T) rectangle.getLeft().create(rectangle.getRight().getI(), rectangle.getLeft().getJ()));
        vertices.add(rectangle.getRight());
        vertices.add((T) rectangle.getLeft().create(rectangle.getLeft().getI(), rectangle.getRight().getJ()));
        return new Polygon<>(vertices, squareRectangleWithoutOverlap);
    }

    /**
     * Определяет принадлежность точки {@code point} текущей фигуре.
     */
    boolean contains(T point);

    /**
     * Возвращает площадь (в кв. пикселях) текущей фигуры.
     */
    double square();
}
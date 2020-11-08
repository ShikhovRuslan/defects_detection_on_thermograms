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
     * Определяет принадлежность точки {@code point} текущей фигуре.
     */
    boolean contains(T point, double focalLength);

    /**
     * Возвращает площадь (в кв. пикселях) текущей фигуры.
     */
    double square(double focalLength);
}
package main;

import java.util.List;

import static tmp.Base.*;


/**
 * Каждая константа перечисления описывает ситуацию пересечения прямоугольников p1 и p2 и содержит два предиката:
 * {@code condition} и {@code action}. Первый предикат - условие, при котором прямоугольники пересекаются, а второй -
 * действие, которое нужно выполнить с прямоугольником p1, если первый предикат выдаёт {@code true}. Второй предикат
 * возвращает {@code boolean}, показывающий, изменился ли прямоугольник p1 (точнее, была ли предпринята попытка его
 * изменить).
 * <p>
 * Аргументы предикатов {@code condition} и {@code action}:
 * <ul>
 *     <li> {@code p1} - прямоугольник,</li>
 *     <li> {@code p2} - прямоугольник,</li>
 *     <li> {@code pipeAngle1} - угол наклона трубы, соответствующий прямоугольнику {@code p1},</li>
 *     <li> {@code minSquare} - минимальная площадь пересечения прямоугольников.</li>
 * </ul>
 * <p>
 * Для различных констант перечисления предусмотрены 3 действия:
 * <ul>
 *     <li> {@code markToDelete} - пометить p1 для удаления (т. е. установить в качестве 0-й вершины значение
 *     {@code null}) (возвращает {@code true}),</li>
 *     <li> {@code markToDeleteMinSquare} - пометить p1 для удаления, если площадь пересечения {@code > minSquare}
 *      (возвращает {@code true} при выполнении этого условия),</li>
 *     <li> {@code shorten} - укоротить p1, если площадь пересечения {@code > minSquare} и укорочение возможно
 *     (возвращает {@code true} при выполнении этого условия).</li>
 * </ul>
 * Значение {@code false} в {@code shorten} может говорить о том, что невозможно отредактировать p1, хотя это нужно.
 */
public enum Intersection {
    /**
     * У прямоугольника p1 имеется хотя бы одна диагональ, принадлежащая p2.
     * Действие: {@code markToDelete}.
     */
    DIAGONAL((p1, p2, pipeAngle1, minSquare) -> {
        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);
        List<Pixel> v1 = p1.getVertices();

        return (verticesFromP1.size() == 2 &&
                v1.indexOf(verticesFromP1.get(1)) - v1.indexOf(verticesFromP1.get(0)) == 2 ||
                verticesFromP1.size() >= 3);
    }),

    /**
     * У прямоугольника p1 имеется сторона, которая параллельна трубе и принадлежит p2.
     * Действие: {@code markToDeleteMinSquare}.
     */
    PARALLEL_SIDE((p1, p2, pipeAngle1, minSquare) -> {
        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);

        return verticesFromP1.size() == 2 &&
                sideParallelToPipe(verticesFromP1.get(0), verticesFromP1.get(1), p1, pipeAngle1);
    }),

    /**
     * У прямоугольника p1 имеется сторона, которая перпендикулярна трубе и принадлежит p2.
     * Действие: {@code shorten}.
     */
    PERPENDICULAR_SIDE((p1, p2, pipeAngle1, minSquare) -> {
        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);

        return verticesFromP1.size() == 2 &&
                sidePerpendicularToPipe(verticesFromP1.get(0), verticesFromP1.get(1), p1, pipeAngle1);
    }),

    /**
     * Одна вершина прямоугольника p1 принадлежит p2.
     * Действие: {@code shorten}.
     */
    ONE_VERTEX((p1, p2, pipeAngle1, minSquare) ->
            p2.verticesFrom(p1, -1).size() == 1
    );

    private static final Predicate4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> markToDelete =
            (p1, p2, pipeAngle1, minSquare) -> {
                p1.getVertices().set(0, null);
                return true;
            };

    private static final Predicate4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> markToDeleteMinSquare =
            (p1, p2, pipeAngle1, minSquare) ->
                    Rectangle.getIntersection(p1, p2, -1).square(-1) > minSquare &&
                            markToDelete.test(p1, p2, pipeAngle1, null);

    private static final Predicate4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> shorten =
            (p1, p2, pipeAngle1, minSquare) -> {
                List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);

                Object[] o = findShift(p1, p2, pipeAngle1, verticesFromP1.get(0), minSquare);
                if (o.length == 2) {
                    shorten(p1, (double) o[0], (String) o[1], pipeAngle1);
                    return true;
                }
                return false;
            };

    private final Predicate4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> condition;
    private Predicate4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> action;

    Intersection(Predicate4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> condition) {
        this.condition = condition;
    }

    static {
        DIAGONAL.action = markToDelete;
        PARALLEL_SIDE.action = markToDeleteMinSquare;
        PERPENDICULAR_SIDE.action = shorten;
        ONE_VERTEX.action = shorten;
    }

    public Predicate4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> getCondition() {
        return condition;
    }

    public Predicate4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> getAction() {
        return action;
    }
}
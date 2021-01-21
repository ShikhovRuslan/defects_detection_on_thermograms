package main;

import java.util.List;

import static tmp.Base.*;


/**
 * Каждая константа перечисления описывает ситуацию пересечения прямоугольников p1 и p2 и содержит предикат
 * {@code condition} и функцию {@code action}. Предикат - условие, при котором прямоугольники пересекаются, а функция -
 * действие, которое нужно выполнить с прямоугольником p1, чтобы ликвидировать пересечение внутренностей
 * прямоугольников, если предикат выдаёт {@code true}.
 * <p>
 * Функция возвращает:
 * <ul>
 *     <li>{@code 0}, если {@code p1} изменился,</li>
 *     <li>{@code 1}, если {@code p1} не изменился по причине малости площади пересечения (а именно, она
 *     {@code <= minSquare}),</li>
 *     <li>{@code 2}, если {@code p1} не изменился по причине невозможности.</li>
 * </ul>
 * Не все функции осуществляют проверку величины площади пересечения.
 * <p>
 * Аргументы предиката {@code condition} и функции {@code action}:
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
 *     {@code null}) (возвращает {@code 0}),</li>
 *     <li> {@code markToDeleteMinSquare} - пометить p1 для удаления, если площадь пересечения {@code > minSquare}
 *      (возвращает {@code 0} при выполнении этого условия, иначе {@code 1}),</li>
 *     <li> {@code shorten} - укоротить p1, если площадь пересечения {@code > minSquare} и укорочение возможно
 *     (возвращает {@code 0} при выполнении этого условия, иначе {@code 1} при нарушении 1-го условия и {@code 2} при
 *     нарушении 2-го).</li>
 * </ul>
 */
public enum Intersection {
    /**
     * У прямоугольника p1 имеется хотя бы одна диагональ, принадлежащая p2.
     * Действие: {@code markToDelete}.
     */
    DIAGONAL((p1, p2, pipeAngle1, minSquare) -> {
        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1, 1);
        List<Pixel> v1 = p1.getVertices();

        return (verticesFromP1.size() == 2 &&
                v1.indexOf(verticesFromP1.get(1)) - v1.indexOf(verticesFromP1.get(0)) == 2 ||
                verticesFromP1.size() >= 3);
    }),

    /**
     * У прямоугольника p1 имеются 2 вершины, принадлежащие p2 и образующие сторону, которая параллельна трубе.
     * Действие: {@code markToDeleteMinSquare}.
     */
    PARALLEL_SIDE((p1, p2, pipeAngle1, minSquare) -> {
        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1, 1);

        return verticesFromP1.size() == 2 &&
                p1.sideParallelToPipe(verticesFromP1.get(0), verticesFromP1.get(1), pipeAngle1);
    }),

    /**
     * У прямоугольника p1 имеются 2 вершины, принадлежащие p2 и образующие сторону, которая перпендикулярна трубе.
     * Действие: {@code shorten}.
     */
    PERPENDICULAR_SIDE((p1, p2, pipeAngle1, minSquare) -> {
        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1, 1);

        return verticesFromP1.size() == 2 &&
                p1.sidePerpendicularToPipe(verticesFromP1.get(0), verticesFromP1.get(1), pipeAngle1);
    }),

    /**
     * Одна вершина прямоугольника p1 принадлежит p2.
     * Действие: {@code shorten}.
     */
    ONE_VERTEX((p1, p2, pipeAngle1, minSquare) ->
            p2.verticesFrom(p1, -1, 1).size() == 1
    );

    private static final Function4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> markToDelete =
            (p1, p2, pipeAngle1, minSquare) -> {
                p1.getVertices().set(0, null);
                return 0;
            };

    private static final Function4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> markToDeleteMinSquare =
            (p1, p2, pipeAngle1, minSquare) -> {
                if (Polygon.getIntersection(p1, p2, -1).square(-1) <= minSquare)
                    return 1;
                else
                    return markToDelete.apply(p1, p2, pipeAngle1, null);
            };

    private static final Function4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> shorten =
            (p1, p2, pipeAngle1, minSquare) -> {
                List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1, 1);

                Object[] o = Polygon.findShift(p1, p2, pipeAngle1, verticesFromP1.get(0), minSquare);
                if (o.length == 2) {
                    p1.shorten((double) o[0], (String) o[1], pipeAngle1);
                    return 0;
                }
                return o.length == 1 ? 1 : 2;
            };

    private final Predicate4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> condition;
    private Function4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> action;

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

    public Function4<Polygon<Pixel>, Polygon<Pixel>, Double, Double> getAction() {
        return action;
    }

//    public boolean conditionAndAction(Polygon<Pixel> p1, Polygon<Pixel> p2, double pipeAngle1, double minSquare) {
//        return condition.test(p1, p2, pipeAngle1, minSquare) && action.test(p1, p2, pipeAngle1, minSquare);
//    }
}
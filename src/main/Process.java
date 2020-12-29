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
 * Для различных констант перечисления предусмотрены 2 действия:
 * <ul>
 *     <li> - пометить p1 для удаления, т. е. установить в качестве 0-й вершины значение {@code null} (возвращает
 *     {@code true}),</li>
 *     <li> - укоротить p1 (возвращает {@code false} при невозможности сдвига или нулевой величине сдвига).</li>
 * </ul>
 */
public enum Process {
    /**
     * У прямоугольника p1 имеется хотя бы одна диагональ, принадлежащая p2.
     * Действие: пометить p1 для удаления.
     */
    DIAGONAL((p1, p2, pipeAngle1) -> {
        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);
        List<Pixel> v1 = p1.getVertices();

        return (verticesFromP1.size() == 2 &&
                v1.indexOf(verticesFromP1.get(1)) - v1.indexOf(verticesFromP1.get(0)) == 2 ||
                verticesFromP1.size() >= 3);
    }),

    /**
     * У прямоугольника p1 имеется сторона, которая параллельна трубе и принадлежит p2. Эти прямоугольники должны
     * пересекаться существенным образом (т. е. площадь пересечения должна быть {@code >0}).
     * Действие: пометить p1 для удаления.
     */
    PARALLEL_SIDE((p1, p2, pipeAngle1) -> {
        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);

        return verticesFromP1.size() == 2 &&
                sideParallelToPipe(verticesFromP1.get(0), verticesFromP1.get(1), p1, pipeAngle1) &&
                checkInteriorIntersection(p1, p2, -1);
    }),

    /**
     * У прямоугольника p1 имеется сторона, которая перпендикулярна трубе и принадлежит p2.
     * Действие: укоротить p1.
     */
    PERPENDICULAR_SIDE((p1, p2, pipeAngle1) -> {
        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);

        return verticesFromP1.size() == 2 &&
                sidePerpendicularToPipe(verticesFromP1.get(0), verticesFromP1.get(1), p1, pipeAngle1);
    }),

    /**
     * Одна вершина прямоугольника p1 принадлежит p2.
     * Действие: укоротить p1.
     */
    ONE_VERTEX((p1, p2, pipeAngle1) ->
            p2.verticesFrom(p1, -1).size() == 1
    );

    private static final TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> markToDelete = (p1, p2, pipeAngle1) -> {
        p1.getVertices().set(0, null);
        return true;
    };

    private static final TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> shorten = (p1, p2, pipeAngle1) -> {
        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);
        List<Pixel> verticesFromP2 = p1.verticesFrom(p2, -1);

        Object[] o = whatToShorten(p1, p2, pipeAngle1, verticesFromP1.get(0), verticesFromP2.toArray(new Pixel[0]));
        if (o.length == 2 && (double) o[0] > 0) {
            shorten(p1, (double) o[0], (String) o[1], pipeAngle1);
            return true;
        }
        return false;
    };

    private final TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> condition;
    private TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> action;

    Process(TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> condition) {
        this.condition = condition;
    }

    static {
        DIAGONAL.action = markToDelete;
        PARALLEL_SIDE.action = markToDelete;
        PERPENDICULAR_SIDE.action = shorten;
        ONE_VERTEX.action = shorten;
    }

    public TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> getCondition() {
        return condition;
    }

    public TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> getAction() {
        return action;
    }
}
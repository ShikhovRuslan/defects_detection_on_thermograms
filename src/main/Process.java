package main;

import java.util.List;

import static java.lang.Math.abs;
import static tmp.Base.*;


/**
 *
 */
public enum Process {
    INNER(
            (p1, p2, pipeAngle1) ->
                    p2.verticesFrom(p1, -1).size() == p1.getVertices().size(),

            (p1, p2, pipeAngle1) -> {
                p1.getVertices().set(0, null);
                return true;
            }
    ),

    TWO_OPPOSITE(
            (p1, p2, pipeAngle1) -> {
                List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);
                List<Pixel> v1 = p1.getVertices();

                return (verticesFromP1.size() == 2 &&
                        abs(v1.indexOf(verticesFromP1.get(0)) -
                                v1.indexOf(verticesFromP1.get(1))) == 2 ||
                        verticesFromP1.size() == 3);
            },

            (p1, p2, pipeAngle1) -> {
                p1.getVertices().set(0, null);
                return true;
            }
    ),

    TWO_SEQUENTIAL_PARALLEL(
            (p1, p2, pipeAngle1) -> {
                List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);

                return verticesFromP1.size() == 2 &&
                        sideParallelToPipe(verticesFromP1.get(0), verticesFromP1.get(1), p1, pipeAngle1) &&
                        checkInteriorIntersection(p1, p2, -1);
            },

            (p1, p2, pipeAngle1) -> {
                p1.getVertices().set(0, null);
                return true;
            }
    ),

    TWO_SEQUENTIAL_PERPENDICULAR(
            (p1, p2, pipeAngle1) -> {
                List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);

                return verticesFromP1.size() == 2 &&
                        sidePerpendicularToPipe(verticesFromP1.get(0), verticesFromP1.get(1), p1, pipeAngle1);
            },

            (p1, p2, pipeAngle1) -> {
                List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);
                List<Pixel> verticesFromP2 = p1.verticesFrom(p2, -1);

                Object[] o = whatToShorten(p1, p2, pipeAngle1,
                        verticesFromP1.get(0),
                        verticesFromP2.toArray(new Pixel[0]));
                if (o.length == 2 && (double) o[0] > 0) {
                    shorten(p1, (double) o[0], (String) o[1], pipeAngle1);
                    return true;
                }
                return false;
            }
    ),

    ONE(
            (p1, p2, pipeAngle1) ->
                    p2.verticesFrom(p1, -1).size() == 1,

            (p1, p2, pipeAngle1) -> {
                List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);
                List<Pixel> verticesFromP2 = p1.verticesFrom(p2, -1);
                Object[] o = whatToShorten(p1, p2, pipeAngle1,
                        verticesFromP1.get(0),
                        verticesFromP2.get(0));
                if (o.length > 0 && (double) o[0] > 0) {
                    shorten(p1, (double) o[0], (String) o[1], pipeAngle1);
                    return true;
                }
                return false;
            }
    );

    static TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> markToDelete = (p1, p2, pipeAngle1) -> {
        p1.getVertices().set(0, null);
        return true;
    };

    private final TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> condition;
    private final TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> action;

    Process(TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> condition,
            TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> action) {

        this.condition = condition;
        this.action = action;
    }

    public TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> getCondition() {
        return condition;
    }

    public TriPredicate<Polygon<Pixel>, Polygon<Pixel>, Double> getAction() {
        return action;
    }
}
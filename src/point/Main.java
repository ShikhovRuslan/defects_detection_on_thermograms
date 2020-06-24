package point;

import java.util.ArrayList;
import java.util.List;

class Line {
    private Point a;
    private Point b;

    Line(Point a, Point b) {
        this.a = a;
        this.b = b;
    }

    Point getA() {
        return a;
    }

    Point getB() {
        return b;
    }

    boolean isHorizontal() {
        return a.getX() == b.getX();
    }

    boolean isVertical() {
        return a.getY() == b.getY();
    }

    Point getOtherEnd(Point point) {
        if (a == point) return b;
        if (b == point) return a;
        return new Point(0, 0);
    }

    Point upperEnd() {
        return a.getX() < b.getX() ? a : b;
    }

    Point lowerEnd() {
        return a.getX() > b.getX() ? a : b;
    }

    Point rightEnd() {
        return a.getY() > b.getY() ? a : b;
    }

    Point leftEnd() {
        return a.getY() < b.getY() ? a : b;
    }
}

class Point {
    private int x;
    private int y;

    Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    // Выдаёт осмысленный результат, только если projectableTo(line).
    int distance(Line line) {
        if (projectableTo(line)) {
            if (line.isHorizontal())
                return Math.abs(x - line.getA().getX());
            if (line.isVertical())
                return Math.abs(y - line.getA().getY());
        }
        return 0;
    }

    boolean projectableTo(Line line) {
        if (line.isHorizontal())
            return y >= Math.min(line.getA().getY(), line.getB().getY()) && y <= Math.max(line.getA().getY(), line.getB().getY());
        if (line.isVertical())
            return x >= Math.min(line.getA().getX(), line.getB().getX()) && y <= Math.max(line.getA().getX(), line.getB().getX());
        return false;
    }

    Point project(Line line) {
        if (line.isHorizontal()) return new Point(line.getA().getX(), y);
        if (line.isVertical()) return new Point(x, line.getA().getY());
        return new Point(0, 0);
    }
}

class Polygon {
    private List<Point> vertices;

    Polygon(List<Point> vertices) {
        this.vertices = vertices;
    }

    Line[] getSides() {
        Line[] sides = new Line[vertices.size()];
        for (int i = 0; i < vertices.size(); i++)
            sides[i] = new Line(vertices.get(i), vertices.get(i < vertices.size() - 1 ? i + 1 : 0));
        return sides;
    }

    boolean isCloseTo(Polygon polygon, int distance) {
        for (Point vertex : vertices)
            for (Line side : polygon.getSides())
                if (vertex.projectableTo(side) && vertex.distance(side) <= distance)
                    return true;
        return false;
    }

    Line[] linkingLine(Polygon polygon, int distance) {
        Point vertex0 = null;
        Line side1 = null;
        for (Point vertex : vertices)
            for (Line side : polygon.getSides())
                if (vertex.projectableTo(side) && vertex.distance(side) <= distance) {
                    vertex0 = vertex;
                    side1 = side;
                }
        return new Line[]{new Line(vertex0, vertex0.project(side1)), side1};
    }

    int getSideToShorten(Point vertex0, boolean isLinkingLineHorizontal) {
        Line side0 = null;
        for (int i = 0; i < getSides().length; i++)
            if (vertex0 == getSides()[i].getA() || vertex0 == getSides()[i].getB())
                if (isLinkingLineHorizontal && getSides()[i].isVertical() ||
                        !isLinkingLineHorizontal && getSides()[i].isHorizontal())
                    return i;
        return 0;
    }

    static Line[][] changePolygons(Polygon gonA, Polygon gonB,
                                   int side0Index, Point vertex0, Line linkingLine, Point vertex1, int side1Index) {
        Line[] sidesA = gonA.getSides();
        Line[] sidesB = gonB.getSides();
        Line sideAToShorten = sidesA[side0Index];
        Line sideBToShorten = sidesB[side1Index];
        Line newSideA = null;
        Line newSideB = null;
        Line newSideB2;
        Point pA = sideAToShorten.getOtherEnd(vertex0);
        if (linkingLine.isHorizontal()) {
            if (pA.getX() < vertex0.getX()) {
                if (pA.getX() < sideBToShorten.upperEnd().getX())
                    newSideA = new Line(pA, new Point(sideBToShorten.upperEnd().getX(), vertex0.getY()));
                else newSideB = new Line(sideBToShorten.upperEnd(), new Point(pA.getX(), vertex1.getY()));
                newSideB2 = new Line(vertex1, sideBToShorten.lowerEnd());
            } else {
                if (pA.getX() > sideBToShorten.lowerEnd().getX())
                    newSideA = new Line(pA, new Point(sideBToShorten.lowerEnd().getX(), vertex0.getY()));
                else newSideB = new Line(sideBToShorten.lowerEnd(), new Point(pA.getX(), vertex1.getY()));
                newSideB2 = new Line(vertex1, sideBToShorten.upperEnd());
            }
        } else {
            if (pA.getY() < vertex0.getY()) {
                if (pA.getY() < sideBToShorten.leftEnd().getY())
                    newSideA = new Line(pA, new Point(vertex0.getX(), sideBToShorten.leftEnd().getY()));
                else newSideB = new Line(sideBToShorten.leftEnd(), new Point(vertex1.getX(), pA.getY()));
                newSideB2 = new Line(vertex1, sideBToShorten.rightEnd());
            } else {
                if (pA.getY() > sideBToShorten.rightEnd().getY())
                    newSideA = new Line(pA, new Point(vertex0.getX(), sideBToShorten.rightEnd().getY()));
                else newSideB = new Line(sideBToShorten.rightEnd(), new Point(vertex1.getX(), pA.getY()));
                newSideB2 = new Line(vertex1, sideBToShorten.leftEnd());
            }
        }
        sidesA[side0Index] = newSideA;
        Line[] sidesBNew = new Line[sidesB.length + 1];
        System.arraycopy(sidesB, 0, sidesBNew, 0, sidesB.length);
        sidesBNew[side1Index] = newSideB;
        sidesBNew[sidesB.length] = newSideB2;
        return new Line[][]{sidesA, sidesBNew};
    }
}

class Main {
    public static void main(String[] args) {
        List<Point> pointsA = new ArrayList<>();
        List<Point> pointsB = new ArrayList<>();

        pointsA.add(new Point(1, 1));
        pointsA.add(new Point(1, 7));
        Point ver0 = new Point(5, 7);
        pointsA.add(ver0);
        pointsA.add(new Point(5, 5));
        pointsA.add(new Point(6, 5));
        pointsA.add(new Point(6, 1));

        pointsB.add(new Point(4, 8));
        pointsB.add(new Point(4, 10));
        pointsB.add(new Point(6, 10));
        pointsB.add(new Point(6, 12));
        pointsB.add(new Point(8, 12));
        pointsB.add(new Point(8, 11));
        pointsB.add(new Point(10, 11));
        pointsB.add(new Point(10, 8));

        Polygon polygonA = new Polygon(pointsA);
        Polygon polygonB = new Polygon(pointsB);

        Line[][] res = Polygon.changePolygons(polygonA, polygonB, 1, ver0, new Line(ver0, new Point(5, 8)), new Point(5, 8), 7);
        int u = 0;
    }
}
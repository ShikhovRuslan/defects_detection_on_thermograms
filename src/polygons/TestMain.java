package polygons;

import main.Polygon;

import java.util.ArrayList;
import java.util.List;


class TestMain {
    public static void main(String[] args) {
        List<Point> pointsA1 = new ArrayList<>();
        List<Point> pointsB1 = new ArrayList<>();

        pointsA1.add(new Point(1, 1));
        pointsA1.add(new Point(1, 7));
        Point ver0 = new Point(5, 7);
        pointsA1.add(ver0);
        pointsA1.add(new Point(5, 5));
        pointsA1.add(new Point(6, 5));
        pointsA1.add(new Point(6, 1));

        pointsB1.add(new Point(4, 8));
        pointsB1.add(new Point(4, 10));
        pointsB1.add(new Point(6, 10));
        pointsB1.add(new Point(6, 12));
        pointsB1.add(new Point(8, 12));
        pointsB1.add(new Point(8, 11));
        pointsB1.add(new Point(10, 11));
        pointsB1.add(new Point(10, 8));

        double focalLength = 25. / 1000;

        Polygon<Point> gonA1 = new Polygon<>(pointsA1, focalLength);
        Polygon<Point> gonB1 = new Polygon<>(pointsB1, focalLength);

        //Line[][] res = Polygon.getPolygonalChains(gonA1, gonB1, 1, ver0, new Line(ver0, new Point(5, 8)), new Point(5, 8), 7);

        //Polygon unitedGon1 = Polygon.unitePolygons(gonA1, gonB1);

        List<Point> pointsA2 = new ArrayList<>();
        List<Point> pointsB2 = new ArrayList<>();

        pointsA2.add(new Point(1, 1));
        pointsA2.add(new Point(1, 4));
        pointsA2.add(new Point(3, 4));
        pointsA2.add(new Point(3, 1));
        pointsB2.add(new Point(4, 2));
        pointsB2.add(new Point(4, 3));
        pointsB2.add(new Point(5, 3));
        pointsB2.add(new Point(5, 5));
        pointsB2.add(new Point(7, 5));
        pointsB2.add(new Point(7, 2));

        Polygon<Point> gonA2 = new Polygon<>(pointsA2, focalLength);
        Polygon<Point> gonB2 = new Polygon<>(pointsB2, focalLength);

        //Polygon unitedGon2 = Polygon.unitePolygons(gonA2, gonB2);
        //Polygon unitedGon22 = gonB2.uniteWith(gonA2, 3);

        int u = 0;
    }
}
package main;

import polygons.Polygon;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.awt.Color.BLACK;
import static polygons.Polygon.drawPolygons;

/*

http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.

 */

class Main {


    private static void f() throws IOException {
        List<List<String>> rawTable = Range.extractRawTable(Range.FILENAME);
        List<List<String>> table = Range.extractTable(rawTable);
        int[][] array = Range.findIf(table, num -> num > Range.T_MIN);
        List<Integer[]> ranges = Range.findRanges(array);
        ranges.removeIf(range -> Range.squarePixels(range) < Range.MIN_SQUARE_PIXELS);

        System.out.println(Arrays.deepToString(ranges.toArray()) + "\n" + ranges.size() + "\n");

        List<Polygon> polygons = Polygon.convertRanges(ranges);
        drawPolygons(polygons, BLACK, Range.PICTURENAME, Range.NEW_PICTURENAME_1);


        List<Polygon> newPolygons1 = Polygon.toBiggerPolygons(polygons, 5);
        System.out.println(newPolygons1.size());
        drawPolygons(newPolygons1, BLACK, Range.PICTURENAME, Range.NEW_PICTURENAME_2);

        List<Polygon> newPolygons2 = Polygon.toBiggerPolygons(newPolygons1, 5);
        System.out.println(newPolygons2.size());
        drawPolygons(newPolygons2, BLACK, Range.PICTURENAME, Range.NEW_PICTURENAME_2);

        List<Polygon> newPolygons3 = Polygon.toBiggerPolygons(newPolygons2, 5);
        System.out.println(newPolygons3.size());
        drawPolygons(newPolygons3, BLACK, Range.PICTURENAME, Range.NEW_PICTURENAME_2);

        List<Polygon> newPolygons4 = Polygon.toBiggerPolygons(newPolygons3, 5);
        System.out.println(newPolygons4.size());
        drawPolygons(newPolygons4, BLACK, Range.PICTURENAME, Range.NEW_PICTURENAME_2);
    }

    public static void main(String[] args) {
        try {
            f();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
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
        int[][] tableBin = Range.findIf(table, num -> num > Range.T_MIN);
        Range.printTable(tableBin);
        List<Integer[]> ranges = Range.findRanges(tableBin);
        ranges.removeIf(range -> Range.squarePixels(range) < Range.MIN_SQUARE_PIXELS);

        System.out.println(Arrays.deepToString(ranges.toArray()) + "\n" + ranges.size() + "\n");

        List<Polygon> polygons = Polygon.convertRanges(ranges);
        drawPolygons(polygons, BLACK, Range.PICTURENAME, Range.NEW_PICTURENAME_1);


        drawPolygons(Polygon.enlargeIteratively(polygons, 5), BLACK, Range.PICTURENAME, Range.NEW_PICTURENAME_2);
    }

    public static void main(String[] args) {
        try {
            f();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
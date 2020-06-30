package main;

import javenue.csv.Csv;
import polygons.Polygon;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.Color.BLACK;
import static polygons.Polygon.drawPolygons;

/*

http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.

 */

class Main {
    private static final String PICTURENAME = Range.DIR + "/picture.jpg";
    private static final String NEW_PICTURENAME_1 = Range.DIR + "/picture2_1.jpg";
    private static final String NEW_PICTURENAME_2 = Range.DIR + "/picture2_2.jpg";
    private static final double T_MIN = 30;
    private static final double T_MAX = 100;
    private static final int HEIGHT = 250;
    private static final int RES_X = 640;
    private static final int RES_Y = 512;
    private static final int MIN_SQUARE_PIXELS = 25;

    private static List<Integer> abc(int[] arr) {
        List<Integer> line = new ArrayList<>();
        for (int a : arr) {
            line.add(a);
        }
        return line;
    }

    private static List<List<Integer>> arrayToList(int[][] arr) {
        List<List<Integer>> table = new ArrayList<>();
        for (int[] f : arr)
            table.add(abc(f));
        return table;
    }

    private static void f() throws IOException {
        List<List<String>> rawTable = Range.extractRawTable(Range.FILENAME);
        List<List<String>> table = Range.extractTable(rawTable);
        int[][] arr = Range.findIf(table, num -> num > T_MIN);
        List<Integer[]> ranges = Range.findRanges(arrayToList(arr));
        ranges.removeIf(range -> Range.squarePixels(range) < MIN_SQUARE_PIXELS);

        System.out.println(Arrays.deepToString(ranges.toArray()) + "\n" + ranges.size() + "\n");

        List<Polygon> polygons = Polygon.convertRanges(ranges);
        drawPolygons(polygons, BLACK, PICTURENAME, NEW_PICTURENAME_1);


        List<Polygon> newPolygons1 = Polygon.toBiggerPolygons(polygons, 5);
        System.out.println(newPolygons1.size());
        drawPolygons(newPolygons1, BLACK, PICTURENAME, NEW_PICTURENAME_2);

        List<Polygon> newPolygons2 = Polygon.toBiggerPolygons(newPolygons1, 5);
        System.out.println(newPolygons2.size());
        drawPolygons(newPolygons2, BLACK, PICTURENAME, NEW_PICTURENAME_2);

        List<Polygon> newPolygons3 = Polygon.toBiggerPolygons(newPolygons2, 5);
        System.out.println(newPolygons3.size());
        drawPolygons(newPolygons3, BLACK, PICTURENAME, NEW_PICTURENAME_2);

        List<Polygon> newPolygons4 = Polygon.toBiggerPolygons(newPolygons3, 5);
        System.out.println(newPolygons4.size());
        drawPolygons(newPolygons4, BLACK, PICTURENAME, NEW_PICTURENAME_2);
    }

    public static void main(String[] args) {
        try {
            f();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
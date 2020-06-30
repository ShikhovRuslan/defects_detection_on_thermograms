package main;

import javenue.csv.Csv;
import polygons.Polygon;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.Color.BLACK;
import static polygons.Polygon.drawPolygons;

/*

http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.

 */

class Main {
    private static final String DIR = "/home/ruslan/geo";
    private static final String FILENAME = DIR + "/file.txt";
    private static final String PICTURENAME = DIR + "/picture.jpg";
    private static final String NEW_PICTURENAME_1 = DIR + "/picture2_1.jpg";
    private static final String NEW_PICTURENAME_2 = DIR + "/picture2_2.jpg";
    private static final double T_MIN = 30;
    private static final double T_MAX = 100;
    private static final int HEIGHT = 250;
    private static final int RES_X = 640;
    private static final int RES_Y = 512;
    private static final int MIN_SQUARE_PIXELS = 25;

    private static void printTable(List<List<String>> table) {
        for (List<String> line : table) {
            System.out.println(line);
        }
    }

    private static void printTable(int[][] arr) {
        for (int[] ints : arr) {
            for (int num : ints)
                System.out.print(num);
            System.out.println();
        }
    }

    private static List<List<String>> extractTable(List<List<String>> rawTable) {
        Pattern pattern = Pattern.compile("-?\\d{1,2},\\d{1,3}");
        Matcher matcher;
        List<List<String>> table = new ArrayList<>();
        int fromIndex = 0;
        int count = 0;
        boolean rightLine = false;
        boolean found;
        for (List<String> line : rawTable) {
            if (line != null) {
                for (int i = 0; i < line.size(); i++) {
                    matcher = pattern.matcher(line.get(i));
                    found = matcher.find();
                    if (found) count++;
                    if (count == 1) fromIndex = i;
                    if (!found && (i == fromIndex + 1 || i == fromIndex + 2)) break; // !
                    if (count >= 3) {
                        rightLine = true;
                        break;
                    }
                }
                for (String s : line.subList(fromIndex, line.size()))
                    if (s.equals("")) {
                        rightLine = false;
                        break;
                    }
                if (rightLine) table.add(line.subList(fromIndex, line.size()));
                count = 0;
                rightLine = false;
            }
        }
        return table;
    }

    private static List<List<String>> extractRawTable(String filename) throws FileNotFoundException {
        Csv.Reader reader = new Csv.Reader(new FileReader(FILENAME))
                .delimiter(';').ignoreComments(true);
        List<List<String>> rawTable = new ArrayList<>();
        List<String> line;
        do {
            line = reader.readLine();
            rawTable.add(line);
        } while (line != null);
        return rawTable;
    }

    private static int[][] findIf(List<List<String>> table, Predicate<Double> predicate) {
        int[][] arr = new int[table.size()][table.get(0).size()];
        for (int i = 0; i < table.size(); i++)
            for (int j = 0; j < table.get(i).size(); j++)
                if (predicate.test(new Double(table.get(i).get(j).replace(',', '.')))) arr[i][j] = 1;
        return arr;
    }

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

    private static int squarePixels(Integer[] range) {
        return (range[2] - range[0] + 1) * (range[3] - range[1] + 1);
    }

    private static void f() throws IOException {
        List<List<String>> rawTable = extractRawTable(FILENAME);
        List<List<String>> table = extractTable(rawTable);
        int[][] arr = findIf(table, num -> num > T_MIN);
        List<Integer[]> ranges = Range.findRanges(arrayToList(arr));
        ranges.removeIf(range -> squarePixels(range) < MIN_SQUARE_PIXELS);

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
package main;

import polygons.Polygon;

import java.awt.*;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

/*

http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.

 */

public class Main {
    private final static String NEW_PICTURENAME = "new_picture.jpg";

    private static void process() throws FileNotFoundException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Введите полное имя снимка: ");
        String pictureName = sc.nextLine();
        System.out.print("Введите полное имя файла с таблицей: ");
        String fileName = sc.nextLine();

        char ch = 0;
        if (System.getProperty("os.name").contains("Linux"))
            ch = '/';
        if (System.getProperty("os.name").contains("Windows"))
            ch = '\\';
        String newPictureName = pictureName.substring(0, pictureName.lastIndexOf(ch) + 1) + NEW_PICTURENAME;
        System.out.println("\nФайл с выделенными дефектами: " + newPictureName + "\n");

        List<List<String>> rawTable = Range.extractRawTable(fileName);
        List<List<String>> table = Range.extractTable(rawTable);
        int[][] tableBin = Range.findIf(table, num -> num > Range.T_MIN);
        List<Integer[]> ranges = Range.findRanges(tableBin);
        ranges.removeIf(range -> Range.squarePixels(range) < Range.MIN_SQUARE_PIXELS);

        List<Polygon> polygons = Polygon.convertRanges(ranges);
        List<Polygon> enlargedPolygons = Polygon.enlargeIteratively(polygons, 5);
        Polygon.drawPolygons(enlargedPolygons, Color.BLACK, pictureName, newPictureName);
        Polygon.showSquaresPixels(enlargedPolygons);
    }

    public static void main(String[] args) {
        try {
            process();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
package main;

import java.awt.*;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Scanner;

import polygons.Point;

/*

http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.

 */

public class Main {
    private final static String NEW_PICTURENAME = "new_picture2.jpg";

    private static void process() throws FileNotFoundException {
        Scanner sc = new Scanner(System.in);
        System.out.print("Введите полное имя снимка: ");
        String pictureName = sc.nextLine();
        System.out.print("Введите полное имя файла с таблицей: ");
        String fileName = sc.nextLine();

        pictureName = "C:\\Users\\shikh\\Documents\\Geo\\DJI_0841_R.jpg";
        fileName = "C:\\Users\\shikh\\Documents\\Geo\\ui.csv";

        char ch = 0;
        if (System.getProperty("os.name").contains("Linux"))
            ch = '/';
        if (System.getProperty("os.name").contains("Windows"))
            ch = '\\';
        String newPictureName = pictureName.substring(0, pictureName.lastIndexOf(ch) + 1) + NEW_PICTURENAME;
        System.out.println("\nФайл с выделенными дефектами: " + newPictureName + "\n");

        List<List<String>> rawTable = Helper.extractRawTable(fileName);
        List<List<String>> table = Helper.extractTable(rawTable);
        int[][] tableBin = Helper.findIf(table, num -> num > Helper.T_MIN);
        List<Rectangle<Point>> ranges = Rectangle.findRectangles(tableBin);
        ranges.removeIf(range -> range.squarePixels() < Helper.MIN_SQUARE_PIXELS);


        double yaw837 = 109.6 - 90;
        double yaw841 = 110.4 - 90;
        double height837 = 152.2;
        double height841 = 152.3;
        com.grum.geocalc.Point groundNadir837 = com.grum.geocalc.Point.at(com.grum.geocalc.Coordinate.fromDMS(53, 46, 42.72), com.grum.geocalc.Coordinate.fromDMS(87, 15, 35.18));
        com.grum.geocalc.Point groundNadir841 = com.grum.geocalc.Point.at(com.grum.geocalc.Coordinate.fromDMS(53, 46, 42.41), com.grum.geocalc.Coordinate.fromDMS(87, 15, 33.89));

        Thermogram thermogram837 = new Thermogram(yaw837, height837, groundNadir837);
        Thermogram thermogram841 = new Thermogram(yaw841, height841, groundNadir841);
        Polygon<Pixel> overlap = thermogram841.getOverlapWith(thermogram837);
        System.out.println(overlap);


        List<Polygon<Point>> polygons = Polygon.convertRanges(ranges, overlap);
        List<Polygon<Point>> enlargedPolygons = Polygon.enlargeIteratively(polygons, 5, overlap);

        Polygon.drawPolygons(enlargedPolygons, Color.BLACK, pictureName, newPictureName);
        Polygon.showSquaresPixels(enlargedPolygons);
    }

    public static void main(String[] args) {
        try {
            process();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //System.out.println(new Segment(new Point(0,1),new Point(-1,9)));
    }
}
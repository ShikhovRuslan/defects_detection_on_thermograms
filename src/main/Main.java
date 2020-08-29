package main;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.grum.geocalc.Coordinate;
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
        int[][] tableBin = Helper.findIf(table, num -> num > Thermogram.T_MIN);
        List<Rectangle<Point>> ranges = Rectangle.findRectangles(tableBin);
        ranges.removeIf(range -> range.squarePixels() < Thermogram.MIN_PIXEL_SQUARE);


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


        List<Polygon<Point>> polygons = Polygon.toPolygons(ranges, overlap, thermogram841.getHeight());
        List<Polygon<Point>> enlargedPolygons = Polygon.enlargeIteratively(polygons, 5, overlap, thermogram841.getHeight());

        Polygon.drawPolygons(enlargedPolygons, Polygon.toPointPolygon(overlap), Color.BLACK, pictureName, newPictureName);
        Polygon.showSquares(enlargedPolygons, thermogram841.getHeight());
    }

    private static void process2() {
        System.out.println(Pixel.findIntersection(new Pixel(0, 0), new Pixel(3, 3), new Pixel(3, 3), new Pixel(5, 5)));

        double s1 = Thermogram.earthDistance(new Pixel(0, 0), new Pixel(Thermogram.RES_X - 1, 0), 152);
        double s2 = Thermogram.earthDistance(new Pixel(0, 0), new Pixel(0, Thermogram.RES_Y - 1), 152);
        System.out.println(Thermogram.Corners.C2.angle(new Pixel(484, 490)) + " " + s2);

        com.grum.geocalc.Point mE = com.grum.geocalc.Point.at(Coordinate.fromDMS(53, 46, 45.70), Coordinate.fromDMS(87, 15, 44.59));

        Pixel m = new Pixel(484, 490);


        Rectangle<Pixel> rectangle = new Rectangle<>(new Pixel(10, 10), new Pixel(100, 100));
        Polygon<Pixel> polygon = new Polygon<>(Arrays.asList(new Pixel(1, 1), new Pixel(1, 104), new Pixel(107, 108), new Pixel(101, 2)));
        System.out.println(Rectangle.getIntersection(rectangle, polygon));

        System.out.println(new Triangle<>(Arrays.asList(new Pixel(401, 85), new Pixel(403, 85), new Pixel(403, 102))).square());
        System.out.println(new Polygon<>(Arrays.asList(new Pixel(401, 85), new Pixel(403, 102))).square());
    }

    public static void main(String[] args) throws IOException {
        try {
            process();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        //System.out.println(new Segment(new Point(0,1),new Point(-1,9)));

        //System.out.println(new Triangle<>(Arrays.asList(new Pixel(401, 85), new Pixel(403, 85), new Pixel(403, 102))));
        //System.out.println(new Polygon<>(Arrays.asList(new Pixel(401, 85), new Pixel(403, 85), new Pixel(403, 102))));

//        Rectangle<Pixel> r1 = new Rectangle<>(new Pixel(0,0), new Pixel(8,9));
//        Rectangle<Point> r2 = new Rectangle<>(new Point(0,0), new Point(8,9));
//        Polygon<Pixel> ov = new Polygon<>(Arrays.asList(new Pixel(1,1), new Pixel(3,4),new Pixel(-1,2)));
//        System.out.println(Figure.toPolygon(r2, Rectangle.squareRectangleWithoutOverlap(Rectangle.toRectangle(r2), ov)));
//        System.out.println(Figure.toPolygon(r1, -1));

//        String pictureName = "C:\\Users\\shikh\\Documents\\Geo\\DJI_0841_R.jpg";
//        char ch = '\\';
//        String newPictureName = pictureName.substring(0, pictureName.lastIndexOf(ch) + 1) + NEW_PICTURENAME;
//        Segment s = new Segment(new Point(250, 250), new Point(302, 300));
//        BufferedImage image = ImageIO.read(new File(pictureName));
//        s.draw(image, Color.BLACK);
//        ImageIO.write(image, "jpg", new File(newPictureName));
    }
}
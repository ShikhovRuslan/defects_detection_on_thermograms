package main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.grum.geocalc.Coordinate;
import polygons.Point;

import java.awt.*;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


/**
 * http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.
 */
public class Main {
    private final static String NEW_PICTURENAME = "new_picture2.jpg";

    private final static String GLOBAL_PARAMS_FILENAME;
    private final static String THERMOGRAMS_INFO_FILENAME;

    private final static String CONFIG_SHORT_FILENAME = "config.txt";
    private final static String GLOBAL_PARAMS_SHORT_FILENAME = "global_params.txt";
    private final static String THERMOGRAMS_INFO_SHORT_FILENAME = "thermograms_info.txt";

    private final static String THERMOGRAMS_DIR_PROPERTY = "THERMOGRAMS_DIR";
    private final static String OUTPUT_SUBDIR_PROPERTY = "OUTPUT_SUBDIR";
    private final static String RAW_SUBDIR_PROPERTY = "RAW_SUBDIR";

    private final static String THERMOGRAMS_DIR;
    private final static String RAW_DIR;

    private final static String PREFIX = "_raw.pgm";

    private final static String NEW_PICTURES_DIR;

    static {
        String currentDir = "";
        try {
            currentDir = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        currentDir = currentDir.substring(0, currentDir.lastIndexOf('/'));

        File configFile = new File(currentDir + "/" + CONFIG_SHORT_FILENAME);

        String outputSubdir = "";
        try {
            Scanner sc = new Scanner(configFile);
            String nextLine;
            while (sc.hasNextLine()) {
                nextLine = sc.nextLine();
                if (nextLine.matches(OUTPUT_SUBDIR_PROPERTY + ".*"))
                    outputSubdir = nextLine.substring(nextLine.indexOf('=') + 2);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String thermogramsDir = "";
        try {
            Scanner sc = new Scanner(configFile);
            String nextLine;
            while (sc.hasNextLine()) {
                nextLine = sc.nextLine();
                if (nextLine.matches(THERMOGRAMS_DIR_PROPERTY + ".*"))
                    thermogramsDir = nextLine.substring(nextLine.indexOf('=') + 2);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String rawSubdir = "";
        try {
            Scanner sc = new Scanner(configFile);
            String nextLine;
            while (sc.hasNextLine()) {
                nextLine = sc.nextLine();
                if (nextLine.matches(RAW_SUBDIR_PROPERTY + ".*"))
                    rawSubdir = nextLine.substring(nextLine.indexOf('=') + 2);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        GLOBAL_PARAMS_FILENAME = currentDir + "/" + outputSubdir + "/" + GLOBAL_PARAMS_SHORT_FILENAME;
        THERMOGRAMS_INFO_FILENAME = currentDir + "/" + outputSubdir + "/" + THERMOGRAMS_INFO_SHORT_FILENAME;
        THERMOGRAMS_DIR = "/" + thermogramsDir.replace('\\', '/');
        RAW_DIR = currentDir + "/" + rawSubdir;
        NEW_PICTURES_DIR = currentDir + "/" + "new_pictures";
    }

    private static void process(Thermogram current, Thermogram previous) {
        String pictureName = THERMOGRAMS_DIR + "/" + current.getName() + ".jpg";
        String rawFile = RAW_DIR + "/" + current.getName() + PREFIX;
        String newPictureName = NEW_PICTURES_DIR + "/" + current.getName() + ".jpg";

        System.out.println("\nФайл с выделенными дефектами: " + newPictureName + "\n");

        int[][] rawTable = New.read(rawFile,
                (int) New.getParam(GLOBAL_PARAMS_FILENAME, New.Param.RAW_THERMAL_IMAGE_HEIGHT),
                (int) New.getParam(GLOBAL_PARAMS_FILENAME, New.Param.RAW_THERMAL_IMAGE_WIDTH));
        double[][] temperatureTable = New.convertTable(rawTable, New.getParams(GLOBAL_PARAMS_FILENAME));

        int[][] binTable = Helper.findIf(temperatureTable, num -> num > Thermogram.T_MIN);
        List<Rectangle<Point>> ranges = Rectangle.findRectangles(binTable);
        ranges.removeIf(range -> range.squarePixels() < Thermogram.MIN_PIXEL_SQUARE);

        Polygon<Pixel> overlap = current.getOverlapWith(previous);
        System.out.println(overlap);

        List<Polygon<Point>> polygons = Polygon.toPolygons(ranges, overlap, current.getHeight());
        List<Polygon<Point>> enlargedPolygons = Polygon.enlargeIteratively(polygons, 5, overlap, current.getHeight());

        Polygon.drawPolygons(enlargedPolygons, Polygon.toPointPolygon(overlap), Color.BLACK, pictureName, newPictureName);
        Polygon.showSquares(enlargedPolygons, current.getHeight());
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

    private static Thermogram[] readThermograms(String filename) throws FileNotFoundException {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Thermogram.class, new ThermogramDeserializer())
                .create();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
        return gson.fromJson(bufferedReader, Thermogram[].class);
    }

    public static void main(String[] args) throws FileNotFoundException {
        Thermogram[] thermograms = readThermograms(THERMOGRAMS_INFO_FILENAME);

        process(thermograms[4], thermograms[3]);
    }
}
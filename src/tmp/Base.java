package tmp;

import main.*;
import polygons.Point;
import polygons.Segment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public class Base {
    public static Segment getSegment(Pixel centre, double angle, double length, int resY) {
        Pixel a = new Pixel(centre.getI() + 0.5 * length * Math.sin(angle),
                centre.getJ() + 0.5 * length * Math.cos(angle));
        Pixel b = new Pixel(centre.getI() - 0.5 * length * Math.sin(angle),
                centre.getJ() - 0.5 * length * Math.cos(angle));
        return new Segment(Point.toPoint(a, resY), Point.toPoint(b, resY));
    }

    public static double av(Segment s, double[][] table, int resX) {
        double a = (s.getB().getJ() - s.getA().getJ()) / (s.getB().getI() - s.getA().getI() + 0.);
        double b = s.getA().getJ() - a * s.getA().getI();
        double sum = 0;
        int j;
        int n = 0; // число исключённых элементов
        for (int i = Math.min(s.getA().getI(), s.getB().getI()); i <= Math.max(s.getA().getI(), s.getB().getI()); i++) {
            j = (int) Math.round(a * i + b);
            if (j >= resX)
                n++;
            else {
                if (table[i][j] < 0)
                sum += table[i][j];
                else
                    sum+=0;
            }
        }
        return sum / (Math.max(s.getA().getI(), s.getB().getI()) - Math.min(s.getA().getI(), s.getB().getI()) + 1 - n);
    }

    public static double f(double diameter, Pixel pixel, int n, double coef, char separator, String filename, int resX, int resY) {
        double[][] realTable = Helper.extractTable(filename, separator);
        double length = coef * diameter;
        int k = -1;
        double max = -1000;
        double val;
        for (int i = 0; i < n; i++) {
            val = av(getSegment(pixel, (i / (n + 0.)) * Math.PI, length, resY), realTable, resX);
            //System.out.println("   " + i + " " + val);
            if (max < val) {
                max = val;
                k = i;
            }
        }
        System.out.println("--- k = " + k);
        return (k / (n + 0.)) * 180;
    }

    public static Rectangle<Pixel> boundingRectangle(Polygon<Pixel> polygon) {
        int[] i = dffd(polygon, Pixel::getI);
        int[] j = dffd(polygon, Pixel::getJ);
        return new Rectangle<>(new Pixel(i[1], j[1]), new Pixel(i[0], j[0]));
    }

    public static double[] angles(Pixel v1, Pixel v2, double diameter, int n, double coef, char separator, String filename, int resX, int resY) {
        double a1 = f(diameter, v1, n, coef, separator, filename, resX, resY);
        double a2 = f(diameter, v2, n, coef, separator, filename, resX, resY);
        double gamma = -1000;
        if (v1.getJ() == v2.getJ())
            if (a1 > 90 && a2 < 90 || a1 < 90 && a2 > 90)
                gamma = 180 - Math.abs(a1 - a2);
            else
                gamma = Math.abs(a1 - a2);
        if (v1.getI() == v2.getI())
            gamma = Math.abs(a1 - a2);
        return new double[]{a1, a2, gamma};
    }

    public static double bisector(double a1, double a2, boolean isHorisontal) {
//        double a1 = f(diameter, v1, n, coef, separator, filename);
//        double a2 = f(diameter, v2, n, coef, separator, filename);
//        double gamma;
//        if (a1 > 90 && a2 < 90 || a1 < 90 && a2 > 90)
//            gamma = 180 - Math.abs(a1 - a2);
//        else
//            gamma = Math.abs(a1 - a2);
//        if (gamma >= 90)
//            return -1000;
        double b = -1000;
        if (isHorisontal) {
            if (a1 > 90 && a2 < 90)
                b = -90 + (a1 + a2) / 2;
            if (a1 < 90 && a2 > 90)
                b = 90 + (a1 + a2 < 180 ? a1 + a2 : -a1 - a2) / 2;
            if (a1 > 90 && a2 > 90 || a1 < 90 && a2 < 90)
                b = (a1 + a2) / 2;
        }
        else
            b = (a1 + a2) / 2;
        System.out.println("a1 = " + a1 + ", a2 = " + a2 + ", b = " + b);
        return b;
    }

    public static double vert(Rectangle<Pixel> rectangle,
                              double diameter, int n, double coef, char separator, String filename, int resX, int resY) {
        // по час. стрелке, начиная с верхней левой
        Pixel v1 = new Pixel(rectangle.getLeft().getI(), rectangle.getRight().getJ());
        Pixel v2 = rectangle.getRight();
        Pixel v3 = new Pixel(rectangle.getRight().getI(), rectangle.getLeft().getJ());
        Pixel v4 = rectangle.getLeft();

        double[] anglesUpperSide = angles(v1, v2, diameter, n, coef, separator, filename, resX, resY);
        double[] anglesLowerSide = angles(v4, v3, diameter, n, coef, separator, filename, resX, resY);
        if (anglesUpperSide[2] <= 90 && anglesLowerSide[2] >= 90 || anglesUpperSide[2] >= 90 && anglesLowerSide[2] <= 90)
            System.out.println("Gammas don't relate.");

        if (anglesUpperSide[2] <= 90) {
            double bUpper = bisector(anglesUpperSide[0], anglesUpperSide[1], true);
            double bLower = bisector(anglesLowerSide[0], anglesLowerSide[1], true);
            double[] newB = toMinus(bUpper, bLower);
            System.out.println("g1 = " + anglesUpperSide[2]);
            System.out.println("g2 = " + anglesLowerSide[2]);
            return (newB[0] + newB[1]) / 2;
        }

        double[] anglesLeftSide = angles(v4, v1, diameter, n, coef, separator, filename, resX, resY);
        double[] anglesRightSide = angles(v3, v2, diameter, n, coef, separator, filename, resX, resY);
        if (anglesLeftSide[2] <= 90 && anglesRightSide[2] >= 90 || anglesLeftSide[2] >= 90 && anglesRightSide[2] <= 90)
            System.out.println("Gammas don't relate.");

        if (anglesLeftSide[2] <= 90) {
            double bLeft = bisector(anglesLeftSide[0], anglesLeftSide[1], false);
            double bRight = bisector(anglesRightSide[0], anglesRightSide[1], false);
            System.out.println("g1 = " + anglesLeftSide[2]);
            System.out.println("g2 = " + anglesRightSide[2]);
            return (bLeft + bRight) / 2;
        }

        throw new IllegalArgumentException("end");
    }

    public static double[] toMinus(double... vals) {
        double[] newVals = new double[vals.length];
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] < 90)
                newVals[i] = vals[i];
            if (vals[i] > 90)
                newVals[i] = vals[i] - 180;
        }
        return newVals;
    }

    public static double realToMatrix(double distance, double height, double pixelSize, double focalLength) {
        return distance / Thermogram.reverseScale(height, focalLength) / pixelSize;
    }

    public static double sss(double diameter, int n, double coef, char separator, String filename, int resX, int resY, Pixel... pixels) {
        double sum = 0;
        for (Pixel pixel : pixels)
            sum += f(diameter, pixel, n, coef, separator, filename, resX, resY);
        return sum / pixels.length;
    }

    public static int[] dffd(Polygon<Pixel> polygon, Function<Pixel, Integer> s) {
        int max = -1;
        int min = 1000;
        for (Pixel vertex : polygon.getVertices()) {
            if (s.apply(vertex) > max)
                max = s.apply(vertex);
            if (s.apply(vertex) < min)
                min = s.apply(vertex);
        }
        return new int[]{max, min};
    }

    public static int width(Polygon<Pixel> polygon) {
        int[] i = dffd(polygon, Pixel::getI);
        return i[0] - i[1] + 1;
    }

    public static int height(Polygon<Pixel> polygon) {
        int[] j = dffd(polygon, Pixel::getJ);
        return j[0] - j[1] + 1;
    }

    public static boolean procedureNotNeeded(int diameterPixel, Polygon<Pixel> polygon) {
        double coef = 1.33;
        return width(polygon) >= diameterPixel && width(polygon) <= diameterPixel * coef && height(polygon) > diameterPixel ||
                height(polygon) >= diameterPixel && height(polygon) <= diameterPixel * coef && width(polygon) > diameterPixel;
    }

    public static Pixel toPixel(Point point, int resY) {
        return new Pixel(point.getJ(), resY - 1 - point.getI());
    }

    public static Polygon<Pixel> toPixelPolygon(Polygon<Point> polygon, double focalLength, int resY) {
        List<Pixel> vertices = new ArrayList<>();
        for (Point vertex : polygon.getVertices())
            vertices.add(toPixel(vertex, resY));
        return new Polygon<>(vertices, focalLength);
    }

    public static Pixel _aaa(Pixel pixel, String filename, char separator, double diameter, double height, double focalLength, int resY) {
        double[][] table = Helper.extractTable(filename, separator);
        int pixelDiameter = (int) Math.round(realToMatrix(diameter, height, Main.PIXEL_SIZE, focalLength));
        int length = (int) Math.round(1.5 * pixelDiameter);
        List<Pixel> arr = new ArrayList<>();
        System.out.println(length);
//        for(int j = pixel.getJ() + 1; j <= pixel.getJ() + length - 1; j++) {
//            System.out.println(table[Main.RES_Y - 1 - (j - 1)][pixel.getI()]);
//            if(table[Main.RES_Y - 1 - (j - 1)][pixel.getI()] - table[Main.RES_Y - 1 - j][pixel.getI()] >= 1.5){
//                arr.add(new Pixel(pixel.getI(), j));
//            }
//        }
        for(int j = pixel.getJ() - 1; j >= pixel.getJ() - length + 1; j--) {
            System.out.println(table[resY - 1 - (j - 1)][pixel.getI()]);
            if(table[resY - 1 - j][pixel.getI()] - table[resY - 1 - (j - 1)][pixel.getI()] >= 1.5){
                arr.add(new Pixel(pixel.getI(), j));
            }
        }
        if(arr.size()>0)
            return arr.get(arr.size() - 1);
        else {
            System.out.println("---");
            return new Pixel(-1, -1);
        }
    }

    public static void main(String[] args) {
        int n = 48;
        double coef = 4;
        double diameter = 0.7;
        double height = 152;
        double focalLength = 25./1000;
        int resY = 512;
        int resX = 640;
        String filename;
        Pixel pixel1, pixel2, pixel3, pixel4;
        double diameterPixel = realToMatrix(diameter, height, Main.PIXEL_SIZE, focalLength);

        // 829 thermogram
        filename = Main.DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT_REAL.getValue() +
                "/" + "DJI_0829_R" + Property.POSTFIX_REAL.getValue() + Main.EXTENSION_REAL;
        // 379,190  Paint
        pixel1 = new Pixel(375, resY - 1 - 188);
        pixel2 = new Pixel(382, resY - 1 - 188);
        pixel3 = new Pixel(382, resY - 1 - 192);
        pixel4 = new Pixel(375, resY - 1 - 192);
        //System.out.println(f(realToMatrix(diameter, height, Main.PIXEL_SIZE), pixel1, n, coef, Main.SEPARATOR_REAL, filename));
        //System.out.println(f(realToMatrix(diameter, height, Main.PIXEL_SIZE), pixel2, n, coef, Main.SEPARATOR_REAL, filename));
        //System.out.println(f(realToMatrix(diameter, height, Main.PIXEL_SIZE), pixel3, n, coef, Main.SEPARATOR_REAL, filename));
        //System.out.println(f(realToMatrix(diameter, height, Main.PIXEL_SIZE), pixel4, n, coef, Main.SEPARATOR_REAL, filename));
        System.out.println(sss(diameterPixel, n, coef, Main.SEPARATOR_REAL, filename, resX, resY,
                pixel1, pixel2, pixel3, pixel4));

        // 817 thermogram
        filename = Main.DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT_REAL.getValue() +
                "/" + "DJI_0817_R" + Property.POSTFIX_REAL.getValue() + Main.EXTENSION_REAL;
//        pixel1 = new Pixel(426, Main.RES_Y - 1 - 37);
//        pixel2 = new Pixel(430, Main.RES_Y - 1 - 37);
//        pixel3 = new Pixel(430, Main.RES_Y - 1 - 41);
//        pixel4 = new Pixel(426, Main.RES_Y - 1 - 41);
//        pixel1 = new Pixel(457, Main.RES_Y - 1 - 38);
//        pixel2 = new Pixel(464, Main.RES_Y - 1 - 38);
//        pixel3 = new Pixel(464, Main.RES_Y - 1 - 43);
//        pixel4 = new Pixel(457, Main.RES_Y - 1 - 43);
        pixel1 = new Pixel(380, resY - 1 - 150);
        pixel2 = new Pixel(387, resY - 1 - 150);
        pixel3 = new Pixel(387, resY - 1 - 154);
        pixel4 = new Pixel(380, resY - 1 - 154);
        System.out.println(sss(diameterPixel, n, coef, Main.SEPARATOR_REAL, filename, resX, resY,
                pixel1, pixel2, pixel3, pixel4));
    }
}
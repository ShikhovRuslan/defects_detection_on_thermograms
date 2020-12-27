package tmp;

import main.*;
import polygons.Point;
import polygons.Segment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.*;


public class Base {
    public static Segment getSegment(Pixel centre, double angle, double length, int resY) {
        Pixel a = new Pixel(centre.getI() + 0.5 * length * sin(angle),
                centre.getJ() + 0.5 * length * cos(angle));
        Pixel b = new Pixel(centre.getI() - 0.5 * length * sin(angle),
                centre.getJ() - 0.5 * length * cos(angle));
        return new Segment(a.toPoint(resY), b.toPoint(resY));
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
                    sum += 0;
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

    public static double[] angles(Pixel v1, Pixel v2, double diameter, int n, double coef, char separator, String filename, int resX, int resY) {
        double a1 = f(diameter, v1, n, coef, separator, filename, resX, resY);
        double a2 = f(diameter, v2, n, coef, separator, filename, resX, resY);
        double gamma = -1000;
        if (v1.getJ() == v2.getJ())
            if (a1 > 90 && a2 < 90 || a1 < 90 && a2 > 90)
                gamma = 180 - abs(a1 - a2);
            else
                gamma = abs(a1 - a2);
        if (v1.getI() == v2.getI())
            gamma = abs(a1 - a2);
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
        } else
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

    public static boolean procedureNotNeeded(int diameterPixel, Polygon<Pixel> polygon) {
        double coef = 1.33;
        return width(polygon) >= diameterPixel && width(polygon) <= diameterPixel * coef && height(polygon) > diameterPixel ||
                height(polygon) >= diameterPixel && height(polygon) <= diameterPixel * coef && width(polygon) > diameterPixel;
    }

    public static double sss(double diameter, int n, double coef, char separator, String filename, int resX, int resY, Pixel... pixels) {
        double sum = 0;
        for (Pixel pixel : pixels)
            sum += f(diameter, pixel, n, coef, separator, filename, resX, resY);
        return sum / pixels.length;
    }

    public static Rectangle<Pixel> boundingRectangle(Polygon<Pixel> polygon) {
        int[] i = findMaxAndMin(polygon, Pixel::getI);
        int[] j = findMaxAndMin(polygon, Pixel::getJ);
        return new Rectangle<>(new Pixel(i[1], j[1]), new Pixel(i[0], j[0]));
    }

    public static double realToMatrix(double distance, double height, double pixelSize, double focalLength) {
        return distance / Thermogram.reverseScale(height, focalLength) / pixelSize;
    }

    public static int[] getMinAndMax(Pixel[] pixels, List<Integer> indices, Function<Pixel, Integer> f) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i : indices) {
            if (f.apply(pixels[i]) < min)
                min = f.apply(pixels[i]);
            if (f.apply(pixels[i]) > max)
                max = f.apply(pixels[i]);
        }
        return new int[]{min, max};
    }

    public static int[] findMaxAndMin(Polygon<Pixel> polygon, Function<Pixel, Integer> f) {
        int max = -1;
        int min = 1000;
        for (Pixel vertex : polygon.getVertices()) {
            if (f.apply(vertex) > max)
                max = f.apply(vertex);
            if (f.apply(vertex) < min)
                min = f.apply(vertex);
        }
        return new int[]{max, min};
    }

    public static int width(Polygon<Pixel> polygon) {
        int[] i = findMaxAndMin(polygon, Pixel::getI);
        return i[0] - i[1] + 1;
    }

    public static int height(Polygon<Pixel> polygon) {
        int[] j = findMaxAndMin(polygon, Pixel::getJ);
        return j[0] - j[1] + 1;
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

    public static Pixel middle(Rectangle<Pixel> rectangle) {
        return new Pixel((rectangle.getLeft().getI() + rectangle.getRight().getI()) / 2,
                (rectangle.getLeft().getJ() + rectangle.getRight().getJ()) / 2);
    }

    public static boolean include(Rectangle<Pixel> rectangle, Rectangle<Pixel> big) {
        //return rectangle.getLeft().getI() >= big.getLeft().getI() && rectangle.getLeft().getJ() >= big.getLeft().getJ() &&
        //       rectangle.getRight().getI() <= big.getRight().getI() && rectangle.getRight().getJ() <= big.getRight().getJ();
        int leftIDiff = rectangle.getLeft().getI() - big.getLeft().getI();
        int leftJDiff = rectangle.getLeft().getJ() - big.getLeft().getJ();
        int rightIDiff = -rectangle.getRight().getI() + big.getRight().getI();
        int rightJDiff = -rectangle.getRight().getJ() + big.getRight().getJ();

        int maxDiff = 3;

        return leftIDiff >= -maxDiff && leftJDiff >= -maxDiff && rightIDiff >= -maxDiff && rightJDiff >= -maxDiff;
    }

    public static Object[] findJump(Pixel start, double angle, double length, double tempDiff, String filename,
                                    char separator, double height, double pixelSize, double focalLength,
                                    int resX, int resY) {

        double[][] realTable = Helper.extractTable(filename, separator);
        List<Pixel> jumps = new ArrayList<>();
        List<Double> temperatures = new ArrayList<>();
        int pI = start.getI();
        int pJ = start.getJ();

        int iInc = (int) Math.round(realToMatrix(length * sin(angle * PI / 180), height, pixelSize, focalLength));
        int jInc = (int) Math.round(realToMatrix(length * cos(angle * PI / 180), height, pixelSize, focalLength));

        int iIncSign = (int) signum(iInc);
        int jIncSign = (int) signum(jInc);

        Pixel end = new Pixel(
                pI + iInc >= 0 && pI + iInc < resX ? pI + iInc : (pI + iInc < 0 ? 0 : resX - 1),
                pJ + jInc >= 0 && pJ + jInc < resY ? pJ + jInc : (pJ + jInc < 0 ? 0 : resY - 1));

        int eI = end.getI();
        int eJ = end.getJ();

        int jPrev = -1, iPrev = -1;
        if (iIncSign != 0 || jIncSign != 0) {
            if (abs(eI - pI) >= abs(eJ - pJ))
                for (int i = pI + iIncSign; iIncSign > 0 ? i <= eI : i >= eI; i = i + iIncSign) {
                    int j = (int) round((pJ + jIncSign) +
                            (i - (pI + iIncSign) + 0.) * (eJ - (pJ + jIncSign)) / (eI - (pI + iIncSign)));
                    temperatures.add(realTable[resY - 1 - j][i]);
                    if (i != pI + iIncSign &&
                            abs(realTable[resY - 1 - j][i] - realTable[resY - 1 - jPrev][i - iIncSign]) >= tempDiff)
                        jumps.add(new Pixel(i, j));
                    jPrev = j;
                }
            else
                for (int j = pJ + jIncSign; jIncSign > 0 ? j <= eJ : j >= eJ; j = j + jIncSign) {
                    int i = (int) round((pI + iIncSign) +
                            (j - (pJ + jIncSign) + 0.) * (eI - (pI + iIncSign)) / (eJ - (pJ + jIncSign)));
                    temperatures.add(realTable[resY - 1 - j][i]);
                    if (j != pJ + jIncSign &&
                            abs(realTable[resY - 1 - j][i] - realTable[resY - 1 - (j - jIncSign)][iPrev]) >= tempDiff)
                        jumps.add(new Pixel(i, j));
                    iPrev = i;
                }
            if (jumps.size() == 0) return new Object[]{new Pixel(-1, -1), 0., angle};
        } else return new Object[]{new Pixel(-2, -2), 0., angle};

        double averageEndTemperature = 0;
        int n = 2;
        for (int i = 0; i < min(n, temperatures.size()); i++)
            averageEndTemperature += temperatures.get(temperatures.size() - 1 - i);
        averageEndTemperature = averageEndTemperature / min(n, temperatures.size());

        return new Object[]{jumps.get(jumps.size() - 1), averageEndTemperature, angle};
    }

    public static double sq(Polygon<Pixel> polygon, double height, double focalLength, double pixelSize) {
        List<Pixel> v = polygon.getVertices();
        return Thermogram.earthDistance(v.get(0), v.get(1), height, focalLength, pixelSize) *
                Thermogram.earthDistance(v.get(1), v.get(2), height, focalLength, pixelSize);
    }

    int f(List<Integer> list) {
        int ind;
        try {
            ind = Helper.findIndexOfMin(list);
        } catch (IllegalArgumentException e) {
            return 0;
        }
        return ind + 1000;
    }

    public static <T> T unsafe(T elements) {
        return elements; // unsafe! don't ever return a parameterized varargs array
    }

    public static <T> T broken(T seed) {
        T plant = unsafe(seed); // broken! This will be an Object[] no matter what T is
        return plant;
    }

    public static void shorten(Polygon<Pixel> polygon, double shift, String sideToShift, double pipeAngle) {
        pipeAngle *= PI / 180;
        Pixel[] v = polygon.getVertices().toArray(new Pixel[0]);

        if (pipeAngle < PI / 2) {
            if (sideToShift.equals("01")) {
                v[0] = (Pixel) v[0].create(v[0].getI() + shift * cos(pipeAngle), v[0].getJ() + shift * sin(pipeAngle));
                v[1] = (Pixel) v[1].create(v[1].getI() + shift * cos(pipeAngle), v[1].getJ() + shift * sin(pipeAngle));
            }
            if (sideToShift.equals("23")) {
                v[2] = (Pixel) v[2].create(v[2].getI() - shift * cos(pipeAngle), v[2].getJ() - shift * sin(pipeAngle));
                v[3] = (Pixel) v[3].create(v[3].getI() - shift * cos(pipeAngle), v[3].getJ() - shift * sin(pipeAngle));
            }
        } else {
            if (sideToShift.equals("03")) {
                v[0] = (Pixel) v[0].create(v[0].getI() - shift * cos(pipeAngle), v[0].getJ() - shift * sin(pipeAngle));
                v[3] = (Pixel) v[3].create(v[3].getI() - shift * cos(pipeAngle), v[3].getJ() - shift * sin(pipeAngle));
            }
            if (sideToShift.equals("12")) {
                v[1] = (Pixel) v[1].create(v[1].getI() + shift * cos(pipeAngle), v[1].getJ() + shift * sin(pipeAngle));
                v[2] = (Pixel) v[2].create(v[2].getI() + shift * cos(pipeAngle), v[2].getJ() + shift * sin(pipeAngle));
            }
        }

        for (int i = 0; i < polygon.getVertices().size(); i++)
            polygon.getVertices().set(i, v[i]);
    }

    public static double distance(Pixel p1, Pixel p2, Pixel p) {
        // Случай вертикальной прямой.
        if (p1.getI() == p2.getI())
            return abs(p.getI() - p1.getI());

        // y=a*x+b - уравнение прямой, проходящей через точки p1 и p2.
        double a = (p2.getJ() - p1.getJ()) / (p2.getI() - p1.getI() + 0.);
        double b = p1.getJ() - a * p1.getI();

        // Случай горизонтальной прямой.
        if (a == 0)
            return abs(p.getJ() - p1.getJ());

        // (x0,y0) - точка пересечения упомянутой выше прямой с прямой, ей перпендикулярной и проходящей через точку p.
        double x0 = (p.getI() + a * (p.getJ() - b)) / (a * a + 1);
        double y0 = (b + a * (p.getI() + a * p.getJ())) / (a * a + 1);

        return sqrt(pow(p.getI() - x0, 2) + pow(p.getJ() - y0, 2));
    }

    public static boolean pointInLine(Pixel p, Pixel p1, Pixel p2) {
        // Случай вертикальной прямой.
        if (p1.getI() == p2.getI())
            return p1.getI() == p.getI();

        // Случай невертикальной прямой.
        // y=a*x+b - уравнение прямой, проходящей через точки p1 и p2.
        double a = (p2.getJ() - p1.getJ()) / (p2.getI() - p1.getI() + 0.);
        double b = p1.getJ() - a * p1.getI();

        return a * p.getI() + b == p.getJ();
    }

    public static boolean pointInSegment(Pixel p, Pixel p1, Pixel p2, boolean... inclusive) {
        boolean include = inclusive.length > 0 && inclusive[0];

        int[] ii = AbstractPoint.findMinAndMax(new Pixel[]{p1, p2}, Pixel::getI);
        int[] jj = AbstractPoint.findMinAndMax(new Pixel[]{p1, p2}, Pixel::getJ);

        // Случай вертикальной прямой.
        if (p1.getI() == p2.getI())
            return p1.getI() == p.getI() && (include ?
                    jj[0] <= p.getJ() && p.getJ() <= jj[1] :
                    jj[0] < p.getJ() && p.getJ() < jj[1]);

        // Случай невертикальной прямой.
        // y=a*x+b - уравнение прямой, проходящей через точки p1 и p2.
        double a = (p2.getJ() - p1.getJ()) / (p2.getI() - p1.getI() + 0.);
        double b = p1.getJ() - a * p1.getI();

        return a * p.getI() + b == p.getJ() && (include ?
                ii[0] <= p.getI() && p.getI() <= ii[1] :
                ii[0] < p.getI() && p.getI() < ii[1]);
    }

    public static Pixel segmentsIntersect(Pixel a1, Pixel b1, Pixel a2, Pixel b2) {
        Pixel intersection = Pixel.findIntersection(a1, b1, a2, b2);
        if (intersection.getI() != Integer.MIN_VALUE) return intersection;

        if (pointInSegment(a1, a2, b2) && !pointInLine(b1, a2, b2)) return a1;

        if (pointInSegment(b1, a2, b2) && !pointInLine(a1, a2, b2)) return b1;

        if (pointInSegment(a2, a1, b1) && !pointInLine(b2, a1, b1)) return a2;

        if (pointInSegment(b2, a1, b1) && !pointInLine(a2, a1, b1)) return b2;

        return new Pixel(-1, -1);
    }

    public static Object[] whatToShorten(Polygon<Pixel> p1, Polygon<Pixel> p2, double pipeAngle1,
                                         int vertex1Index, int vertex2Index) {

        String[] sidesPer = sidesPerpendicular(pipeAngle1);
        String[] sidesPar = sidesParallel(pipeAngle1);

        List<Pixel> vertices1 = p1.getVertices();
        List<Pixel> vertices2 = p2.getVertices();

        int sideToShiftIndex = sidesPer[0].contains(vertex1Index + "") ? 0 :
                (sidesPer[1].contains(vertex1Index + "") ? 1 : sidesPer.length);
        int otherSideIndex = sidesPer.length - 1 - sideToShiftIndex;

        String sideToShift = sidesPer[sideToShiftIndex];
        String otherSide = sidesPer[otherSideIndex];
        var intersections = new ArrayList<Pixel>();

        for (int i = 0; i < vertices2.size(); i++) {
            Pixel a = vertices2.get(i);
            Pixel b = vertices2.get(i + 1 < vertices2.size() ? i + 1 : 0);

            if (segmentsIntersect(a, b,
                    vertices1.get(otherSide.charAt(0) - '0'),
                    vertices1.get(otherSide.charAt(1) - '0')).getI() != -1)
                return new Object[0];

            for (String side : sidesPar) {
                Pixel p = segmentsIntersect(a, b,
                        vertices1.get(side.charAt(0) - '0'),
                        vertices1.get(side.charAt(1) - '0'));
                if (p.getI() != -1)
                    intersections.add(p);
            }
        }

        Pixel end1OfSideToShift = vertices1.get(sideToShift.charAt(0) - '0');
        Pixel end2OfSideToShift = vertices1.get(sideToShift.charAt(1) - '0');

        double shift = intersections.size() > 0 ? max(distance(end1OfSideToShift, end2OfSideToShift, vertices2.get(vertex2Index)), intersections
                .stream()
                .mapToDouble(p -> distance(end1OfSideToShift, end2OfSideToShift, p))
                .max().orElseThrow(NoSuchElementException::new)) : 0;
        return new Object[]{shift, sideToShift};
    }

    public static boolean[] oneOne(Polygon<Pixel> p1, Polygon<Pixel> p2,
                                   double pipeAngle1, double pipeAngle2, int vertex1Index, int vertex2Index) {

        Object[] o1 = whatToShorten(p1, p2, pipeAngle1, vertex1Index, vertex2Index);
        Object[] o2 = whatToShorten(p2, p1, pipeAngle2, vertex2Index, vertex1Index);

        double shift1 = -1, shift2 = -1;
        String side1ToShift = "", side2ToShift = "";

        if (o1.length == 2) {
            shift1 = (double) o1[0];
            side1ToShift = (String) o1[1];
        }
        if (o2.length == 2) {
            shift2 = (double) o2[0];
            side2ToShift = (String) o2[1];
        }

        if (o1.length == 2 && o2.length == 0 && shift1 > 0) {
            shorten(p1, shift1, side1ToShift, pipeAngle1);
            return new boolean[]{true, false};
        }

        if (o1.length == 0 && o2.length == 2 && shift2 > 0) {
            shorten(p2, shift2, side2ToShift, pipeAngle2);
            return new boolean[]{false, true};
        }

        if (o1.length == 0 && o2.length == 0) {
            p2.getVertices().set(0, null);
            return new boolean[]{false, true};
        }

        if (shift1 < shift2) {
            if (shift1 > 0) {
                shorten(p1, shift1, side1ToShift, pipeAngle1);
                return new boolean[]{true, false};
            }
        } else {
            if (shift2 > 0) {
                shorten(p2, shift2, side2ToShift, pipeAngle2);
                return new boolean[]{false, true};
            }
        }
        return new boolean[]{false, false};
    }

    /**
     * Если один многоугольник принадлежит другому (т. е. все вершины принадлежат другому), то у него в качестве 0-й
     * вершины устанавливается значение {@code null}.
     * Редактируется не более одного многоугольника. Порядок просмотра - порядок в списке параметров.
     */
    public static boolean[] processInner(Polygon<Pixel> p1, Polygon<Pixel> p2) {
        if (p1.getVertices().get(0) == null || p2.getVertices().get(0) == null)
            return new boolean[]{false, false};

        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);
        List<Pixel> verticesFromP2 = p1.verticesFrom(p2, -1);

        // Все вершины p1 принадлежат p2.
        if (verticesFromP1.size() == p1.getVertices().size()) {
            p1.getVertices().set(0, null);
            return new boolean[]{true, false};
        }

        // Все вершины p2 принадлежат p1.
        if (verticesFromP2.size() == p2.getVertices().size()) {
            p2.getVertices().set(0, null);
            return new boolean[]{false, true};
        }

        return new boolean[]{false, false};
    }

    /**
     * Если один многоугольник имеет 2 противоположные или 3 вершины, которые принадлежат другому, то у него в качестве
     * 0-й вершины устанавливается значение {@code null}.
     */
    public static boolean[] processTwoOpposite(Polygon<Pixel> p1, Polygon<Pixel> p2) {
        List<Pixel> v1 = p1.getVertices();
        List<Pixel> v2 = p2.getVertices();

        if (v1.get(0) == null || v2.get(0) == null)
            return new boolean[]{false, false};

        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);
        List<Pixel> verticesFromP2 = p1.verticesFrom(p2, -1);

        if ((verticesFromP1.size() == 2 &&
                abs(v1.indexOf(verticesFromP1.get(0)) - v1.indexOf(verticesFromP1.get(1))) == 2 ||
                verticesFromP1.size() == 3)) {

            p1.getVertices().set(0, null);
            return new boolean[]{true, false};
        }

        if ((verticesFromP2.size() == 2 &&
                abs(v2.indexOf(verticesFromP2.get(0)) - v2.indexOf(verticesFromP2.get(1))) == 2 ||
                verticesFromP2.size() == 3)) {

            p2.getVertices().set(0, null);
            return new boolean[]{false, true};
        }

        return new boolean[]{false, false};
    }

    public static boolean[] processOneOne(Polygon<Pixel> p1, Polygon<Pixel> p2, double pipeAngle1, double pipeAngle2) {
        if (p1.getVertices().get(0) == null || p2.getVertices().get(0) == null)
            return new boolean[]{false, false};

        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);
        List<Pixel> verticesFromP2 = p1.verticesFrom(p2, -1);

        if (verticesFromP1.size() == 1 && verticesFromP2.size() == 1)
            return oneOne(p1, p2, pipeAngle1, pipeAngle2,
                    p1.getVertices().indexOf(verticesFromP1.get(0)),
                    p2.getVertices().indexOf(verticesFromP2.get(0)));
        return new boolean[]{false, false};
    }

    public static String[] sidesParallel(double pipeAngle) {
        return pipeAngle < 90 ? new String[]{"03", "12"} : new String[]{"01", "23"};
    }

    public static String[] sidesPerpendicular(double pipeAngle) {
        return pipeAngle < 90 ? new String[]{"01", "23"} : new String[]{"03", "12"};
    }

    public static boolean sideParallelToPipe(Pixel v1, Pixel v2, Polygon<Pixel> polygon, double pipeAngle) {
        int v1Index = polygon.getVertices().indexOf(v1);
        int v2Index = polygon.getVertices().indexOf(v2);
        return Arrays.asList(sidesParallel(pipeAngle)).contains("" + min(v1Index, v2Index) + max(v1Index, v2Index));
    }

    public static boolean sidePerpendicularToPipe(Pixel v1, Pixel v2, Polygon<Pixel> polygon, double pipeAngle) {
        int v1Index = polygon.getVertices().indexOf(v1);
        int v2Index = polygon.getVertices().indexOf(v2);
        return Arrays.asList(sidesPerpendicular(pipeAngle)).contains("" + min(v1Index, v2Index) + max(v1Index, v2Index));
    }

    /**
     *
     */
    public static boolean[] processTwoSequentialParallel(Polygon<Pixel> p1, Polygon<Pixel> p2,
                                                         double pipeAngle1, double pipeAngle2) {

        if (p1.getVertices().get(0) == null || p2.getVertices().get(0) == null)
            return new boolean[]{false, false};

        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);
        List<Pixel> verticesFromP2 = p1.verticesFrom(p2, -1);

        if (verticesFromP1.size() == 2 &&
                sideParallelToPipe(verticesFromP1.get(0), verticesFromP1.get(1), p1, pipeAngle1) &&
                checkInteriorIntersection(p1, p2, -1)) {

            p1.getVertices().set(0, null);
            return new boolean[]{true, false};
        }

        if (verticesFromP2.size() == 2 &&
                sideParallelToPipe(verticesFromP2.get(0), verticesFromP2.get(1), p2, pipeAngle2) &&
                checkInteriorIntersection(p1, p2, -1)) {

            p2.getVertices().set(0, null);
            return new boolean[]{false, true};
        }

        return new boolean[]{false, false};
    }

    public static boolean[] processTwoSequentialPerpendicular(Polygon<Pixel> p1, Polygon<Pixel> p2,
                                                              double pipeAngle1, double pipeAngle2) {

        if (p1.getVertices().get(0) == null || p2.getVertices().get(0) == null)
            return new boolean[]{false, false};

        List<Pixel> verticesFromP1 = p2.verticesFrom(p1, -1);
        List<Pixel> verticesFromP2 = p1.verticesFrom(p2, -1);

        if (verticesFromP1.size() == 2 &&
                sidePerpendicularToPipe(verticesFromP1.get(0), verticesFromP1.get(1), p1, pipeAngle1)) {

            Object[] o = twoSequentialPerpendicular(p1, p2, pipeAngle1,
                    verticesFromP1.get(0), verticesFromP1.get(1),
                    verticesFromP2.toArray(new Pixel[0]));
            if (o.length == 2 && (double) o[0] > 0) {
                shorten(p1, (double) o[0], (String) o[1], pipeAngle1);
                return new boolean[]{true, false};
            }
        }

        if (verticesFromP2.size() == 2 &&
                sidePerpendicularToPipe(verticesFromP2.get(0), verticesFromP2.get(1), p2, pipeAngle2)) {

            Object[] o = twoSequentialPerpendicular(p2, p1, pipeAngle2,
                    verticesFromP2.get(0), verticesFromP2.get(1),
                    verticesFromP1.toArray(new Pixel[0]));
            if (o.length == 2 && (double) o[0] > 0) {
                shorten(p2, (double) o[0], (String) o[1], pipeAngle2);
                return new boolean[]{false, true};
            }
        }

        return new boolean[]{false, false};
    }

    public static Object[] twoSequentialPerpendicular(Polygon<Pixel> p1, Polygon<Pixel> p2, double pipeAngle1,
                                                      Pixel vertex11, Pixel vertex12, Pixel... vertices2) {
        List<Pixel> v1 = p1.getVertices();
        List<Pixel> v2 = p2.getVertices();

        int vertex11Index = v1.indexOf(vertex11);
        int vertex12Index = v1.indexOf(vertex12);
        String sideToShift = "" + min(vertex11Index, vertex12Index) + max(vertex11Index, vertex12Index);

        String[] sidesPer = sidesPerpendicular(pipeAngle1);
        String otherSide = sidesPer[sidesPer.length - 1 - Arrays.asList(sidesPer).indexOf(sideToShift)];

        var intersections = new ArrayList<Pixel>();

        for (int i = 0; i < v2.size(); i++) {
            Pixel a = v2.get(i);
            Pixel b = v2.get(i + 1 < v2.size() ? i + 1 : 0);

            if (segmentsIntersect(a, b,
                    v1.get(otherSide.charAt(0) - '0'),
                    v1.get(otherSide.charAt(1) - '0')).getI() != -1)
                return new Object[0];

            for (String side : sidesParallel(pipeAngle1)) {
                Pixel p = segmentsIntersect(a, b,
                        v1.get(side.charAt(0) - '0'),
                        v1.get(side.charAt(1) - '0'));
                if (p.getI() != -1)
                    intersections.add(p);
            }
        }

        Pixel sideToShiftEnd1 = v1.get(sideToShift.charAt(0) - '0');
        Pixel sideToShiftEnd2 = v1.get(sideToShift.charAt(1) - '0');

        double shift = max(
                vertices2.length > 0 ? Arrays.stream(vertices2)
                        .mapToDouble(p -> distance(sideToShiftEnd1, sideToShiftEnd2, p))
                        .max().orElseThrow(NoSuchElementException::new) : 0,
                intersections.size() > 0 ? intersections
                        .stream()
                        .mapToDouble(p -> distance(sideToShiftEnd1, sideToShiftEnd2, p))
                        .max().orElseThrow(NoSuchElementException::new) : 0);
        return new Object[]{shift, sideToShift};
    }

    static class Class {
        int i;

        Class(int i) {
            this.i = i;
        }

        @Override
        public String toString() {
            return i + "";
        }
    }

    public static List<Class> vv(List<Class> l) {
        var res = new ArrayList<Class>();
        if (l.get(0).i == 1) res.add(l.get(0));
        return res;
    }

    public static boolean checkInteriorIntersection(Polygon<Pixel> polygon1, Polygon<Pixel> polygon2, double focalLength) {
        return Rectangle.getIntersection(polygon1, polygon2, focalLength).square(focalLength) > 0;
    }

    public static double squarePolygon(Polygon<Pixel> polygon, Polygon<Pixel> overlap, Polygon<Pixel> bigPolygon,
                                       double thermogramHeight, double pixelSize, double focalLength) {

        double s1 = Rectangle.squarePolygonWithoutOverlap(polygon, overlap, focalLength);
        double s2 = Rectangle.squarePolygonWithoutOverlap(polygon, bigPolygon, focalLength);
        return Thermogram.toEarthSquare(s1 - s2, thermogramHeight, focalLength, pixelSize);
    }

    public static void main(String[] args) {

        Polygon<Pixel> p1 = new Polygon<>(Arrays.asList(new Pixel(10, 10), new Pixel(100, 10),
                new Pixel(100, 20), new Pixel(10, 20)), -1);
        Polygon<Pixel> p2 = (new Rectangle<>(new Pixel(0, 0), new Pixel(200, 15))).toPolygon();

        List<Pixel> l = p2.verticesFrom(p1, -1);
        System.out.println(l);
        p1.getVertices().set(0, null);
        System.out.println(l);

        /*double a = 2.00000000001;
        double b = 17.00050000404;
        var p = new Polygon<>(Arrays.asList(
                new Pixel(1, a * 1 + b), new Pixel(10, a * 10 + b),
                new Pixel(100, a * 100 + b),
                new Pixel(10000, a * 10000 + b + 1)),
                -1);
        System.out.println(p.square(-1));*/

        /*Object[] o = new Object[]{12.4};
        System.out.println(((double) o[0]) + "  " + ((Double) o[0]) + "  " + ((double) o[0] > 0));*/

        /*List<Class> list = Arrays.asList(new Class(1), new Class(2));
        //Object[] v = new ArrayList<>(list).toArray();
        Class[] v = new ArrayList<>(list).toArray(new Class[0]);
        ((Class)v[1]).i = -1;
        //list.get(1).i=10;
        v[0] = new Class(1000);
        System.out.println(Arrays.toString(v) + "  " + list);*/

        /*Polygon<Pixel> p = new Polygon<>(Arrays.asList(
                new Pixel(0, 0), new Pixel(1, 0), new Pixel(1, 1), new Pixel(0, 1)), 0);
        System.out.println(p);
        p.getVertices().set(0, new Pixel(-1,-1));
        System.out.println(p);*/

        /*List<Integer> list = Arrays.asList(1, 2, 3);
        List<String> out = list.stream()
                .flatMap(i -> list.stream()
                        .filter(j -> !i.equals(j)).map(j -> "" + i + j))
                .collect(Collectors.toList());
        out.forEach(System.out::println);*/

        /*var l = new ArrayList<Class>();
        var o = new Class(1);
        l.add(o);
        List<Class> out = vv(l);
        out.get(0).i = 2;
        System.out.println((o==l.get(0)) + "  " + (out.get(0)==o) + "   " + out.get(0).i + "   " + o.i + "   " + l.get(0).i);*/

        //Helper.write("/home/ruslan/geo/a_test/rest/a.txt", "");

        //System.out.println(Helper.filename("a", "b", null, "d"));

        /*String a = "-";
        String b = "-";

        String[] sss= new String[]{a, b};
        Arrays.stream(sss).sequential().forEach(s -> s+="+++");
        System.out.println(a + "   " + b);

        StringBuilder a2 = new StringBuilder("-");
        StringBuilder b2 = new StringBuilder("-");

        StringBuilder[] sss2= new StringBuilder[]{a2, b2};
        Arrays.stream(sss2).sequential().forEach(s -> s.append("+++"));
        System.out.println(a2 + "   " + b2);*/

        /*for(String s : new String[]{a, b})
            s = "0000" + "";*/

        /*double num = 1.10100042;
        int k = 6;
        String s = round(num * pow(10, k)) / pow(10, k) + "";
        System.out.println(s);*/
        //Helper.clear("/home/ruslan/geo/a_test/output files/pipe_squares.txt");
        /*Helper.addFile("/home/ruslan/geo/a_test/output files/pipe_squares.txt",
                "/home/ruslan/geo/a_test/output files/tmp/pipe_squares___DJI_0299_R.txt", true);*/

        //List<Integer> list = new ArrayList<>();

        //String plants = broken("seed"); // ClassCastException

        /*int n = 48;
        double coef = 4;
        double diameter = 0.7;
        double height = 152;
        double focalLength = 25. / 1000;
        int resY = 512;
        int resX = 640;
        String filename;
        Pixel pixel1, pixel2, pixel3, pixel4;
        //double diameterPixel = realToMatrix(diameter, height, Main.PIXEL_SIZE, focalLength);
        double diameterPixel = 7;

        // 829 thermogram
        filename = Main.DIR_CURRENT + "/" + Property.SUBDIR_REAL_TEMPS.value() +
                "/" + "DJI_0829_R" + Property.POSTFIX_REAL_TEMPS.value() + Main.EXTENSION_REAL;
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
        filename = Main.DIR_CURRENT + "/" + Property.SUBDIR_REAL_TEMPS.value() +
                "/" + "DJI_0817_R" + Property.POSTFIX_REAL_TEMPS.value() + Main.EXTENSION_REAL;
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
                pixel1, pixel2, pixel3, pixel4));*/
    }
}
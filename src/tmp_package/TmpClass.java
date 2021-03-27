package tmp_package;

import figures.*;
import main.*;
import main.Thermogram;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.*;


class TmpClass {
    static Segment getSegment(Pixel centre, double angle, double length, int resY) {
        Pixel a = new Pixel(centre.getI() + 0.5 * length * sin(angle),
                centre.getJ() + 0.5 * length * cos(angle));
        Pixel b = new Pixel(centre.getI() - 0.5 * length * sin(angle),
                centre.getJ() - 0.5 * length * cos(angle));
        return new Segment(a.toPoint(resY), b.toPoint(resY));
    }

    static double av(Segment s, double[][] table, int resX) {
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

    static double f(double diameter, Pixel pixel, int n, double coef, char separator, String filename, int resX, int resY) {
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

    static double[] angles(Pixel v1, Pixel v2, double diameter, int n, double coef, char separator, String filename, int resX, int resY) {
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

    static double bisector(double a1, double a2, boolean isHorisontal) {
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

    static double vert(Rectangle<Pixel> rectangle,
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

    static double[] toMinus(double... vals) {
        double[] newVals = new double[vals.length];
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] < 90)
                newVals[i] = vals[i];
            if (vals[i] > 90)
                newVals[i] = vals[i] - 180;
        }
        return newVals;
    }

    static double sss(double diameter, int n, double coef, char separator, String filename, int resX, int resY, Pixel... pixels) {
        double sum = 0;
        for (Pixel pixel : pixels)
            sum += f(diameter, pixel, n, coef, separator, filename, resX, resY);
        return sum / pixels.length;
    }

    static double realToMatrix(double distance, double height, double pixelSize, double focalLength) {
        return distance / Thermogram.reverseScale(height, focalLength) / pixelSize;
    }

    static double sq(Polygon<Pixel> polygon, double height, double focalLength, double pixelSize) {
        List<Pixel> v = polygon.getVertices();
        return Thermogram.earthDistance(v.get(0), v.get(1), height, focalLength, pixelSize) *
                Thermogram.earthDistance(v.get(1), v.get(2), height, focalLength, pixelSize);
    }

    /*static boolean pointInLine(Pixel p, Pixel p1, Pixel p2) {
        // Случай вертикальной прямой.
        if (p1.getI() == p2.getI())
            return p1.getI() == p.getI();

        // Случай невертикальной прямой.
        // y=a*x+b - уравнение прямой, проходящей через точки p1 и p2.
        double a = (p2.getJ() - p1.getJ()) / (p2.getI() - p1.getI() + 0.);
        double b = p1.getJ() - a * p1.getI();

        return a * p.getI() + b == p.getJ();
    }*/

    /*static Pixel segmentsIntersect(Pixel a1, Pixel b1, Pixel a2, Pixel b2) {
        Pixel intersection = Pixel.findIntersection(a1, b1, a2, b2);
        if (intersection.getI() != Integer.MIN_VALUE) return intersection;

        if (pointInSegment(a1, a2, b2) && !pointInLine(b1, a2, b2)) return a1;

        if (pointInSegment(b1, a2, b2) && !pointInLine(a1, a2, b2)) return b1;

        if (pointInSegment(a2, a1, b1) && !pointInLine(b2, a1, b1)) return a2;

        if (pointInSegment(b2, a1, b1) && !pointInLine(a2, a1, b1)) return b2;

        return new Pixel(-1, -1);
    }*/

    /*static Map<String, List<Rectangle<Pixel>>> readForbiddenZones(String filename) {
        return Helper.mapFromFileWithJsonArray(filename, o -> {
            JsonObject jRectangle = (JsonObject) o;
            JsonObject jLeft = (JsonObject) jRectangle.get("Left");
            JsonObject jRight = (JsonObject) jRectangle.get("Right");
            return new Rectangle<>(
                    new Pixel(jLeft.get("I").getAsInt(), jLeft.get("J").getAsInt()),
                    new Pixel(jRight.get("I").getAsInt(), jRight.get("J").getAsInt()));
        }, "Name", "ForbiddenZones");
    }*/

    /*static Map<String, List<Double>> readStandardPipeAngles(String filename) {
        return Helper.mapFromFileWithJsonArray(filename, JsonElement::getAsDouble, "Name", "CustomPipeAngles");
    }*/

    /*static Map<String, List<Double>> readStandardPipeAngles(String filename) {
        JsonArray arrEntries = null, arrAngles;
        JsonObject jEntry;

        var names = new ArrayList<String>();
        var anglesLists = new ArrayList<List<Double>>();
        var angles = new ArrayList<Double>();

        try {
            arrEntries = (JsonArray) new JsonParser().parse(new FileReader(filename));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for (Object objEntry : arrEntries) {
            jEntry = (JsonObject) objEntry;
            names.add(jEntry.get("Name").getAsString());
            arrAngles = (JsonArray) jEntry.get("StandardPipeAngles");
            for (Object objAngle : arrAngles)
                angles.add(((JsonPrimitive) objAngle).getAsDouble());
            anglesLists.add(new ArrayList<>(angles));
            angles.clear();
        }
        Map<String, List<Double>> map = new HashMap<>();
        for (int i = 0; i < names.size(); i++)
            map.put(names.get(i), anglesLists.get(i));
        return map;
    }*/

    static boolean checkInteriorIntersection(Polygon<Pixel> polygon1, Polygon<Pixel> polygon2, double focalLength) {
        return Polygon.getIntersection(polygon1, polygon2, focalLength).square(focalLength) > 0;
    }

    static Pixel middle(Rectangle<Pixel> rectangle) {
        return new Pixel((rectangle.getLeft().getI() + rectangle.getRight().getI()) / 2,
                (rectangle.getLeft().getJ() + rectangle.getRight().getJ()) / 2);
    }

    synchronized static double check(Polygon<Pixel> defect, double minLengthwiseDistance, double maxTransverseDistance) {
        var distances = new ArrayList<Double>();
        double no = -1;
        List<Pixel> vertices = defect.getVertices();
        var inclinations = new ArrayList<Double>();

        for (int i = 0; i < vertices.size(); i++)
            for (int j = i + 1; j < vertices.size(); j++) {
                distances.add(AbstractPoint.distance(vertices.get(i), vertices.get(j)));
                if (vertices.get(i).getI() == vertices.get(j).getI())
                    inclinations.add(90.);
                else {
                    double m = atan(Segment.coefs(vertices.get(i), vertices.get(j))[0]) * 180 / PI;
                    inclinations.add(m < 0 ? m + 180 : m);
                }
            }

        if (distances.stream().max(Double::compareTo).orElse(no) < minLengthwiseDistance)
            return no;

        if (inclinations.contains(100.61965527615513)) {
            System.out.println("= =========================   FOUND\n");
            System.out.println(Thread.currentThread().getName());
            System.out.println(distances.size() + "   " + inclinations.size());
            System.out.println(distances + "\n");
            System.out.println(inclinations + "\n");
        }

        try {
            inclinations.sort(Comparator.comparingDouble(o -> {
                if (o.equals(100.61965527615513) && inclinations.indexOf(o) == -1) {
                    System.out.println("-------  " + o);
                    System.out.println(Thread.currentThread().getName());
                    System.out.println(distances.size() + "   " + inclinations.size());
                    System.out.println(distances + "\n");
                    System.out.println(inclinations + "\n");
                    //throw new IllegalArgumentException();
                }
                return distances.get(inclinations.indexOf(o));
            }));
        } catch (Exception e) {
            //throw new Error();
        }

        int n = 3;
        double[] a = new double[n];
        for (int k = 0; k < n; k++)
            a[k] = inclinations.get(inclinations.size() - k - 1);
        double sinSum = 0;
        double cosSum = 0;
        for (int i = 0; i < n; i++) {
            sinSum += sin((n - i) * a[i] * PI / 180);
            cosSum += cos((n - i) * a[i] * PI / 180);
        }
        double average = atan2(sinSum, cosSum) * 180 / PI;
        average = average < 0 ? average + 180 : average;
        average = average == 180 ? 0 : average;

        var distances2 = new ArrayList<Double>();
        for (int i = 0; i < vertices.size(); i++)
            for (int j = i + 1; j < vertices.size(); j++) {
                double aa;
                Pixel p1 = vertices.get(j), p2;
                if (average == 90) {
                    p2 = new Pixel(p1.getI(), -1);
                } else {
                    aa = 90 < average && average < 180 ? average - 180 : average;
                    p2 = new Pixel(-1, tan(aa * PI / 180) * (-1) + (p1.getJ() - tan(aa * PI / 180) * p1.getI()));
                }
                distances2.add(vertices.get(i).distanceToLine(p1, p2));
            }

        if (distances2.stream().max(Double::compareTo).orElse(no) > maxTransverseDistance)
            return no;

        return average;
    }


    static void main(String[] args) throws IOException {

        /*Pixel centre2 = new Pixel(0, 1);
        Pixel centre1 = new Pixel(0, 0);
        double angle = atan2(centre1.getJ() - centre2.getJ(), centre1.getI() - centre2.getI());
        System.out.println(angle * 180 / PI);*/

        /*Segment[] segments = new Segment[]{
                new Segment(new Point(304, 203), new Point(237, 316)),
                new Segment(new Point(309, 251), new Point(304, 261))};

        BufferedImage image = ImageIO.read(new File("/home/ruslan/geo/The rmograms/DJI_0343_R.JPG"));
        double[][] realTable = Helper.extractTable("/home/ruslan/geo/a_test/real temps/DJI_0343_R_real_temps.csv",
                ';');

        for (Segment segment : segments) {
            segment.draw(image, Color.BLACK);
            ImageIO.write(image, "jpg", new File("/home/ruslan/geo/a_test/rest/folder/aa.jpg"));
            System.out.println(detect(segment, realTable));
        }*/

        //System.out.println(Runtime.getRuntime().availableProcessors());

        /*ExecutorService executor = Executors.newFixedThreadPool(3);
        ExecutorService executor2 = Executors.newFixedThreadPool(3);

        Runnable[] childTasks = new Runnable[25];
        for (int i = 0; i < childTasks.length; i++) {
            int ii = i;
            childTasks[i] = () -> {
                try {
                    Thread.sleep(100);
                    System.out.println("childTask " + ii + " finished");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
        }

        Runnable[] tasks = new Runnable[50];
        for (int i = 0; i < tasks.length; i++) {
            int ii = i;
            tasks[i] = () -> {
                try {
                    Thread.sleep(500);
                    Future[] futures = new Future[childTasks.length];
                    System.out.println("task " + ii + " started");
                    for (int k = 0; k < 25; k++) {
                        int kk = k;
                        futures[k] = executor2.submit(() -> {
                            try {
                                Thread.sleep(100);
                                System.out.println("childTask " + kk + " finished");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                    try {
                        for (int k = 0; k < 25; k++) {
                            futures[k].get();
                        }
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                    //for(Runnable t : childTasks)
                    //executor.shutdown();
                    //executor.awaitTermination(1, TimeUnit.DAYS);
                    System.out.println("task " + ii + " finished");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
        }

        Future[] futures = new Future[tasks.length];
        for (int i = 0; i < tasks.length; i++)
            futures[i] = executor.submit(tasks[i]);

        try {
            for (int k = 0; k < tasks.length; k++) {
                futures[k].get();
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        executor2.shutdown();
        try {
            executor2.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }*/

        /*Integer[] arr = new Integer[]{4,1,0,7,2};
        arr = Arrays.stream(arr).sorted(Comparator.comparingInt(o -> o)).toArray(Integer[]::new);
        System.out.println(Arrays.toString(arr));*/

        /*Function<? super B, Integer> f1 = o -> o.f();
        Function<? super B, Integer> f2 = o -> ((C) o).f();
        Function<? super B, Integer> f3 = o -> ((C) o).g();
        f2.apply(new B());
        f1.apply(new B());*/
        /*AbstractPoint p = new Point(0,0);
        Point p1 = new Point(5,6);
        Point p2 = new Point(5,6);
        try {
            System.out.println(p.distanceToLine(p1, p2));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }*/

        /*Polygon<Pixel> p1 = new Polygon<>(Arrays.asList(new Pixel(10, 10), new Pixel(100, 10),
                new Pixel(100, 20), new Pixel(10, 20)), -1);
        Polygon<Pixel> p2 = (new Rectangle<>(new Pixel(0, 0), new Pixel(200, 15))).toPolygon();

        List<Pixel> l = p2.verticesFrom(p1, -1);
        System.out.println(l);
        p1.getVertices().set(0, null);
        System.out.println(l);*/

        /*double a = 2.00000000001;
        double b = 17.00050000404;
        var p = new Polygon<>(Arrays.asList(
                new Pixel(1, a * 1 + b), new Pixel(10, a * 10 + b),
                new Pixel(100, a * 100 + b),
                new Pixel(10000, a * 10000 + b)),
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
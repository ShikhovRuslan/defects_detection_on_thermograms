package main;

import javenue.csv.Csv;
import polygons.Point;
import polygons.Polygon;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
    private static final String NEW_PICTURENAME = DIR + "/picture2.jpg";
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

    private static int amountOfOnes(List<List<Integer>> table, int i1, int j1, int i2, int j2) {
        int count = 0;
        for (int i = i1; i <= Math.min(i2, table.size() - 1); i++)
            for (int j = j1; j <= Math.min(j2, table.get(0).size() - 1); j++)
                if (table.get(i).get(j) == 1) count++;
        return count;
    }

    private static List<Integer> squarePixels(List<Integer[]> ranges) {
        List<Integer> result = new ArrayList<>();
        for (Integer[] range : ranges)
            result.add(squarePixels(range));
        return result;
    }

    private static int[] makeRange(List<List<Integer>> table, int i, int j) {
        int x = i, y = j;
        boolean incrementX, incrementY;
        do {
            incrementX = false;
            incrementY = false;
            if (amountOfOnes(table, i, j, x + 1, y) - amountOfOnes(table, i, j, x, y) > (y - j) / 2) {
                x++;
                incrementX = true;
            }
            if (amountOfOnes(table, i, j, x, y + 1) - amountOfOnes(table, i, j, x, y) > (x - i) / 2) {
                y++;
                incrementY = true;
            }
        } while (incrementX || incrementY);
        return new int[]{x, y};
    }

    private static boolean isIn(int i0, int j0, List<Integer[]> ranges) {
        for (Integer[] range : ranges)
            if (i0 >= range[0] && i0 <= range[2] && j0 >= range[1] && j0 <= range[3]) return true;
        return false;
    }

    private static List<Integer[]> findRanges(List<List<Integer>> table) {
        int[] point;
        List<Integer[]> ranges = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            for (int j = 0; j < table.get(0).size(); j++)
                if (table.get(i).get(j) == 1 & !(isIn(i, j, ranges))) {
                    point = makeRange(table, i, j);
                    ranges.add(new Integer[]{i, j, point[0], point[1]});
                }
        }
        return ranges;
    }

    private static Integer[] uniteRanges(Integer[] r1, Integer[] r2) {
        Integer[] result = new Integer[r1.length + r2.length];
        System.arraycopy(r1, 0, result, 0, r1.length);
        System.arraycopy(r2, 0, result, r1.length, r2.length);
        return result;
    }

    private static Integer[] areRangesNear(Integer[] r1, Integer[] r2) {
        int x1, x2, y1, y2;
        Integer[] coords = new Integer[r1.length + r2.length + 8];
        f:
        if (r1[0] != null && r2[0] != null) {
            if (Math.abs(r1[1] - r2[3]) < 3) {
                x1 = Math.max(r1[0], r2[0]);
                x2 = Math.min(r1[2], r2[2]);
                coords = uniteRanges(new Integer[]{x1, r1[1], x1, r2[3], x2, r1[1], x2, r2[3]}, uniteRanges(r1, r2));
                break f;
            }
            if (Math.abs(r1[3] - r2[1]) < 3) {
                x1 = Math.max(r1[0], r2[0]);
                x2 = Math.min(r1[2], r2[2]);
                Integer[] arr1 = {x1, r1[3], x1, r2[1], x2, r1[3], x2, r2[1]};
                Integer[] arr2 = uniteRanges(r1, r2);
                coords = uniteRanges(arr1, arr2);
                break f;
            }
            if (Math.abs(r1[2] - r2[0]) < 3) {
                y1 = Math.max(r1[1], r2[1]);
                y2 = Math.min(r1[3], r2[3]);
                Integer[] arr1 = {y1, r1[2], y1, r2[0], y2, r1[2], y2, r2[0]};
                Integer[] arr2 = uniteRanges(r1, r2);
                coords = uniteRanges(arr1, arr2);
                break f;
            }
            if (Math.abs(r1[0] - r2[2]) < 3) {
                y1 = Math.max(r1[1], r2[1]);
                y2 = Math.min(r1[3], r2[3]);
                coords = uniteRanges(new Integer[]{y1, r1[0], y1, r2[2], y2, r1[0], y2, r2[2]}, uniteRanges(r1, r2));
                break f;
            }
        }
        return coords;
    }

    private static List<Integer[]> toPoligons(List<Integer[]> ranges) {
        for (int k = 0; k < ranges.size(); k++)
            for (int s = k + 1; s < ranges.size(); s++)
                if (!Arrays.equals(areRangesNear(ranges.get(k), ranges.get(s)), new Integer[ranges.get(k).length + ranges.get(s).length + 8])) {
                    ranges.set(k, areRangesNear(ranges.get(k), ranges.get(s)));
                    ranges.set(s, new Integer[4]);
                    break;
                }
        return ranges;
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

    public static void drawLine(BufferedImage image, Color color, int i1, int j1, int i2, int j2) {
        int tmpI = i1, tmpJ = j1;
        i1 = Math.min(i1, i2);
        i2 = Math.max(tmpI, i2);
        j1 = Math.min(j1, j2);
        j2 = Math.max(tmpJ, j2);
        int[] w = i1 == i2 ? new int[]{0, 1} : new int[]{1, 0};
        for (int k = i1 * w[0] + j1 * w[1]; k <= i2 * w[0] + j2 * w[1]; k++)
            image.setRGB(k * w[1] + j1 * w[0], k * w[0] + i1 * w[1], color.getRGB());
    }

    private static void drawRectangle(BufferedImage image, Color color, int i1, int j1, int i2, int j2) {
        drawLine(image, color, i1, j1, i2, j1);
        drawLine(image, color, i2, j1, i2, j2);
        drawLine(image, color, i2, j2, i1, j2);
        drawLine(image, color, i1, j2, i1, j1);
    }

    private static void drawRanges(List<Integer[]> ranges, String pictureName, String newPictureName) {
        try {
            BufferedImage image = ImageIO.read(new File(pictureName));
            for (Integer[] range : ranges)
                drawRectangle(image, BLACK, range[0], range[1], range[2], range[3]);
            ImageIO.write(image, "jpg", new File(newPictureName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void drawRangesSoph(List<Integer[]> ranges, String pictureName, String newPictureName) {
        try {
            BufferedImage image = ImageIO.read(new File(pictureName));
            for (Integer[] range : ranges) {
                if (range.length == 4)
                    drawRectangle(image, BLACK, range[0], range[1], range[2], range[3]);
                else {
                    drawRectangle(image, BLACK, range[8], range[9], range[10], range[11]);
                    drawRectangle(image, BLACK, range[12], range[13], range[14], range[15]);
                    drawLine(image, BLACK, range[0], range[1], range[2], range[3]);
                    drawLine(image, BLACK, range[4], range[5], range[6], range[7]);
                }
            }
            ImageIO.write(image, "jpg", new File(newPictureName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int squarePixels(Integer[] range) {
        return (range[2] - range[0] + 1) * (range[3] - range[1] + 1);
    }

    private static List<Integer[]> selectRanges(List<Integer[]> rawRanges, Predicate<Integer[]> predicate) {
        List<Integer[]> ranges = new ArrayList<>();
        for (Integer[] range : rawRanges)
            if (predicate.test(range)) ranges.add(range);
        return ranges;
    }

    private static List<Integer[]> deleteNulls(List<Integer[]> ranges) {
        Integer[] range;
        for (Iterator<Integer[]> iter = ranges.iterator(); iter.hasNext(); ) {
            range = iter.next();
            if (range[0] == null)
                iter.remove();
        }
        return ranges;
    }

    private static Polygon convertRange(Integer[] range) {
        List<Point> vertices = new ArrayList<>();
        vertices.add(new Point(range[0], range[1]));
        vertices.add(new Point(range[0], range[3]));
        vertices.add(new Point(range[2], range[3]));
        vertices.add(new Point(range[2], range[1]));
        return new Polygon(vertices);
    }

    private static List<Polygon> convertRanges(List<Integer[]> ranges) {
        List<Polygon> polygons = new ArrayList<>();
        for (Integer[] range : ranges)
            polygons.add(convertRange(range));
        return polygons;
    }

    private static void f() throws IOException {
        List<List<String>> rawTable = extractRawTable(FILENAME);
        List<List<String>> table = extractTable(rawTable);
        //printTable(table);
        int[][] arr = findIf(table, num -> num > T_MIN);
        //printTable(arr);
        List<Integer[]> rawRanges = findRanges(arrayToList(arr));
        //System.out.println(Arrays.deepToString(rawRanges.toArray()) + "\n" + rawRanges.size());
        //System.out.println(Arrays.toString(squarePixels(rawRanges).toArray()));
        drawRanges(rawRanges, PICTURENAME, NEW_PICTURENAME);
        List<Integer[]> ranges = selectRanges(rawRanges, range -> squarePixels(range) >= MIN_SQUARE_PIXELS);
        System.out.println(Arrays.deepToString(ranges.toArray()) + "\n" + ranges.size());

        drawRanges(ranges, PICTURENAME, NEW_PICTURENAME);

//        List<Integer[]> newRangers = toPoligons(ranges);
//        List<Integer[]> newRangers2 = deleteNulls(newRangers);
//        System.out.println(Arrays.deepToString(newRangers2.toArray()) + "\n" + newRangers2.size());
//        drawRangesSoph(newRangers2, PICTURENAME, NEW_PICTURENAME);

        List<Polygon> polygons = convertRanges(ranges);
        drawPolygons(polygons, BLACK, PICTURENAME, NEW_PICTURENAME);
        drawPolygons(Polygon.toPoligons(polygons), BLACK, PICTURENAME, NEW_PICTURENAME);

//        BufferedImage image = ImageIO.read(new File(PICTURENAME));
//        List<Point> points = new ArrayList<>();
//        points.add(new Point(100,100));
//        points.add(new Point(100,500));
//        points.add(new Point(200,500));
//        points.add(new Point(200,100));
//        new Polygon(points).draw(image, BLACK);
//        //new Line(new Point(100, 100), new Point(400, 100)).draw(image, BLACK);
//        ImageIO.write(image, "jpg", new File(NEW_PICTURENAME));



        int u = 0;
    }

    public static void main(String[] args) {
//        Csv.Writer writer = new Csv.Writer(FILE).delimiter(',');
//        writer.comment("example of csv")
//                .value("a").value("b").newLine()
//                .value("c").close();
//        try {
//            Csv.Reader reader = new Csv.Reader(new FileReader(FILE))
//                    .delimiter(',').ignoreComments(true);
//            System.out.println(reader.readLine());
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        try {
            f();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        List<List<Integer>> table = new ArrayList<>();
//        int i = 0;
//        try {
//            File file = new File("/home/ruslan/geo" + "/file2.txt");
//            BufferedReader reader = new BufferedReader(new FileReader(file));
//            String line;
//            table = new ArrayList<>();
//            i = 0;
//            do {
//                line = reader.readLine();
//                table.add(new ArrayList<>());
//                for (int j = 0; j < line.length(); j++) {
//                    table.get(i).add(line.toCharArray()[j] - '0');
//                }
//                i++;
//            } while (!line.equals(""));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        int y = 0;
//        table.remove(i-1);
//        List<List<Integer>> l = table;
//        System.out.println(Arrays.deepToString(find(table).toArray()));
    }
}
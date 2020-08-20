package main;

import javenue.csv.Csv;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * На снимке выбрана система координат Oij. Центром O является верхняя левая точка. Ось Oi направлена вдоль левой
 * стороны термограммы, а ось Oj - вдоль верхней.
 * <p>
 * Прямоугольник задаётся в виде массива {@code {r0, r1, r2, r3}}, где ({@code r0}, {@code r1}) - верхняя левая вершина,
 * а ({@code r2}, {@code r3}) - нижняя правая.
 */
public class Range {
    //static final String DIR = "/home/ruslan/geo";
    //static final String FILENAME = DIR + "/file.txt";
    //static final String PICTURENAME = DIR + "/picture.jpg";
    //static final String NEW_PICTURENAME_1 = DIR + "/picture2_1.jpg";
    //static final String NEW_PICTURENAME_2 = DIR + "/picture2_2.jpg";
    static final double T_MIN = 30;
    static final double T_MAX = 100;
    static final int HEIGHT = 250;
    public static final int RES_I = 512;
    public static final int RES_J = 640;
    static final int MIN_SQUARE_PIXELS = 25;

    /**
     * Возвращает представление файла с названием {@code filename} в виде таблицы.
     */
    static List<List<String>> extractRawTable(String filename) throws FileNotFoundException {
        Csv.Reader reader = new Csv.Reader(new FileReader(filename)).delimiter(';').ignoreComments(true);
        List<List<String>> rawTable = new ArrayList<>();
        List<String> line;
        do {
            line = reader.readLine();
            rawTable.add(line);
        } while (line != null);
        return rawTable;
    }

    /**
     * Возвращает подтаблицу таблицы {@code rawTable}, содержащую числа.
     */
    static List<List<String>> extractTable(List<List<String>> rawTable) {
        Pattern pattern = Pattern.compile("-?\\d{1,2},\\d{1,3}");
        Matcher matcher;
        List<List<String>> table = new ArrayList<>();
        int fromIndex = 0;
        int count = 0;
        boolean rightLine = false;
        boolean found;
        for (List<String> line : rawTable)
            if (line != null) {
                for (int i = 0; i < line.size(); i++) {
                    matcher = pattern.matcher(line.get(i));
                    found = matcher.find();
                    if (found)
                        count++;
                    if (count == 1)
                        fromIndex = i;
                    if (!found && (i == fromIndex + 1 || i == fromIndex + 2))
                        break;
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
                if (rightLine)
                    table.add(line.subList(fromIndex, line.size()));
                count = 0;
                rightLine = false;
            }
        return table;
    }

    /**
     * Печатает таблицу {@code table}.
     */
    static void printTable(List<List<String>> table) {
        for (List<String> line : table)
            System.out.println(line);
    }

    /**
     * Печатает таблицу {@code table}.
     */
    static void printTable(int[][] table) {
        for (int[] line : table) {
            for (int num : line)
                System.out.print(num);
            System.out.println();
        }
    }

    /**
     * Определяет, является ли указанный прямоугольник {@code range} горизонтальной или вертикальной линией.
     */
    private static boolean isLine(Integer[] range) {
        return range[0].equals(range[2]) || range[1].equals(range[3]);
    }

    /**
     * Определяет принадлежность точки ({@code i0}, {@code j0}) прямоугольнику {@code range}.
     */
    private static boolean pointIsInRange(int i0, int j0, Integer[] range) {
        return (range[0] <= i0 && i0 <= range[2]) && (range[1] <= j0 && j0 <= range[3]);
    }

    /**
     * Определяет, принадлежит ли точка ({@code i0}, {@code j0}) какому-нибудь прямоугольнику из списка
     * {@code ranges}.
     */
    private static boolean pointIsInRanges(int i0, int j0, List<Integer[]> ranges) {
        for (Integer[] range : ranges)
            if (pointIsInRange(i0, j0, range))
                return true;
        return false;
    }

    /**
     * Возвращает таблицу целых чисел, построенную на основании таблицы {@code table} (которая содержит строковые
     * представления чисел) и предиката {@code predicate}.
     * Если элемент таблицы {@code table} удовлетворяет критерию, задаваемому предикатом {@code predicate}, то на этом
     * же месте в таблице целых чисел пишется значение {@code 1}, иначе остаётся значение по умолчанию {@code 0}.
     */
    static int[][] findIf(List<List<String>> table, Predicate<Double> predicate) {
        int[][] newTable = new int[table.size()][table.get(0).size()];
        for (int i = 0; i < table.size(); i++)
            for (int j = 0; j < table.get(0).size(); j++)
                if (predicate.test(new Double(table.get(i).get(j).replace(',', '.'))))
                    newTable[i][j] = 1;
        return newTable;
    }

    /**
     * Возвращает число точек в прямоугольнике {@code range}.
     */
    public static int squarePixels(Integer[] range) {
        return (range[2] - range[0] + 1) * (range[3] - range[1] + 1);
    }

    /**
     * Конвертирует прямоугольник {@code range} из системы координат Oij в систему координат c'x'y'.
     */
    public static Pixel[] toRectangle(Integer[] range) {
        return new Pixel[]{new Pixel(range[1], NewClass.RES_Y - range[2]), new Pixel(range[3], NewClass.RES_Y - range[0])};
    }

    /**
     * Определяет, пересекает ли вертикальная линия с концами ({@code i0}, {@code j}) и ({@code i1}, {@code j})
     * какой-нибудь прямоугольник из списка {@code ranges}.
     */
    private static boolean verticalLineIntersectsRanges(int i0, int i1, int j, List<Integer[]> ranges) {
        for (int k = Math.min(i0, i1); k <= Math.max(i0, i1); k++)
            if (Range.pointIsInRanges(k, j, ranges))
                return true;
        return false;
    }

    /**
     * Возвращает число единиц в прямоугольнике из таблицы {@code table}, ограниченном верхней левой точкой
     * ({@code i1}, {@code j1}) и нижней правой точкой ({@code i2}, {@code j2}).
     */
    private static int amountOfOnes(int[][] table, int i1, int j1, int i2, int j2) {
        int count = 0;
        for (int i = i1; i <= i2; i++)
            for (int j = j1; j <= j2; j++)
                if (table[i][j] == 1)
                    count++;
        return count;
    }

    /**
     * Возвращает прямоугольник, чья верхняя левая вершина примерно совпадает с точкой ({@code i}, {@code j}), на
     * основании таблицы {@code table} и списка уже построенных прямоугольников {@code ranges}.
     */
    private static Integer[] makeRange(int[][] table, int i, int j, List<Integer[]> ranges) {
        int x = i, y = j;
        boolean incrementX, incrementY;
        do {
            incrementX = false;
            incrementY = false;
            if (x + 1 < table.length &&
                    amountOfOnes(table, i, j, x + 1, y) - amountOfOnes(table, i, j, x, y) > (y - j + 1) / 2) {
                x++;
                incrementX = true;
            }
            if (y + 1 < table[0].length &&
                    amountOfOnes(table, i, j, x, y + 1) - amountOfOnes(table, i, j, x, y) > (x - i + 1) / 2 &&
                    !Range.verticalLineIntersectsRanges(i, x, y + 1, ranges)) {
                y++;
                incrementY = true;
            }
        } while (incrementX || incrementY);
        if (amountOfOnes(table, i, j, i, y) < (y - j + 1) / 2 && i + 1 < table.length)
            i++;
        if (amountOfOnes(table, i, j, x, j) < (x - i + 1) / 2 && j + 1 < table[0].length)
            j++;
        return new Integer[]{i, j, x, y};
    }

    /**
     * Возвращает список прямоугольников, созданных на основании таблицы {@code table}.
     */
    static List<Integer[]> findRanges(int[][] table) {
        List<Integer[]> ranges = new ArrayList<>();
        Integer[] range;
        for (int i = 0; i < table.length; i++)
            for (int j = 0; j < table[0].length; j++)
                if (table[i][j] == 1 && !(Range.pointIsInRanges(i, j, ranges))) {
                    range = makeRange(table, i, j, ranges);
                    if (!Range.isLine(range))
                        ranges.add(range);
                }
        return ranges;
    }
}
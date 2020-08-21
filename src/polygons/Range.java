package polygons;

import javenue.csv.Csv;
import main.NewClass;
import main.Pixel;
import main.Rectangle;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Переменная x в системе координат Oxy переименована в i, а y - в j.
 * <p>
 * Стороны прямоугольника параллельны координатным осям, и он задаётся двумя вершинами: верхней левой и нижней правой.
 */
public class Range {
    /**
     * Верхняя левая вершина прямоугольника.
     */
    private final Point upperLeft;
    /**
     * Нижняя правая вершина прямоугольника.
     */
    private final Point lowerRight;

    /**
     * Минимальная температура.
     */
    public static final double T_MIN = 30;
    /**
     * Разрешение по оси Oi.
     */
    public static final int RES_I = 512;
    /**
     * Разрешение по оси Oj.
     */
    public static final int RES_J = 640;
    /**
     * Минимальная площадь прямоугольника (в кв. пикселях).
     */
    public static final int MIN_SQUARE_PIXELS = 25;

    public Range(Point upperLeft, Point lowerRight) {
        this.upperLeft = upperLeft;
        this.lowerRight = lowerRight;
    }

    public Point getUpperLeft(){
        return upperLeft;
    }

    public Point getLowerRight(){
        return lowerRight;
    }

    /**
     * Возвращает представление файла с названием {@code filename} в виде таблицы.
     */
    public static List<List<String>> extractRawTable(String filename) throws FileNotFoundException {
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
    public static List<List<String>> extractTable(List<List<String>> rawTable) {
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
     * Возвращает таблицу целых чисел, построенную на основании таблицы {@code table} (которая содержит строковые
     * представления чисел) и предиката {@code predicate}.
     * Если элемент таблицы {@code table} удовлетворяет критерию, задаваемому предикатом {@code predicate}, то на этом
     * же месте в таблице целых чисел пишется значение {@code 1}, иначе остаётся значение по умолчанию {@code 0}.
     */
    public static int[][] findIf(List<List<String>> table, Predicate<Double> predicate) {
        int[][] newTable = new int[table.size()][table.get(0).size()];
        for (int i = 0; i < table.size(); i++)
            for (int j = 0; j < table.get(0).size(); j++)
                if (predicate.test(new Double(table.get(i).get(j).replace(',', '.'))))
                    newTable[i][j] = 1;
        return newTable;
    }

    /**
     * Печатает таблицу {@code table}.
     */
    public static void printTable(List<List<String>> table) {
        for (List<String> line : table)
            System.out.println(line);
    }

    /**
     * Печатает таблицу {@code table}.
     */
    public static void printTable(int[][] table) {
        for (int[] line : table) {
            for (int num : line)
                System.out.print(num);
            System.out.println();
        }
    }

    /**
     * Определяет, является ли текущий прямоугольник горизонтальной или вертикальной линией.
     */
    private boolean isLine() {
        return upperLeft.getX() == lowerRight.getX() || upperLeft.getY() == lowerRight.getY();
    }

    /**
     * Определяет принадлежность точки {@code point} текущему прямоугольнику.
     */
    public boolean containsPoint(Point point) {
        return (upperLeft.getX() <= point.getX() && point.getX() <= lowerRight.getX()) &&
                (upperLeft.getY() <= point.getY() && point.getY() <= lowerRight.getY());
    }

    /**
     * Возвращает число точек в текущем прямоугольнике.
     */
    public int squarePixels() {
        return (lowerRight.getX() - upperLeft.getX() + 1) * (lowerRight.getY() - upperLeft.getY() + 1);
    }

    /**
     * Конвертирует текущий прямоугольник из системы координат Oij в систему координат c'x'y'.
     */
    public Rectangle toRectangle() {
        return new Rectangle(new Pixel(upperLeft.getY(), NewClass.RES_Y - lowerRight.getX()), new Pixel(lowerRight.getY(), NewClass.RES_Y - upperLeft.getX()));
    }

    /**
     * Определяет, пересекает ли вертикальная линия с концами ({@code i0}, {@code j}) и ({@code i1}, {@code j})
     * какой-нибудь прямоугольник из списка {@code ranges}.
     */
    private static boolean verticalLineIntersectsRanges(int i0, int i1, int j, List<Range> ranges) {
        for (int k = Math.min(i0, i1); k <= Math.max(i0, i1); k++)
            if (new Point(k, j).isInRanges(ranges))
                return true;
        return false;
    }

    /**
     * Возвращает число единиц в текущем прямоугольнике из таблицы {@code table}.
     */
    private int amountOfOnes(int[][] table) {
        int count = 0;
        for (int i = upperLeft.getX(); i <= lowerRight.getX(); i++)
            for (int j = upperLeft.getY(); j <= lowerRight.getY(); j++)
                if (table[i][j] == 1)
                    count++;
        return count;
    }

    /**
     * Возвращает прямоугольник, чья верхняя левая вершина примерно совпадает с точкой {@code point}, на основании
     * таблицы {@code table} и списка уже построенных прямоугольников {@code ranges}.
     */
    private static Range makeRange(int[][] table, Point point, List<Range> ranges) {
        int x = point.getX(), y = point.getY();
        boolean incrementX, incrementY;
        do {
            incrementX = false;
            incrementY = false;
            if (x + 1 < table.length &&
                    new Range(point, new Point(x + 1, y)).amountOfOnes(table) - new Range(point, new Point(x, y)).amountOfOnes(table) > (y - point.getY() + 1) / 2) {
                x++;
                incrementX = true;
            }
            if (y + 1 < table[0].length &&
                    new Range(point, new Point(x, y+1)).amountOfOnes(table) - new Range(point, new Point(x, y)).amountOfOnes(table) > (x - point.getX() + 1) / 2 &&
                    !Range.verticalLineIntersectsRanges(point.getX(), x, y + 1, ranges)) {
                y++;
                incrementY = true;
            }
        } while (incrementX || incrementY);
        int newI = point.getX();
        int newJ = point.getY();
        if (new Range(point, new Point(point.getX(), y)).amountOfOnes(table) < (y - point.getY() + 1) / 2 && point.getX() + 1 < table.length)
            newI++;
        if (new Range(point, new Point(x, point.getY())).amountOfOnes(table) < (x - point.getX() + 1) / 2 && point.getY() + 1 < table[0].length)
            newJ++;
        return new Range(new Point(newI, newJ), new Point(x,y));
    }

    /**
     * Возвращает список прямоугольников, созданных на основании таблицы {@code table}.
     */
    public static List<Range> findRanges(int[][] table) {
        List<Range> ranges = new ArrayList<>();
        Range range;
        for (int i = 0; i < table.length; i++)
            for (int j = 0; j < table[0].length; j++)
                if (table[i][j] == 1 && !(new Point(i, j).isInRanges(ranges))) {
                    range = makeRange(table, new Point(i, j), ranges);
                    if (!range.isLine())
                        ranges.add(range);
                }
        return ranges;
    }
}
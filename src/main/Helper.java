package main;

import javenue.csv.Csv;
import polygons.Line;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Helper {
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
     * Возвращает индекс первого вхождения минимального числа в списке {@code list}.
     */
    public static int findIndexOfMin(List<Integer> list) {
        int index = 0;
        int min = list.get(index);
        for (int i = 1; i < list.size(); i++)
            if (list.get(i) < min) {
                index = i;
                min = list.get(index);
            }
        return index;
    }

    /**
     * Возвращает массив, состоящий из массива {@code array} с удалённым элементом с индексом {@code index} и со
     * сдвинутыми влево элементами.
     */
    public static Line[] deleteWithShift(Line[] array, int index) {
        Line[] result = new Line[array.length - 1];
        System.arraycopy(array, 0, result, 0, index);
        System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        return result;
    }

    /**
     * Определяет принадлежность значения {@code val0} списку {@code list}.
     */
    public static boolean isIn(List<Integer> list, int val0) {
        for (Integer val : list)
            if (val == val0)
                return true;
        return false;
    }
}
package main;

import java.util.ArrayList;
import java.util.List;


public class Range {
    /**
     * Определяет, является ли указанный прямоугольник {@param range} горизонтальной или вертикальной линией.
     */
    private static boolean isLine(Integer[] range) {
        return range[0].equals(range[2]) || range[1].equals(range[3]);
    }

    /**
     * Определяет, принадлежит ли точка ({@param i0}, {@param j0}) какому-нибудь прямоугольнику из списка
     * {@param ranges}.
     */
    private static boolean pointIsInRanges(int i0, int j0, List<Integer[]> ranges) {
        for (Integer[] range : ranges)
            if (i0 >= range[0] && i0 <= range[2] && j0 >= range[1] && j0 <= range[3])
                return true;
        return false;
    }

    /**
     * Определяет, пересекает ли вертикальная линия с концами ({@param i0}, {@param j}) и ({@param i1}, {@param j})
     * какой-нибудь прямоугольник из списка {@param ranges}.
     */
    private static boolean verticalLineIntersectsRanges(int i0, int i1, int j, List<Integer[]> ranges) {
        for (int k = Math.min(i0, i1); k <= Math.max(i0, i1); k++)
            if (Range.pointIsInRanges(k, j, ranges))
                return true;
        return false;
    }

    /**
     * Возвращает число единиц в прямоугольнике из таблицы {@param table}, ограниченном верхней левой точкой
     * ({@param i1}, {@param j1}) и нижней правой точкой ({@param i2}, {@param j2}).
     */
    private static int amountOfOnes(List<List<Integer>> table, int i1, int j1, int i2, int j2) {
        int count = 0;
        for (int i = i1; i <= i2; i++)
            for (int j = j1; j <= j2; j++)
                if (table.get(i).get(j) == 1)
                    count++;
        return count;
    }

    /**
     * Возвращает прямоугольник, чья верхняя левая вершина примерно совпадает с точкой ({@param i}, {@param j}), на
     * основании таблицы {@param table} и списка уже построенных прямоугольников {@param ranges}.
     */
    private static Integer[] makeRange(List<List<Integer>> table, int i, int j, List<Integer[]> ranges) {
        int x = i, y = j;
        boolean incrementX, incrementY;
        do {
            incrementX = false;
            incrementY = false;
            if (amountOfOnes(table, i, j, x + 1, y) - amountOfOnes(table, i, j, x, y) > (y - j) / 2) {
                x++;
                incrementX = true;
            }
            if (amountOfOnes(table, i, j, x, y + 1) - amountOfOnes(table, i, j, x, y) > (x - i) / 2 &&
                    !Range.verticalLineIntersectsRanges(i, x, y + 1, ranges)) {
                y++;
                incrementY = true;
            }
        } while (incrementX || incrementY);
        return new Integer[]{i, j, x, y};
    }

    /**
     * Возвращает список прямоугольников, созданных на основании таблицы {@param table}.
     */
    static List<Integer[]> findRanges(List<List<Integer>> table) {
        List<Integer[]> ranges = new ArrayList<>();
        Integer[] range;
        for (int i = 0; i < table.size(); i++)
            for (int j = 0; j < table.get(0).size(); j++)
                if (table.get(i).get(j) == 1 && !(Range.pointIsInRanges(i, j, ranges))) {
                    range = makeRange(table, i, j, ranges);
                    if (!Range.isLine(range))
                        ranges.add(range);
                }
        return ranges;
    }
}
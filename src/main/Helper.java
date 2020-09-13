package main;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import javenue.csv.Csv;
import polygons.Segment;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.*;

public class Helper {
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
        int[][] binTable = new int[table.size()][table.get(0).size()];
        for (int i = 0; i < table.size(); i++)
            for (int j = 0; j < table.get(0).size(); j++)
                if (predicate.test(new Double(table.get(i).get(j).replace(',', '.'))))
                    binTable[i][j] = 1;
        return binTable;
    }

    /**
     *
     */
    public static int[][] findIf(double[][] table, Predicate<Double> predicate) {
        int[][] binTable = new int[table.length][table[0].length];
        for (int i = 0; i < binTable.length; i++)
            for (int j = 0; j < binTable[0].length; j++)
                if (predicate.test(table[i][j]))
                    binTable[i][j] = 1;
        return binTable;
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
    public static Segment[] deleteWithShift(Segment[] array, int index) {
        Segment[] result = new Segment[array.length - 1];
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

    public static JsonObject getJsonObject(String filename) {
        JsonObject jsonObject = null;
        try {
            jsonObject = (JsonObject) (new JsonParser().parse(new FileReader(filename)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * Конвертирует необработанное температурное значение {@code rawValue} в температуру.
     */
    public static double rawValueToReal(int rawValue, double[] exifParams) {
        double planckR1 = exifParams[0];
        double planckR2 = exifParams[1];
        double planckO = exifParams[2];
        double planckB = exifParams[3];
        double planckF = exifParams[4];
        double emissivity = exifParams[5];
        double tRefl = exifParams[6] + 273.15;

        double rawRefl = planckR1 / (planckR2 * (pow(E, planckB / tRefl) - planckF)) - planckO;
        double rawObj = (rawValue - (1 - emissivity) * rawRefl) / emissivity;
        return planckB / log(planckR1 / (planckR2 * (rawObj + planckO)) + planckF) - 273.15;
    }

    /**
     * Конвертирует таблицу необработанных температурных данных {@code rawTable} в таблицу температур.
     */
    public static double[][] rawTableToReal(int[][] rawTable, double[] exifParams) {
        double[][] realTable = new double[rawTable.length][rawTable[0].length];
        for (int i = 0; i < realTable.length; i++)
            for (int j = 0; j < realTable[0].length; j++)
                realTable[i][j] = Helper.rawValueToReal(rawTable[i][j], exifParams);
        return realTable;
    }

    /**
     * Извлекает таблицу размером {@code height x width} необработанных температурных данных из файла {@code filename}.
     */
    public static int[][] extractRawTable(String filename, int height, int width) {
        int[][] rawTable = new int[height][width];
        FileReader reader = null;
        List<String[]> allData = null;

        try {
            reader = new FileReader(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        CSVParser parser = new CSVParserBuilder().withSeparator(' ').build();
        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withCSVParser(parser)
                .build();
        try {
            allData = csvReader.readAll();
        } catch (IOException e) {
            e.printStackTrace();
        }

        int i0 = 0;
        int j0 = 0;
        for (int i = 0; i < rawTable.length; i++)
            for (int j = 0; j < rawTable[0].length; j++) {
                rawTable[i][j] = new Integer(allData.get(i0)[j0]);
                if (j0 + 1 < allData.get(i0).length - 1)
                    j0++;
                else {
                    j0 = 0;
                    i0++;
                }
            }
        return rawTable;
    }
}
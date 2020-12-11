package main;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.opencsv.*;
import javenue.csv.Csv;
import polygons.Segment;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.*;


public final class Helper {
    /**
     * Операционные системы.
     */
    public enum Os {
        WINDOWS("Windows"),
        LINUX("Linux");

        private final String name;

        Os(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Os getByName(String name) {
            return switch (name) {
                case "Windows" -> WINDOWS;
                case "Linux" -> LINUX;
                default -> throw new IllegalStateException("Unexpected value: " + name);
            };
        }
    }

    private Helper() {
    }

    /**
     * Возвращает представление файла с названием {@code filename} в виде таблицы.
     */
    public static List<List<String>> extractTable(String filename) throws FileNotFoundException {
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
     * Аналогично методу {@link main.Helper#findIf(List, Predicate)}.
     */
    public static int[][] findIf(double[][] table, Predicate<Double> predicate) {
        int[][] binTable = new int[table.length][table[0].length];
        for (int i = 0; i < table.length; i++)
            for (int j = 0; j < table[0].length; j++)
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
     *
     * @throws IllegalArgumentException если список пуст
     */
    public static int findIndexOfMin(List<Integer> list) {
        if (list.isEmpty()) throw new IllegalArgumentException("Empty list.");
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
     * Сравнивает числа {@code a} и {@code b}; если оба равны {@link Double#NaN}, то возвращает {@code true}.
     */
    public static boolean compare(Double a, Double b) {
        return a.equals(b) || a.isNaN() && b.isNaN();
    }

    /**
     * Определяет, является ли угол между прямыми с углами наклона {@code angle1} и {@code angle2} не больше значения
     * {@code eps} град. Углы наклона (в град.) отсчитываются от положительного направления оси c'x' против часовой
     * стрелки и принадлежат промежутку {@code [0,180)}.
     */
    public static boolean close(double angle1, double angle2, double eps) {
        return abs(angle1 - angle2) <= eps || abs(angle1 - angle2) >= 180 - eps;
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

    /**
     * Возвращает индекс первого вхождения максимального элемента среди элементов массива {@code nums} с индексами из
     * списка {@code indices}.
     * Этот список должен состоять из индексов данного массива. Если список пуст, то возвращает {@code -1}.
     */
    public static int findIndexOfMax(double[] nums, List<Integer> indices) {
        double max = Double.NEGATIVE_INFINITY;
        int indexMax = -1;
        for (int i : indices)
            if (nums[i] > max) {
                indexMax = i;
                max = nums[indexMax];
            }
        return indexMax;
    }

    public static String filename(Object... dir) {
        StringBuilder filename = new StringBuilder();
        for (Object o : dir) filename.append("/").append(o);
        return filename.substring(1);
    }

    /**
     * Записывает строку {@code str} (вместе с символом новой строки) в конец файла с названием {@code filename}.
     */
    public static void log(String filename, String str) {
        writeToFile(filename, str, true);
    }

    /**
     * Записывает строку {@code str} (вместе с символом новой строки) в файл с названием {@code filename}, перезаписывая
     * его.
     */
    public static void write(String filename, String str) {
        writeToFile(filename, str, false);
    }

    /**
     * Очищает файлы с названиями из массива {@code filenames}.
     */
    public static void clear(String... filenames) {
        for (String f : filenames)
            write(f, "");
    }

    public static void deleteDirectory(String path) {
        try (Stream<Path> walk = Files.walk(Paths.get(path))) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeToFile(String filename, String str, boolean append) {
        try {
            FileWriter writer = new FileWriter(filename, append);
            writer.write(str + "\n");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void concatenateFiles(String filename, String dir) {
        List<String> filenames = Arrays.stream(Objects.requireNonNull(new File(dir).list()))
                .sorted(Comparator.naturalOrder()).collect(Collectors.toList());
        for (String f : filenames)
            addFile(filename, dir + f, true);
    }

    public static void addFile(String filename, String src, boolean append) {
        try {
            FileWriter writer = new FileWriter(filename, append);
            BufferedReader reader = new BufferedReader(new FileReader(src));

            String line = reader.readLine();

            while (line != null) {
                writer.write(line + "\n");
                line = reader.readLine();
            }

            writer.flush();

            reader.close();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createDirectories(String... dirs) {
        for (String dir : dirs)
            try {
                Files.createDirectory(Paths.get(dir));
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public static String shortFilenameWithoutExtension(String filename) {
        return filename.substring(filename.lastIndexOf('/') + 1, filename.lastIndexOf('.'));
    }

    public static String addPostfixToFilename(String dir, String srcFilename, String postfix) {
        String srcShortFilenameWithoutExtension = shortFilenameWithoutExtension(srcFilename);
        String srcExtension = srcFilename.substring(srcFilename.lastIndexOf('.'));
        return Helper.filename(dir, srcShortFilenameWithoutExtension + postfix + srcExtension);
    }

    /**
     * @param filename файл в формате JSON
     */
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
    public static double rawValueToReal(int rawValue, double[] params) {
        double planckR1 = params[0];
        double planckR2 = params[1];
        double planckO = params[2];
        double planckB = params[3];
        double planckF = params[4];
        double emissivity = params[5];
        double tRefl = params[6];

        double rawRefl = planckR1 / (planckR2 * (pow(E, planckB / tRefl) - planckF)) - planckO;
        double rawObj = (rawValue - (1 - emissivity) * rawRefl) / emissivity;
        return planckB / Math.log(planckR1 / (planckR2 * (rawObj + planckO)) + planckF) - 273.15;
    }

    /**
     * Конвертирует таблицу необработанных температурных данных {@code rawTable} в таблицу температур.
     */
    public static double[][] rawTableToReal(int[][] rawTable, double[] params) {
        double[][] realTable = new double[rawTable.length][rawTable[0].length];
        for (int i = 0; i < rawTable.length; i++)
            for (int j = 0; j < rawTable[0].length; j++)
                realTable[i][j] = rawValueToReal(rawTable[i][j], params);
        return realTable;
    }

    /**
     * Извлекает таблицу размером {@code height x width} из файла {@code filename} в формате CSV с разделителем
     * {@code separator}, содержащего по крайней мере {@code height*width} значений. Фактически, формирование таблицы
     * происходит при помощи массива всех последовательных значений указанного файла. Число строк в нём и их длины
     * произвольны.
     */
    public static int[][] extractTable(String filename, int height, int width, char separator) {
        int[][] table = new int[height][width];
        FileReader reader = null;
        List<String[]> allData = null;

        try {
            reader = new FileReader(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
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
        for (int i = 0; i < table.length; i++)
            for (int j = 0; j < table[0].length; j++) {
                //table[i][j] = Integer.parseInt(allData.get(i0)[j0]);
                table[i][j] = new Integer(allData.get(i0)[j0]);
                if (j0 + 1 < allData.get(i0).length - 1)
                    j0++;
                else {
                    j0 = 0;
                    i0++;
                }
            }
        return table;
    }

    /**
     * В таблице {@code table} ставит значение {@code 0} в тех позициях, которые принадлежат хотя бы одному
     * прямоугольнику из списка {@code rectangles}.
     * Точка ({@code i}, {@code j}) системы координат c'x'y' соответствует позиции ({@code RES_Y-1-j}, {@code i}) в
     * таблице.
     */
    static void nullifyRectangles(int[][] table, List<Rectangle<Pixel>> rectangles, int resY) {
        if (rectangles != null)
            for (Rectangle<Pixel> rectangle : rectangles)
                for (int i = rectangle.getLeft().getI(); i <= rectangle.getRight().getI(); i++)
                    for (int j = rectangle.getLeft().getJ(); j <= rectangle.getRight().getJ(); j++)
                        table[resY - 1 - j][i] = 0;
    }

    /**
     * Конвертирует массив {@code array} типа {@code double[]} в массив типа {@code String[]}.
     */
    public static String[] toStringArray(double[] array) {
        String[] newArray = new String[array.length];
        for (int i = 0; i < array.length; i++)
            newArray[i] = String.valueOf(array[i]);
        return newArray;
    }

    /**
     * Записывает таблицу {@code table} в файл {@code filename} в формате CSV с разделителем {@code separator}.
     */
    public static void writeAsCsv(double[][] table, char separator, String filename) {
        List<String[]> entries = new ArrayList<>();
        for (double[] line : table)
            entries.add(toStringArray(line));
        try {
            try (FileOutputStream fos = new FileOutputStream(filename);
                 OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                 CSVWriter writer = new CSVWriter(osw, separator, CSVWriter.DEFAULT_QUOTE_CHARACTER,
                         CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)) {
                writer.writeAll(entries, false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Записывает таблицу температур в файл {@code realFilename} в формате CSV с разделителем {@code realSeparator},
     * вычисляемую на основании таблицы размером {@code height x width} необработанных температурных данных, извлечённой
     * из файла {@code rawFilename} в формате CSV с разделителем {@code rawSeparator}.
     */
    public static void rawFileToRealFile(String rawFilename, String realFilename, int height, int width,
                                         char rawSeparator, char realSeparator, double[] params) {
        int[][] rawTable = Helper.extractTable(rawFilename, height, width, rawSeparator);
        double[][] realTable = Helper.rawTableToReal(rawTable, params);
        Helper.writeAsCsv(realTable, realSeparator, realFilename);
    }

    /**
     * Извлекает таблицу из файла {@code filename} в формате CSV с разделителем {@code separator}. Все строки в этом
     * файле должны иметь одинаковую длину.
     */
    public static double[][] extractTable(String filename, char separator) {
        FileReader reader = null;
        List<String[]> allData = null;

        try {
            reader = new FileReader(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        CSVParser parser = new CSVParserBuilder().withSeparator(separator).build();
        CSVReader csvReader = new CSVReaderBuilder(reader)
                .withCSVParser(parser)
                .build();
        try {
            allData = csvReader.readAll();
        } catch (IOException e) {
            e.printStackTrace();
        }

        double[][] table = new double[allData.size()][allData.get(0).length];
        for (int i = 0; i < allData.size(); i++)
            for (int j = 0; j < allData.get(0).length; j++)
                table[i][j] = Double.parseDouble(allData.get(i)[j]);
        return table;
    }

    /**
     * Запускает пакетный файл Windows или bash-скрипт Linux {@code scriptname}, находящийся в папке {@code dir}, из
     * рабочей директории {@code dir}.
     *
     * @param params необязательный список параметров командной строки для скрипта
     */
    public static void run(String dir, String scriptname, String os, String... params) {
        String command = switch (Os.getByName(os)) {
            case WINDOWS -> "cmd /C start";
            case LINUX -> "/bin/bash";
        } + " " + scriptname;

        for (String param : params)
            command += " " + param;

        try {
            Runtime.getRuntime().exec(command, null, new File(dir)).waitFor();
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }
}
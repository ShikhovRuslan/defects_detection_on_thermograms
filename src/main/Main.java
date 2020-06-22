package main;

import javenue.csv.Csv;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*

http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.

 */

class Main {
    private static final String FILENAME = "/home/ruslan/geo" + "/file.txt";
    private static final double T_MIN = 30;
    private static final double T_MAX = 100;
    private static final int HEIGHT = 250;
    private static final int RES_X = 640;
    private static final int RES_Y = 512;

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
                    if (!found && (i == fromIndex + 1 || i == fromIndex + 2)) break;
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
        for (int i = i1; i <= Math.min(i2,table.size()-1); i++)
            for (int j = j1; j <= Math.min(j2,table.get(0).size()-1); j++)
                if (table.get(i).get(j) == 1) count++;
        return count;
    }

    private static List<Integer> squarePixels (List<Integer[]> list) {
        List<Integer> result=new ArrayList<>();
        for(Integer[] ints : list)
            result.add((ints[2]-ints[0] + 1) * (ints[3]-ints[1] + 1));
        return result;
    }

    private static int[] rectangle(List<List<Integer>> table, int i, int j) {
        int x = i, y = j;
        boolean incrementX, incrementY;
        do {
            incrementX = false;
            incrementY = false;
            if (amountOfOnes(table, i, j, x, y + 1) - amountOfOnes(table, i, j, x, y) > (x - i) / 2) {
                y++;
                incrementY = true;
            }
            if (amountOfOnes(table, i, j, x + 1, y) - amountOfOnes(table, i, j, x, y) > (y - j) / 2) {
                x++;
                incrementX = true;
            }
        } while (incrementX || incrementY);
        return new int[]{x, y};
    }

    private static boolean isIn(int i0, int j0, List<Integer[]> arr) {
        for (Integer[] f : arr)
            if (i0 >= f[0] && i0 <= f[2] && j0 >= f[1] && j0 <= f[3]) return true;
        return false;
    }

    private static List<Integer[]> find(List<List<Integer>> table) {
        int[] coords;
        List<Integer[]> result = new ArrayList<>();
        for (int i = 0; i < table.size(); i++) {
            for (int j = 0; j < table.get(0).size(); j++) {
                if (table.get(i).get(j) == 1 & !(isIn(i, j, result))) {
                    coords = rectangle(table, i, j);
                    result.add(new Integer[]{i, j, coords[0], coords[1]});
                }
            }
        }
        return result;
    }

    private static List<Integer> abc (int[] arr) {
        List<Integer> line = new ArrayList<>();
        for(int a : arr) {
            line.add(a);
        }
        return line;
    }

    private static List<List<Integer>> arrayToList (int[][] arr) {
        List<List<Integer>> table = new ArrayList<>();
        for(int[] f : arr)
            table.add(abc(f));
        return table;
    }

    private static void f() throws FileNotFoundException {
        List<List<String>> rawTable = extractRawTable(FILENAME);
        List<List<String>> table = extractTable(rawTable);
        //printTable(table);
        int[][] arr = findIf(table, num -> num > T_MIN);
        printTable(arr);
        List<Integer[]> res = find(arrayToList(arr));
        System.out.println(Arrays.deepToString(res.toArray()));
        System.out.println(Arrays.toString(squarePixels(res).toArray()));
        String filename = "/home/ruslan/geo" + "/тест.csv";
        Image image = null;
        try {
            image = ImageIO.read(new File(filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        //new Graphics().drawImage(image, x, y, null);
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
        } catch (FileNotFoundException e) {
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
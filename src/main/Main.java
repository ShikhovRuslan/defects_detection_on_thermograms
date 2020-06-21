package main;

import javenue.csv.Csv;

import java.io.*;
import java.util.ArrayList;
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

    private static void f() throws FileNotFoundException {
        List<List<String>> rawTable = extractRawTable(FILENAME);
        List<List<String>> table = extractTable(rawTable);
        //printTable(table);
        int[][] arr = findIf(table, num -> num > T_MIN);
        printTable(arr);
    }

    private static void find(List<List<Integer>> table) {
        for(int i = 0; i<table.size(); i++){
            for(int j = 0; j<table.get(0).size(); j++){

            }
        }
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

//        try {
//            f();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        List<List<Integer>> table = new ArrayList<>();
        try {
            File file = new File("/home/ruslan/geo" + "/file2.txt");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            table = new ArrayList<>();
            int i = 0;
            do {
                line = reader.readLine();
                for(int j = 0; j<line.length();j++){
                    table.add(new ArrayList<>());
                    table.get(i).add(line.toCharArray()[j] - '0');
                }
                i++;
            } while (!line.equals(""));
        } catch (IOException e) {
            e.printStackTrace();
        }
        int y = 0;
        List<List<Integer>> l = table;
        find(table);
    }
}
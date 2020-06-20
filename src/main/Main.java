package main;

import javenue.csv.Csv;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*

http://www.javenue.info/post/78 - чтение и запись CSV файлов в Java.

 */

class Main {
    private static final String FILENAME = "/home/ruslan/geo" + "/file.txt";
    private static final int T_MIN = 30;
    private static final int T_MAX = 100;
    private static final int HEIGHT = 250;
    private static final int RES_X = 640;
    private static final int RES_Y = 512;

    private static void printTable(List<List<String>> table) {
        for (List<String> line : table) {
            System.out.println(line);
        }
    }

    private static List<List<String>> extractTable(List<List<String>> table) {
        Pattern pattern = Pattern.compile("-?\\d{1,2},\\d{1,3}");
        Matcher matcher;
        List<List<String>> result = new ArrayList<>();
        int startingIndex = 0;
        int count = 0;
        boolean lineWithNumbers = false;
        for (List<String> line : table) {
            if (line != null) {
                for (int i = 0; i < line.size(); i++) {
                    matcher = pattern.matcher(line.get(i));
                    // Желательно, чтобы последовательно находить совпадения.
                    if (matcher.find()) count++;
                    if (count == 1) startingIndex = i;
                    if (count >= 3) {
                        lineWithNumbers = true;
                        break;
                    }
                }
                if (lineWithNumbers) result.add(line.subList(startingIndex, line.size()));
                count = 0;
                lineWithNumbers = false;
            }
        }
        return result;
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

    private static int[][] detectHighTemp(List<List<String>> table) {
        int[][] arr = new int[table.size()][table.get(0).size()];
        for (int i = 0; i < table.size(); i++)
            for (int j = 0; j < table.get(i).size(); j++)
                if (new Double(table.get(i).get(j)) > T_MIN) arr[i][j] = 1;
        return arr;
    }

    private static void f() throws FileNotFoundException {
        List<List<String>> rawTable = extractRawTable(FILENAME);
        List<List<String>> table = extractTable(rawTable);
        printTable(table);
        int [][] arr = detectHighTemp(table);
        //printTable(arr);
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
    }
}
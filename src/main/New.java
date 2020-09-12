package main;

import com.google.gson.*;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.*;
import java.util.List;

import static java.lang.Math.*;


/**
 * https://exiftool.org/forum/index.php/topic,4898.msg23972.html#msg23972
 */
public class New {
    public enum Param {
        PLANCK_R1("PlanckR1"),
        PLANCK_R2("PlanckR2"),
        PLANCK_O("PlanckO"),
        PLANCK_B("PlanckB"),
        PLANCK_F("PlanckF"),
        EMISSIVITY("Emissivity"),
        REFLECTED_APPARENT_TEMPERATURE("ReflectedApparentTemperature"),
        RAW_THERMAL_IMAGE_HEIGHT("RawThermalImageHeight"),
        RAW_THERMAL_IMAGE_WIDTH("RawThermalImageWidth");

        private final String rawName;

        Param(String rawName) {
            this.rawName = rawName;
        }

        private String getRawName() {
            return rawName;
        }
    }

    public static JsonObject getJsonObject(String filename) throws Exception {
        return (JsonObject) (new JsonParser().parse(new FileReader(filename)));
    }

    public static double getParam(String filename, Param param) {
        double value = -1_000_000_000.0;
        try {
            value = getJsonObject(filename).get(param.getRawName()).getAsDouble();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static double[] getParams(String filename) {
        double[] params = new double[Param.values().length];
        for (int i = 0; i < Param.values().length; i++)
            params[i] = getParam(filename, Param.values()[i]);
        return params;
    }

    public static double convert(int num, double[] params) {
        double planckR1 = params[0];
        double planckR2 = params[1];
        double planckO = params[2];
        double planckB = params[3];
        double planckF = params[4];
        double emissivity = params[5];
        double tRefl = params[6] + 273.15;

        double rawRefl = planckR1 / (planckR2 * (pow(E, planckB / tRefl) - planckF)) - planckO;
        double rawObj = (num - (1 - emissivity) * rawRefl) / emissivity;
        return planckB / log(planckR1 / (planckR2 * (rawObj + planckO)) + planckF) - 273.15;
    }

    public static int[][] read(String file, int height, int width) {
        int[][] table = new int[height][width];
        FileReader filereader = null;
        List<String[]> allData = null;

        try {
            filereader = new FileReader(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        CSVParser parser = new CSVParserBuilder().withSeparator(' ').build();
        CSVReader csvReader = new CSVReaderBuilder(filereader)
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
                table[i][j] = new Integer(allData.get(i0)[j0]);
                if (j0 + 1 < allData.get(i0).length - 1)
                    j0++;
                else {
                    j0 = 0;
                    i0++;
                }
            }

//        for (int[] line : table) {
//            for (int str : line)
//                System.out.print(str + " ");
//            System.out.println();
//        }
        return table;
    }

    public static double[][] convertTable(int[][] intTable, double[] params) {
        double[][] doubleTable = new double[intTable.length][intTable[0].length];
        for (int i = 0; i < doubleTable.length; i++)
            for (int j = 0; j < doubleTable[0].length; j++)
                doubleTable[i][j] = convert(intTable[i][j], params);
//        for (double[] line : doubleTable) {
//            for (double str : line)
//                System.out.print(str + " ");
//            System.out.println();
//        }
        return doubleTable;
    }

    public static void main(String[] args) {
        String filename = "C:\\Users\\shikh\\Documents\\Geo\\DJI_0835_R_raw.pgm";
        String file = "C:\\Users\\shikh\\Documents\\Geo\\params.txt";

        int[][] intTable = read(filename,
                (int) getParam(file, Param.RAW_THERMAL_IMAGE_HEIGHT),
                (int) getParam(file, Param.RAW_THERMAL_IMAGE_WIDTH));
        for (int[] line : intTable) {
            for (int num : line)
                System.out.print(num + " ");
            System.out.println();
        }

        double[][] table = convertTable(intTable, getParams(file));
        for (double[] line : table) {
            for (double num : line)
                System.out.print(num + " ");
            System.out.println();
        }
    }
}
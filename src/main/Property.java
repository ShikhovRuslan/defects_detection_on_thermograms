package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


/**
 * Содержит параметры из конфигурационного файла {@code SHORT_FILENAME_CONFIG}.
 */
public enum Property {
    DIR_THERMOGRAMS("DIR_THERMOGRAMS"),

    SUBDIR_OUTPUT("SUBDIR_OUTPUT"),

    SUBDIR_AUXILIARY("SUBDIR_AUXILIARY"),

    SUBDIR_RAW_TEMPS("SUBDIR_RAW_TEMPS"),
    POSTFIX_RAW_TEMPS("POSTFIX_RAW_TEMPS"),

    SUBDIR_REAL_TEMPS("SUBDIR_REAL_TEMPS"),
    POSTFIX_REAL_TEMPS("POSTFIX_REAL_TEMPS"),

    SUBDIR_RAW_DEFECTS("SUBDIR_RAW_DEFECTS"),
    POSTFIX_RAW_DEFECTS("POSTFIX_RAW_DEFECTS"),

    SUBDIR_DEFECTS("SUBDIR_DEFECTS"),
    POSTFIX_DEFECTS("POSTFIX_DEFECTS"),

    PIXEL_SIZE("PIXEL_SIZE"),
    PRINCIPAL_POINT_X("PRINCIPAL_POINT_X"),
    PRINCIPAL_POINT_Y("PRINCIPAL_POINT_Y"),
    DIAMETER("DIAMETER"),
    T_MIN("T_MIN"),
    T_MAX("T_MAX"),
    MIN_PIXEL_SQUARE("MIN_PIXEL_SQUARE"),
    MIN_INTERSECTION_SQUARE("MIN_INTERSECTION_SQUARE"),

    K1("K1"),
    K2("K2"),
    K3("K3"),
    DEFAULT_PIPE_ANGLES("DEFAULT_PIPE_ANGLES"),

    DISTANCE("DISTANCE"),
    T_MIN_PSEUDO("T_MIN_PSEUDO"),
    MAX_DIFF("MAX_DIFF"),
    K("K"),
    COEF("COEF"),
    TEMP_JUMP("TEMP_JUMP"),
    NUMBER_END_PIXELS("NUMBER_END_PIXELS"),
    DEC("DEC"),
    EPS("EPS"),
    MAX_ITER("MAX_ITER");

    /**
     * Имя параметра.
     */
    private final String name;
    /**
     * Значение параметра.
     */
    private String value;

    Property(String name) {
        this.name = name;
    }

    static {
        Scanner sc = null;
        try {
            sc = new Scanner(new File(Helper.filename(Main.DIR_CURRENT, Main.CONFIG)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String nextLine;
        while (sc.hasNextLine()) {
            nextLine = sc.nextLine();
            for (Property p : Property.values())
                if (nextLine.matches(p.name + " *=.*")) {
                    p.value = nextLine.substring(nextLine.indexOf('=') + 2);
                    break;
                }
        }
    }

    public String value() {
        return value;
    }

    int intValue() {
        return Integer.parseInt(value);
    }

    double doubleValue() {
        return Double.parseDouble(value);
    }

    Double[] doubleArrayValue() {
        var res = new ArrayList<Double>();
        for (String s : value.split(", "))
            res.add(Double.parseDouble(s));
        return res.toArray(new Double[0]);
    }
}
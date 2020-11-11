package main;

import java.io.File;
import java.io.FileNotFoundException;
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
    MIN_PIXEL_SQUARE("MIN_PIXEL_SQUARE");

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
            sc = new Scanner(new File(Main.DIR_CURRENT + "/" + Main.SHORT_FILENAME_CONFIG));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String nextLine;
        while (sc.hasNextLine()) {
            nextLine = sc.nextLine();
            for (Property p : Property.values())
                if (nextLine.matches(p.name + ".*")) {
                    p.value = nextLine.substring(nextLine.indexOf('=') + 2);
                    break;
                }
        }
    }

    String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    int getIntValue() {
        return Integer.parseInt(value);
    }

    double getDoubleValue() {
        return Double.parseDouble(value);
    }
}
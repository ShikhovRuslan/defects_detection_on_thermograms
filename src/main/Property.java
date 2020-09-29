package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;


/**
 * Содержит параметры из конфигурационного файла {@code SHORT_FILENAME_CONFIG}.
 */
public enum Property {
    DIR_THERMOGRAMS("THERMOGRAMS_DIR"),
    SUBDIR_OUTPUT("OUTPUT_SUBDIR"),
    SUBDIR_RAW("RAW_SUBDIR"),
    SUBDIR_OUTPUT_PICTURES("OUTPUT_PICTURES_SUBDIR"),
    SUBDIR_OUTPUT_REAL("OUTPUT_REAL_SUBDIR"),
    POSTFIX_RAW("POSTFIX_RAW"),
    POSTFIX_PROCESSED("POSTFIX_PROCESSED"),
    POSTFIX_REAL("POSTFIX_REAL"),
    PIXEL_SIZE("PIXEL_SIZE"),
    PRINCIPAL_POINT_X("PRINCIPAL_POINT_X"),
    PRINCIPAL_POINT_Y("PRINCIPAL_POINT_Y"),
    T_MIN("T_MIN"),
    SQUARE_MIN("SQUARE_MIN");

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
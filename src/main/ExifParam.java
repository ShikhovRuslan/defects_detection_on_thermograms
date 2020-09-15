package main;


/**
 * Содержит общие для всех термограмм EXIF-параметры, находящиеся в файле {@code SHORT_FILENAME_GLOBAL_PARAMS}.
 */
public enum ExifParam {
    FOCAL_LENGTH("FocalLength"),
    PLANCK_R1("PlanckR1"),
    PLANCK_R2("PlanckR2"),
    PLANCK_O("PlanckO"),
    PLANCK_B("PlanckB"),
    PLANCK_F("PlanckF"),
    EMISSIVITY("Emissivity"),
    REFLECTED_APPARENT_TEMPERATURE("ReflectedApparentTemperature"),
    RAW_THERMAL_IMAGE_HEIGHT("RawThermalImageHeight"),
    RAW_THERMAL_IMAGE_WIDTH("RawThermalImageWidth");

    /**
     * Имя параметра.
     */
    private final String rawName;
    /**
     * Значение параметра.
     */
    private final double value;

    ExifParam(String rawName) {
        this.rawName = rawName;
        this.value = readValue(
                Main.DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT.getValue() + "/" +
                        Main.SHORT_FILENAME_GLOBAL_PARAMS);
    }

    public String getRawName() {
        return rawName;
    }

    public double getValue() {
        return value;
    }

    /**
     * Извлекает значение текущего параметра из файла {@code filename} в формате JSON.
     */
    private double readValue(String filename) {
        return Helper.getJsonObject(filename).get(rawName).getAsDouble();
    }

    /**
     * Извлекает массив значений всех параметров.
     */
    static double[] readValues() {
        double[] exifParams = new double[ExifParam.values().length];
        for (int i = 0; i < ExifParam.values().length; i++)
            exifParams[i] = ExifParam.values()[i].value;
        return exifParams;
    }
}
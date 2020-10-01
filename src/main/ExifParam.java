package main;


/**
 * Содержит общие для всех термограмм EXIF-параметры, находящиеся в файле {@code SHORT_FILENAME_GLOBAL_PARAMS}.
 * Фокальное расстояние {@code FOCAL_LENGTH} переводится из миллиметров в метры.
 * Отражённая температура {@code REFLECTED_APPARENT_TEMPERATURE} переводится из градусов Цельсия в Кельвины.
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
    RES_X("RawThermalImageWidth"),
    RES_Y("RawThermalImageHeight");

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

        switch (rawName) {
            case "FocalLength":
                this.value = readValue(Main.DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT.getValue() + "/" +
                        Main.SHORT_FILENAME_GLOBAL_PARAMS) / 1000;
                break;
            case "ReflectedApparentTemperature":
                this.value = readValue(Main.DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT.getValue() + "/" +
                        Main.SHORT_FILENAME_GLOBAL_PARAMS) + 273.15;
                break;
            default:
                this.value = readValue(Main.DIR_CURRENT + "/" + Property.SUBDIR_OUTPUT.getValue() + "/" +
                        Main.SHORT_FILENAME_GLOBAL_PARAMS);
        }
    }

    public String getRawName() {
        return rawName;
    }

    public double getValue() {
        return value;
    }

    public int getIntValue() {
        return (int) value;
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
package main;

import static java.lang.Math.*;


/**
 * Углы термограммы в системе координат c'x'y', начиная с верхнего левого угла и заканчивая нижним левым.
 */
public enum Corners {
    /**
     * Верхний левый угол термограммы.
     */
    C0(0, ExifParam.RES_Y.getIntValue() - 1),
    /**
     * Верхний правый угол термограммы.
     */
    C1(ExifParam.RES_X.getIntValue() - 1, ExifParam.RES_Y.getIntValue() - 1),
    /**
     * Нижний правый угол термограммы.
     */
    C2(ExifParam.RES_X.getIntValue() - 1, 0),
    /**
     * Нижний левый угол термограммы.
     */
    C3(0, 0);

    /**
     * Абсцисса угла термограммы.
     */
    private final int i;
    /**
     * Ордината угла термограммы.
     */
    private final int j;

    Corners(int i, int j) {
        this.i = i;
        this.j = j;
    }

    /**
     * Конвертирует текущий угол термограммы в точку.
     */
    Pixel toPixel() {
        return new Pixel(i, j);
    }

    /**
     * Вычисляет острый угол (в градусах) между отрезком, соединяющим точку {@code point} и текущий угол
     * термограммы, и прямой, проходящей через точку {@code point} и параллельной оси c'x'.
     */
    double angle(Pixel point) {
        return (180 / PI) * atan(abs(j - point.getJ()) / abs(i - point.getI() + 0.));
    }
}
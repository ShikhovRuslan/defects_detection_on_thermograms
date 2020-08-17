package main;

public class Thermogram {
    private final double yaw;
    private final double height;
    private final static Pixel PRINCIPAL_POINT = new Pixel(NewClass.PRINCIPAL_POINT_X,NewClass.PRINCIPAL_POINT_Y);

    Thermogram(double yaw, double height) {
        this.yaw = yaw;
        this.height = height;
    }
}
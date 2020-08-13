package main;

import com.grum.geocalc.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

import static java.lang.Math.abs;

/*
https://github.com/grumlimited/geocalc
 */

class LocalCoords {
    private final int i;
    private final int j;

    LocalCoords(int i, int j) {
        this.i = i;
        this.j = j;
    }

    public int getI() {
        return i;
    }

    public int getJ() {
        return j;
    }
}

public class NewClass {
    private final static int resX = 640;
    private final static int resY = 512;
    private final static double pixelSize = 17./1000_000; // метры
    private final static double focalLength = 25./1000; // метры
    private final static double EARTH_RADIUS = 6371.01 * 1000; // метры

    private static double imageLength(){
        return resX*pixelSize;
    }

    private static double imageWidth(){
        return resY*pixelSize;
    }

    private static double areaLength(double height){
        return height*imageLength()/focalLength;
    }

    private static double areaWidth(double height){
        return height*imageWidth()/focalLength;
    }

    private static double toAreaLength(double height, int pixel){
        return areaLength(height)*pixel/resX;
    }

    private static double toAngle(double length) {
        return (length/EARTH_RADIUS)*(180/Math.PI);
    }

    private static double anglePerPixel(double height, double angle) {
        return toAngle(areaLength(height)/resX);
    }

    private static double anglePerPixel2(double height, double angle) {
        return toAngle(areaWidth(height)/resY);
    }

    private static double length(LocalCoords a, LocalCoords b, double height){
        return (height*pixelSize/focalLength)*Math.sqrt(Math.pow(a.getI()-b.getI(),2)+Math.pow(a.getJ()-b.getJ(),2));
    }

    static String toDMSCoordinate(double degrees) {
        double _wholeDegrees = (int) degrees;
        double remaining = abs(degrees - _wholeDegrees);
        double _minutes = (int) (remaining * 60);
        remaining = remaining * 60 - _minutes;
        double _seconds = new BigDecimal(remaining * 60).setScale(4, RoundingMode.HALF_UP).doubleValue();
        return _wholeDegrees + " "+ _minutes + " " + _seconds;
    }

    private static Point[] getCorners(LocalCoords o, double yaw, double height, Point coords) {
        Point[] res = new Point[4];
        res[0] = EarthCalc.pointAt(coords, -(90+Math.atan(o.getI()/(resY-o.getJ()+0.))+yaw), length(o, new LocalCoords(0, resY), height));
        res[1] = EarthCalc.pointAt(coords, -(Math.atan((resY-o.getJ())/(resX-o.getI()+0.))+yaw), length(o, new LocalCoords(resX, resY), height));
        res[2] = EarthCalc.pointAt(coords, -(-Math.atan(o.getJ()/(resX-o.getI()+0.))+yaw), length(o, new LocalCoords(resX, 0), height));
        res[3] = EarthCalc.pointAt(coords, -(-90-Math.atan(o.getI()/(o.getJ()+0.))+yaw), length(o, new LocalCoords(0, 0), height));
        return res;
    }

    public static void main(String[] args) {
        Point[] points = getCorners(new LocalCoords(484, 490), 39.7, 152.2, Point.at(Coordinate.fromGPS(53, 46 + 45.70 / 60), Coordinate.fromGPS(87, 15 + 44.59 / 60)));
        for(int i = 0; i<4; i++)
            try {
                System.out.println("lat=" + toDMSCoordinate(points[i].latitude) + "  lon=" + toDMSCoordinate(points[i].longitude));
            } catch (NumberFormatException e) {
                System.out.println("NumberFormatException");
            }

        System.out.println(Arrays.toString(points));
    }
}
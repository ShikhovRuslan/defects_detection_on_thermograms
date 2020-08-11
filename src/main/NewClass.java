package main;

import com.grum.geocalc.Coordinate;
import com.grum.geocalc.Point;

public class NewClass {
    private final static int resX = 640;
    private final static int resY = 512;
    private final static double pixelSize = 17*Math.pow(10,-6);
    private final static double focalLength = 25*Math.pow(10,-3);

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

    public static void main(String[] args) {
        //Kew, London
        Coordinate lat = Coordinate.fromDegrees(51.4843774);
        Coordinate lng = Coordinate.fromDegrees(-0.2912044);
        Point kew = Point.at(lat, lng);

        System.out.println(kew);
    }
}
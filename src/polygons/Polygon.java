package polygons;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Polygon {
    private List<Point> vertices;

    public Polygon(List<Point> vertices) {
        this.vertices = vertices;
    }

    public Line[] getSides() {
        Line[] sides = new Line[vertices.size()];
        for (int i = 0; i < vertices.size(); i++)
            sides[i] = new Line(vertices.get(i), vertices.get(i < vertices.size() - 1 ? i + 1 : 0));
        return sides;
    }

    public boolean isCloseTo(Polygon polygon, int distance) {
        for (Point vertex : vertices)
            for (Line side : polygon.getSides())
                if (vertex.projectableTo(side) && vertex.distance(side) <= distance)
                    return true;
        return false;
    }

    public static int findMin(List<Integer> list) {
        int index = 0;
        int min = list.get(index);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) < min) {
                index = i;
                min = list.get(i);
            }
        }
        return index;
    }

    public Line[] linkingLine(Polygon polygon, int distance) {
        Point vertex0 = null;
        Line side1 = null;
        List<Integer> lInt = new ArrayList<>();
        List<Point> lP = new ArrayList<>();
        List<Line> lL = new ArrayList<>();
        for (Point vertex : vertices)
            for (Line side : polygon.getSides())
                if (vertex.projectableTo(side) && vertex.distance(side) <= distance) {
                    lInt.add(vertex.distance(side));
                    lP.add(vertex);
                    lL.add(side);
                }
        vertex0 = lP.get(findMin(lInt));
        side1 = lL.get(findMin(lInt));
        return new Line[]{new Line(vertex0, vertex0.project(side1)), side1};
    }

    public int getSideToShorten(Point vertex0, boolean isLinkingLineHorizontal) {
        Line side0 = null;
        for (int i = 0; i < getSides().length; i++)
            if (vertex0 == getSides()[i].getA() || vertex0 == getSides()[i].getB())
                if (isLinkingLineHorizontal && getSides()[i].isVertical() ||
                        !isLinkingLineHorizontal && getSides()[i].isHorizontal())
                    return i;
        return 0;
    }

    public int getSideWithPoint(Point point) {
        Line[] sides = getSides();
        for (int i = 0; i < sides.length; i++)
            if (sides[i].contains(point))
                return i;
        return -1;
    }

    public static Line[] deleteWithShift(Line[] array, int index) {
        Line[] result = new Line[array.length - 1];
        System.arraycopy(array, 0, result, 0, index);
        System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        return result;
    }

    public static Line[][] getPolygonalChains(Polygon gonA, Polygon gonB,
                                              int side0Index, Point vertex0, Line linkingLine, Point vertex1, int side1Index) {
        Line[] sidesA = gonA.getSides();
        Line[] sidesB = gonB.getSides();
        Line sideAToShorten = sidesA[side0Index];
        Line sideBToShorten = sidesB[side1Index];
        Line newSideA = null;
        Line newSideB = null;
        Line newSideB2;
        Point pA = sideAToShorten.getOtherEnd(vertex0);
        Point otherBorderA, otherBorderB;
        if (linkingLine.isHorizontal()) {
            if (pA.getX() < vertex0.getX()) {
                if (pA.getX() < sideBToShorten.upperEnd().getX()) {
                    newSideA = new Line(pA, new Point(sideBToShorten.upperEnd().getX(), vertex0.getY()));
                    otherBorderB = sideBToShorten.upperEnd();
                    otherBorderA = otherBorderB.project(sideAToShorten);
                } else {
                    newSideB = new Line(sideBToShorten.upperEnd(), new Point(pA.getX(), vertex1.getY()));
                    otherBorderA = pA;
                    otherBorderB = otherBorderA.project(sideBToShorten);
                }
                newSideB2 = new Line(vertex1, sideBToShorten.lowerEnd());
            } else {
                if (pA.getX() > sideBToShorten.lowerEnd().getX()) {
                    newSideA = new Line(pA, new Point(sideBToShorten.lowerEnd().getX(), vertex0.getY()));
                    otherBorderB = sideBToShorten.lowerEnd();
                    otherBorderA = otherBorderB.project(sideAToShorten);
                } else {
                    newSideB = new Line(sideBToShorten.lowerEnd(), new Point(pA.getX(), vertex1.getY()));
                    otherBorderA = pA;
                    otherBorderB = otherBorderA.project(sideBToShorten);
                }
                newSideB2 = new Line(vertex1, sideBToShorten.upperEnd());
            }
        } else {
            if (pA.getY() < vertex0.getY()) {
                if (pA.getY() < sideBToShorten.leftEnd().getY()) {
                    newSideA = new Line(pA, new Point(vertex0.getX(), sideBToShorten.leftEnd().getY()));
                    otherBorderB = sideBToShorten.leftEnd();
                    otherBorderA = otherBorderB.project(sideAToShorten);
                } else {
                    newSideB = new Line(sideBToShorten.leftEnd(), new Point(vertex1.getX(), pA.getY()));
                    otherBorderA = pA;
                    otherBorderB = otherBorderA.project(sideBToShorten);
                }
                newSideB2 = new Line(vertex1, sideBToShorten.rightEnd());
            } else {
                if (pA.getY() > sideBToShorten.rightEnd().getY()) {
                    newSideA = new Line(pA, new Point(vertex0.getX(), sideBToShorten.rightEnd().getY()));
                    otherBorderB = sideBToShorten.rightEnd();
                    otherBorderA = otherBorderB.project(sideAToShorten);
                } else {
                    newSideB = new Line(sideBToShorten.rightEnd(), new Point(vertex1.getX(), pA.getY()));
                    otherBorderA = pA;
                    otherBorderB = otherBorderA.project(sideBToShorten);
                }
                newSideB2 = new Line(vertex1, sideBToShorten.leftEnd());
            }
        }
        if (newSideA != null)
            sidesA[side0Index] = newSideA;
        else
            sidesA = deleteWithShift(sidesA, side0Index);
        Line[] sidesBNew = new Line[sidesB.length + 1];
        if (newSideB != null) {
            System.arraycopy(sidesB, 0, sidesBNew, 0, sidesB.length);
            sidesBNew[side1Index] = newSideB2;
            sidesBNew[sidesB.length] = newSideB;
        } else {
            sidesB[side1Index] = newSideB2;
        }
        return new Line[][]{sidesA, newSideB != null ? sidesBNew : sidesB, new Line[]{new Line(otherBorderA, otherBorderB)}};
    }

    public static boolean isIn(List<Integer> list, int num) {
        for (Integer val : list)
            if (val == num) return true;
        return false;
    }

    public static Line[] order (Line[] sides) throws NullPointerException{
        Line[] orderedSides = new Line[sides.length];
        List<Integer> processed = new ArrayList<>();
        orderedSides[0] = sides[0];

            for (int i = 1; i < sides.length; i++) { //5 4 проблема
                if (!isIn(processed, i)) {
                    for (int k = 1; k < sides.length; k++) {
                        if (!isIn(processed, k)) {
                            if (orderedSides[i - 1].getB().equals(sides[k].getA())) {
                                orderedSides[i] = sides[k];
                                processed.add(k);
                                break;
                            }
                            if (orderedSides[i - 1].getB().equals(sides[k].getB())) {
                                orderedSides[i] = new Line(sides[k].getB(), sides[k].getA());
                                processed.add(k);
                                break;
                            }
                        }
                    }
                }
            }

        return orderedSides;
    }

    public static Polygon unitePolygonalChains(Line[] chain1, Line[] chain2, Line linkingLine, Line otherBorder)
    throws NullPointerException{
        Line[] newSides = new Line[chain1.length + chain2.length + 2];
        System.arraycopy(chain1, 0, newSides, 0, chain1.length);
        System.arraycopy(chain2, 0, newSides, chain1.length, chain2.length);
        newSides[chain1.length + chain2.length] = linkingLine;
        newSides[chain1.length + chain2.length + 1] = otherBorder;
        newSides = order(newSides);
        List<Point> newPoints = new ArrayList<>();
        newPoints.add(newSides[0].getA());
        for (Line side : newSides) {
            if (!newPoints.contains(side.getA())) {
                newPoints.add(side.getA());
            }
            if (!newPoints.contains(side.getB()))
                newPoints.add(side.getB());
        }
        return new Polygon(newPoints);
    }

    public static Polygon unitePolygons(Polygon gonA, Polygon gonB) throws NullPointerException{
        Line[] lines = gonA.linkingLine(gonB, 3);
        Line linkingLine = lines[0];
        Line side1 = lines[1];
        Point vertex0 = linkingLine.getA();
        Point vertex1 = linkingLine.getB();
        int side0Index = gonA.getSideToShorten(vertex0, linkingLine.isHorizontal());
        int side1Index = gonB.getSideWithPoint(vertex1);
        Line[][] polygonalChains = getPolygonalChains(gonA, gonB, side0Index, vertex0, linkingLine, vertex1, side1Index);
        Line otherBoard = polygonalChains[2][0];
        return unitePolygonalChains(polygonalChains[0], polygonalChains[1], linkingLine, otherBoard);
    }

    public void draw(BufferedImage image, Color color) {
        for (Line side : getSides())
            side.draw(image, color);
    }

    public static void drawPolygons(List<Polygon> polygons, Color color, String pictureName, String newPictureName) {
        try {
            BufferedImage image = ImageIO.read(new File(pictureName));
            for (Polygon polygon : polygons)
                polygon.draw(image, color);
            ImageIO.write(image, "jpg", new File(newPictureName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Polygon> toPoligons(List<Polygon> polygons) {
        List<Polygon> newPolygons = new ArrayList<>();
        List<Integer> processedPolygons = new ArrayList<>();
        int ii = -1;
        int jj = -1;
        try {

            for (int i = 0; i < polygons.size(); i++) {
                ii = i;
                if(ii == 3){
                    System.out.println();
                }
                if (!isIn(processedPolygons, i)) {
                    for (int j = i + 1; j < polygons.size(); j++) {
                        jj = j;
                        if (polygons.get(i).isCloseTo(polygons.get(j), 3) && !isIn(processedPolygons, j)) {
                            newPolygons.add(unitePolygons(polygons.get(i), polygons.get(j)));
                            processedPolygons.add(j);
                            break;
                        }
                    }
                }
            }
        } catch (NullPointerException e) {
            System.out.println(ii + "   " + jj);
        }
        return newPolygons;
    }
}
package polygons;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


public class Polygon {
    private List<Point> vertices;

    public Polygon(List<Point> vertices) {
        this.vertices = vertices;
    }

    private Line[] getSides() {
        Line[] sides = new Line[vertices.size()];
        for (int i = 0; i < vertices.size(); i++)
            sides[i] = new Line(vertices.get(i), vertices.get(i < vertices.size() - 1 ? i + 1 : 0));
        return sides;
    }

    private boolean[] isCloseTo(Polygon polygon, int distance) {
        for (Point vertex : vertices)
            for (Line side : polygon.getSides())
                if (vertex.projectableTo(side) && vertex.distance(side) <= distance)
                    return new boolean[]{true, false};
        for (Point vertex : polygon.vertices)
            for (Line side : getSides())
                if (vertex.projectableTo(side) && vertex.distance(side) <= distance)
                    return new boolean[]{true, true};
        return new boolean[]{false, false};
    }

    public static void removeRedundantVertices(List<Polygon> polygons) {
        for (Polygon polygon : polygons)
            polygon.removeRedundantVertices();
    }

    private void removeRedundantVertices() {
        List<Point> newVertices = new ArrayList<>();
        int index;
        boolean vertexIsAdded = false;
        for (Point vertex : vertices) {
            if (!vertexIsAdded) {
                index = vertices.indexOf(vertex);
                if (getSides()[index > 0 ? index - 1 : getSides().length - 1].getA().getX() == getSides()[index].getB().getX() ||
                        getSides()[index > 0 ? index - 1 : getSides().length - 1].getA().getY() == getSides()[index].getB().getY()) {
                    if (index < vertices.size() - 1)
                        newVertices.add(getSides()[index].getB());
                    vertexIsAdded = true;
                    continue;
                } else
                    newVertices.add(vertex);
            }
            vertexIsAdded = false;
        }
        vertices = newVertices;
    }

    private static int findIndexOfMin(List<Integer> list) {
        int index = 0;
        int min = list.get(index);
        for (int i = 1; i < list.size(); i++)
            if (list.get(i) < min) {
                index = i;
                min = list.get(index);
            }
        return index;
    }

    private Line[] Perpendicular(Polygon polygon, int distance) {
        List<Integer> tmpDistances = new ArrayList<>();
        List<Point> tmpVertices = new ArrayList<>();
        List<Line> tmpSides = new ArrayList<>();
        for (Point vertex : vertices)
            for (Line side : polygon.getSides())
                if (vertex.projectableTo(side) && vertex.distance(side) <= distance) {
                    tmpDistances.add(vertex.distance(side));
                    tmpVertices.add(vertex);
                    tmpSides.add(side);
                }
        int index = findIndexOfMin(tmpDistances);
        Point vertex0 = tmpVertices.get(index);
        Line side1 = tmpSides.get(index);
        return new Line[]{new Line(vertex0, vertex0.project(side1)), side1};
    }

    private int indexOfSideToShorten(Point vertex0, boolean isPerpendicularHorizontal) {
        Line[] sides = getSides();
        for (int i = 0; i < sides.length; i++)
            if ((vertex0 == sides[i].getA() || vertex0 == sides[i].getB()) &&
                    (isPerpendicularHorizontal && sides[i].isVertical() ||
                            !isPerpendicularHorizontal && sides[i].isHorizontal()))
                return i;
        return -1;
    }

    // Вершины игнорируются.
    private int indexOfSideWithPoint(Point point) {
        Line[] sides = getSides();
        for (int i = 0; i < sides.length; i++)
            if (sides[i].contains(point))
                return i;
        return -1;
    }

    private static Line[] deleteWithShift(Line[] array, int index) {
        Line[] result = new Line[array.length - 1];
        System.arraycopy(array, 0, result, 0, index);
        System.arraycopy(array, index + 1, result, index, array.length - index - 1);
        return result;
    }

    private static Line[][] getPolygonalChains(Polygon polygon1, Polygon polygon2, int side0Index, Point vertex0,
                                               Line perpendicular, Point vertex1, int side1Index) {
        Line[] sides1 = polygon1.getSides();
        Line[] sides2 = polygon2.getSides();
        Line side1ToShorten = sides1[side0Index];
        Line side2ToShorten = sides2[side1Index];
        Line newSide1 = null;
        Line newSide2 = null;
        Line newSide22;
        Point pA = side1ToShorten.getOtherEnd(vertex0);
        Point otherBorder1, otherBorder2;
        if (perpendicular.isHorizontal()) {
            if (pA.getX() < vertex0.getX()) {
                if (pA.getX() < side2ToShorten.upperEnd().getX()) {
                    newSide1 = new Line(pA, new Point(side2ToShorten.upperEnd().getX(), vertex0.getY()));
                    otherBorder2 = side2ToShorten.upperEnd();
                    otherBorder1 = otherBorder2.project(side1ToShorten);
                } else {
                    newSide2 = new Line(side2ToShorten.upperEnd(), new Point(pA.getX(), vertex1.getY()));
                    otherBorder1 = pA;
                    otherBorder2 = otherBorder1.project(side2ToShorten);
                }
                newSide22 = new Line(vertex1, side2ToShorten.lowerEnd());
            } else {
                if (pA.getX() > side2ToShorten.lowerEnd().getX()) {
                    newSide1 = new Line(pA, new Point(side2ToShorten.lowerEnd().getX(), vertex0.getY()));
                    otherBorder2 = side2ToShorten.lowerEnd();
                    otherBorder1 = otherBorder2.project(side1ToShorten);
                } else {
                    newSide2 = new Line(side2ToShorten.lowerEnd(), new Point(pA.getX(), vertex1.getY()));
                    otherBorder1 = pA;
                    otherBorder2 = otherBorder1.project(side2ToShorten);
                }
                newSide22 = new Line(vertex1, side2ToShorten.upperEnd());
            }
        } else {
            if (pA.getY() < vertex0.getY()) {
                if (pA.getY() < side2ToShorten.leftEnd().getY()) {
                    newSide1 = new Line(pA, new Point(vertex0.getX(), side2ToShorten.leftEnd().getY()));
                    otherBorder2 = side2ToShorten.leftEnd();
                    otherBorder1 = otherBorder2.project(side1ToShorten);
                } else {
                    newSide2 = new Line(side2ToShorten.leftEnd(), new Point(vertex1.getX(), pA.getY()));
                    otherBorder1 = pA;
                    otherBorder2 = otherBorder1.project(side2ToShorten);
                }
                newSide22 = new Line(vertex1, side2ToShorten.rightEnd());
            } else {
                if (pA.getY() > side2ToShorten.rightEnd().getY()) {
                    newSide1 = new Line(pA, new Point(vertex0.getX(), side2ToShorten.rightEnd().getY()));
                    otherBorder2 = side2ToShorten.rightEnd();
                    otherBorder1 = otherBorder2.project(side1ToShorten);
                } else {
                    newSide2 = new Line(side2ToShorten.rightEnd(), new Point(vertex1.getX(), pA.getY()));
                    otherBorder1 = pA;
                    otherBorder2 = otherBorder1.project(side2ToShorten);
                }
                newSide22 = new Line(vertex1, side2ToShorten.leftEnd());
            }
        }
        if (newSide1 != null)
            sides1[side0Index] = newSide1;
        else
            sides1 = deleteWithShift(sides1, side0Index);
        Line[] sidesBNew = new Line[sides2.length + 1];
        sides2[side1Index] = newSide22;
        if (newSide2 != null) {
            if (!isPointNotLine(newSide2)) {
                System.arraycopy(sides2, 0, sidesBNew, 0, sides2.length);
                sidesBNew[sides2.length] = newSide2;
                return new Line[][]{sides1, sidesBNew, new Line[]{new Line(otherBorder1, otherBorder2)}};
            }
        }
        return new Line[][]{sides1, sides2, new Line[]{new Line(otherBorder1, otherBorder2)}};
    }

    private static boolean isPointNotLine(Line line) {
        return line.getA().equals(line.getB());
    }

    private static boolean isIn(List<Integer> list, int val0) {
        for (Integer val : list)
            if (val == val0) return true;
        return false;
    }

    private static Line[] order(Line[] sides) throws NullPointerException {
        Line[] newSides = new Line[sides.length];
        List<Integer> processed = new ArrayList<>();
        newSides[0] = sides[0];
        for (int i = 1; i < sides.length; i++)
            for (int k = 1; k < sides.length; k++)
                if (!isIn(processed, k)) {
                    if (newSides[i - 1].getB().equals(sides[k].getA())) {
                        newSides[i] = sides[k];
                        processed.add(k);
                        break;
                    }
                    if (newSides[i - 1].getB().equals(sides[k].getB())) {
                        newSides[i] = new Line(sides[k].getB(), sides[k].getA());
                        processed.add(k);
                        break;
                    }
                }
        return newSides;
    }

    private static Polygon unitePolygonalChains(Line[] chain1, Line[] chain2, Line linkingLine, Line otherBorder)
            throws NullPointerException {
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

    static Polygon unitePolygons(Polygon gonA, Polygon gonB, int distance) throws NullPointerException {
        Line[] lines = gonA.Perpendicular(gonB, distance);
        Line linkingLine = lines[0];
        Point vertex0 = linkingLine.getA();
        Point vertex1 = linkingLine.getB();
        int side0Index = gonA.indexOfSideToShorten(vertex0, linkingLine.isHorizontal());
        int side1Index = gonB.indexOfSideWithPoint(vertex1);
        Line[][] polygonalChains = getPolygonalChains(gonA, gonB, side0Index, vertex0, linkingLine, vertex1, side1Index);
        Line otherBoard = polygonalChains[2][0];
        return unitePolygonalChains(polygonalChains[0], polygonalChains[1], linkingLine, otherBoard);
    }

    private void draw(BufferedImage image, Color color) {
        Line[] sides = getSides();
        for (Line side : sides)
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

    public static List<Polygon> toPolygons(List<Polygon> polygons, int distance) {
        List<Polygon> newPolygons = new ArrayList<>();
        List<Integer> processedPolygons = new ArrayList<>();
        try {
            for (int i = 0; i < polygons.size(); i++) {
                if (!isIn(processedPolygons, i)) {
                    int j;
                    for (j = i + 1; j < polygons.size(); j++) {
                        if (polygons.get(i).isCloseTo(polygons.get(j), distance)[0] && !isIn(processedPolygons, j)) {
                            if (!polygons.get(i).isCloseTo(polygons.get(j), distance)[1])
                                newPolygons.add(unitePolygons(polygons.get(i), polygons.get(j), distance));
                            else
                                newPolygons.add(unitePolygons(polygons.get(j), polygons.get(i), distance));
                            processedPolygons.add(j);
                            break;
                        }
                    }
                    if (j == polygons.size() && !isIn(processedPolygons, j)) {
                        newPolygons.add(polygons.get(i));
                    }
                }
            }
        } catch (NullPointerException e) {
            System.out.println("NullPointerException in Polygon.toPolygons().");
            e.printStackTrace();
        }
        removeLoops(newPolygons);
        return newPolygons;
    }

    private static void removeLoops(List<Polygon> polygons) {
        for (Polygon polygon : polygons)
            polygon.removeLoops();
    }

    private void removeLoops() {
        Iterator iter = vertices.iterator();
        Point curr;
        Point prev = vertices.get(vertices.size() - 1);
        while (iter.hasNext()) {
            curr = (Point) iter.next();
            if (curr.equals(prev))
                iter.remove();
            prev = curr;
        }
    }

    @Override
    public String toString() {
        return Arrays.toString(vertices.toArray());
    }
}
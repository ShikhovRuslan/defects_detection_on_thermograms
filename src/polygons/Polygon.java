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

    public static List<Polygon> removeRedundantVertices(List<Polygon> polygons) {
        List<Polygon> newPolygons = new ArrayList<>();
        for (Polygon polygon : polygons) newPolygons.add(polygon.removeRedundantVertices());
        return newPolygons;
    }

    private Polygon removeRedundantVertices() {
        List<Point> newPoints = new ArrayList<>();
        int index;
        boolean vertexIsAdded = false;
        for (Point vertex : vertices) {
            if (!vertexIsAdded) {
                index = vertices.indexOf(vertex);
                if (getSides()[index == 0 ? getSides().length - 1 : index - 1].getA().getX() == getSides()[index].getB().getX() ||
                        getSides()[index == 0 ? getSides().length - 1 : index - 1].getA().getY() == getSides()[index].getB().getY()) {
                    if (index < vertices.size() - 1)
                        newPoints.add(getSides()[index].getB());
                    vertexIsAdded = true;
                    continue;
                } else {
                    newPoints.add(vertex);
                }
            }
            vertexIsAdded = false;
        }
        return new Polygon(newPoints);
    }

    private static int findMin(List<Integer> list) {
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

    private Line[] linkingLine(Polygon polygon, int distance) {
        Point vertex0;
        Line side1;
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

    private int getSideToShorten(Point vertex0, boolean isLinkingLineHorizontal) {
        for (int i = 0; i < getSides().length; i++)
            if (vertex0 == getSides()[i].getA() || vertex0 == getSides()[i].getB())
                if (isLinkingLineHorizontal && getSides()[i].isVertical() ||
                        !isLinkingLineHorizontal && getSides()[i].isHorizontal())
                    return i;
        return 0;
    }

    private int getSideWithPoint(Point point) {
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

    private static Line[][] getPolygonalChains(Polygon gonA, Polygon gonB,
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
        sidesB[side1Index] = newSideB2;
        if (newSideB != null) {
            if (!isPointNotLine(newSideB)) {
                System.arraycopy(sidesB, 0, sidesBNew, 0, sidesB.length);
                sidesBNew[sidesB.length] = newSideB;
                return new Line[][]{sidesA, sidesBNew, new Line[]{new Line(otherBorderA, otherBorderB)}};
            }
        }
        return new Line[][]{sidesA, sidesB, new Line[]{new Line(otherBorderA, otherBorderB)}};
    }

    private static boolean isPointNotLine(Line line) {
        return line.getA().equals(line.getB());
    }

    private static boolean isIn(List<Integer> list, int num) {
        for (Integer val : list)
            if (val == num) return true;
        return false;
    }

    private static Line[] order(Line[] sides) throws NullPointerException {
        Line[] orderedSides = new Line[sides.length];
        List<Integer> processed = new ArrayList<>();
        orderedSides[0] = sides[0];

        for (int i = 1; i < sides.length; i++) {
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

        return orderedSides;
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
        Line[] lines = gonA.linkingLine(gonB, distance);
        Line linkingLine = lines[0];
        Point vertex0 = linkingLine.getA();
        Point vertex1 = linkingLine.getB();
        int side0Index = gonA.getSideToShorten(vertex0, linkingLine.isHorizontal());
        int side1Index = gonB.getSideWithPoint(vertex1);
        Line[][] polygonalChains = getPolygonalChains(gonA, gonB, side0Index, vertex0, linkingLine, vertex1, side1Index);
        Line otherBoard = polygonalChains[2][0];
        return unitePolygonalChains(polygonalChains[0], polygonalChains[1], linkingLine, otherBoard);
    }

    private void draw(BufferedImage image, Color color) {
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
package polygons;


public class Point {
    private int x;
    private int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    // Выдаёт осмысленный результат, только если projectableTo(line).
    public int distance(Line line) {
        if (projectableTo(line)) {
            if (line.isHorizontal())
                return Math.abs(x - line.getA().getX());
            if (line.isVertical())
                return Math.abs(y - line.getA().getY());
        }
        return 0;
    }

    public boolean projectableTo(Line line) {
        if (line.isHorizontal())
            return y > Math.min(line.getA().getY(), line.getB().getY()) && y < Math.max(line.getA().getY(), line.getB().getY());
        if (line.isVertical())
            return x > Math.min(line.getA().getX(), line.getB().getX()) && y < Math.max(line.getA().getX(), line.getB().getX());
        return false;
    }

    public Point project(Line line) {
        if (line.isHorizontal()) return new Point(line.getA().getX(), y);
        if (line.isVertical()) return new Point(x, line.getA().getY());
        return new Point(0, 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Point))
            return false;
        return x == ((Point) obj).getX() && y == ((Point) obj).getY();
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
package polygons;


public class Point {
    private int x;
    private int y;

    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    int getX() {
        return x;
    }

    int getY() {
        return y;
    }

    /**
     * Возвращает расстояние от текущей точки до линии {@param line}.
     *
     * @throws IllegalArgumentException если текущая точка не может быть спроектирована на внутренность линии или линия
     *                                  не является ни горизонтальной, ни вертикальной
     */
    int distance(Line line) {
        if (projectableTo(line)) {
            if (line.isHorizontal())
                return Math.abs(x - line.getA().getX());
            if (line.isVertical())
                return Math.abs(y - line.getA().getY());
        }
        throw new IllegalArgumentException("Текущая точка не может быть спроектирована на внутренность линии или " +
                "линия не является ни горизонтальной, ни вертикальной.");
    }

    /**
     * Определяет возможность проектирования текущей точки на внутренность линии {@param line}.
     *
     * @throws IllegalArgumentException если линия не является ни горизонтальной, ни вертикальной
     */
    boolean projectableTo(Line line) {
        if (line.isHorizontal())
            return y > Math.min(line.getA().getY(), line.getB().getY()) && y < Math.max(line.getA().getY(), line.getB().getY());
        if (line.isVertical())
            return x > Math.min(line.getA().getX(), line.getB().getX()) && x < Math.max(line.getA().getX(), line.getB().getX());
        throw new IllegalArgumentException("Линия не является ни горизонтальной, ни вертикальной.");
    }

    /**
     * Возвращает проекцию текущей точки на внутренность линии {@param line}.
     *
     * @throws IllegalArgumentException если линия не является ни горизонтальной, ни вертикальной
     */
    Point project(Line line) {
        if (line.isHorizontal()) return new Point(line.getA().getX(), y);
        if (line.isVertical()) return new Point(x, line.getA().getY());
        throw new IllegalArgumentException("Линия не является ни горизонтальной, ни вертикальной.");
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
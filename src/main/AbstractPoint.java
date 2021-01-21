package main;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Math.*;


/**
 * Содержит точку правой дискретной двумерной системы координат.
 */
public abstract class AbstractPoint {
    /**
     * Абсцисса точки.
     */
    private final int i;
    /**
     * Ордината точки.
     */
    private final int j;

    protected AbstractPoint(int i, int j) {
        this.i = i;
        this.j = j;
    }

    protected AbstractPoint(double iD, double jD) {
        this((int) Math.round(iD), (int) Math.round(jD));
    }

    public int getI() {
        return i;
    }

    public int getJ() {
        return j;
    }

    /**
     * Создаёт точку с координатами ({@code i}, {@code j}).
     */
    public abstract AbstractPoint create(int i, int j);

    /**
     * Создаёт точку с координатами, являющимися округлениями чисел {@code iD}, {@code jD}.
     */
    public AbstractPoint create(double iD, double jD) {
        return create((int) Math.round(iD), (int) Math.round(jD));
    }

    /**
     * Вычисляет минимальный и максимальный числа среди чисел, полученных в результате применения функции {@code f} к
     * точкам массива {@code points} с индексами из списка {@code indices}. Этот список должен содержать только
     * существующие индексы.
     */
    public static <T extends AbstractPoint> int[] findMinAndMax(T[] points, List<Integer> indices, Function<T, Integer> f) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i : indices) {
            if (f.apply(points[i]) < min)
                min = f.apply(points[i]);
            if (f.apply(points[i]) > max)
                max = f.apply(points[i]);
        }
        return new int[]{min, max};
    }

    /**
     * Вычисляет минимальный и максимальный числа среди чисел, полученных в результате применения функции {@code f} к
     * точкам массива {@code points}.
     */
    public static <T extends AbstractPoint> int[] findMinAndMax(T[] points, Function<T, Integer> f) {
        List<Integer> allIndices = IntStream.rangeClosed(0, points.length - 1)
                .boxed().collect(Collectors.toList());
        return findMinAndMax(points, allIndices, f);
    }

    /**
     * Возвращает ординату точки, которая имеет абсциссу {@code i} и лежит на прямой, проходящей через точки {@code a} и
     * {@code b}.
     *
     * @throws IllegalArgumentException если прямая, проходящая через указанные точки, вертикальна
     */
    public static <T extends AbstractPoint> double linearFunction(int i, T a, T b) {
        if (a.getI() != b.getI())
            return a.getJ() + (i - a.getI()) * (b.getJ() - a.getJ() + 0.) / (b.getI() - a.getI());
        else
            throw new IllegalArgumentException("Прямая, проходящая через точки " + a + " и " + b + ", вертикальна.");
    }

    /**
     * Определяет принадлежность текущей точки отрезку с концами {@code p1} и {@code p2}.
     */
    public <T extends AbstractPoint> boolean isInLine(T p1, T p2, double eps) {
        if (p1.getI() == p2.getI())
            return i >= p1.getI() - eps && i <= p1.getI() + eps && min(p1.getJ(), p2.getJ()) - eps <= j && j <= max(p1.getJ(), p2.getJ()) + eps;
        // Прямая, содержащая отрезок (в случае невертикального отрезка): y = a*x + b.
        double a = (p1.getJ() - p2.getJ()) / (p1.getI() - p2.getI() + 0.);
        double b = p1.getJ() - a * p1.getI();
        if (this.equals(new Pixel(211, 329)) &&
                (p1.equals(new Pixel(210, 331)) && p2.equals(new Pixel(221, 308)) ||
                        p2.equals(new Pixel(210, 331)) && p1.equals(new Pixel(221, 308))))
            System.out.println(a + "  " + b);
        return j >= a * i + b - eps && j <= a * i + b + eps && min(p1.getI(), p2.getI()) - eps <= i && i <= max(p1.getI(), p2.getI()) + eps;
    }

    /**
     * Упорядочивает точки из списка {@code points} против часовой стрелки.
     */
    public static <T extends AbstractPoint> List<T> order(List<T> points) {
        OptionalDouble newI0Optional = points
                .stream()
                .mapToDouble(AbstractPoint::getI)
                .average();
        OptionalDouble newJ0Optional = points
                .stream()
                .mapToDouble(AbstractPoint::getJ)
                .average();
        double newI0 = newI0Optional.orElse(0);
        double newJ0 = newJ0Optional.orElse(0);
        points.sort((p1, p2) -> (int) signum(atan2(p1.getJ() - newJ0, p1.getI() - newI0) -
                atan2(p2.getJ() - newJ0, p2.getI() - newI0)));
        return points;
    }

    /**
     * Возвращает расстояние между точками {@code p1} и {@code p2}.
     */
    public static <T extends AbstractPoint> double distance(T p1, T p2) {
        return sqrt(pow(p1.getI() - p2.getI(), 2) + pow(p1.getJ() - p2.getJ(), 2));
    }

    /**
     * Возвращает расстояние от текущей точки до прямой, задаваемой точками {@code p1} и {@code p2}.
     *
     * @throws IllegalArgumentException если среди текущей точки и аргументов присутствуют разные типы или
     *                                  прямая неопределена вследствие совпадения аргументов
     */
    public double distanceToLine(AbstractPoint p1, AbstractPoint p2) {
        if (!getClass().equals(p1.getClass()) || !p1.getClass().equals(p2.getClass()))
            throw new IllegalArgumentException("Среди текущей точки и аргументов присутствуют разные типы.");

        if (p1.equals(p2))
            throw new IllegalArgumentException("Прямая неопределена вследствие совпадения аргументов.");

        // Случай вертикальной прямой.
        if (p1.getI() == p2.getI())
            return abs(getI() - p1.getI());

        // y=a*x+b - уравнение прямой, проходящей через точки p1 и p2.
        double a = (p2.getJ() - p1.getJ()) / (p2.getI() - p1.getI() + 0.);
        double b = p1.getJ() - a * p1.getI();

        // Случай горизонтальной прямой.
        if (a == 0)
            return abs(getJ() - p1.getJ());

        // (x0,y0) - точка пересечения упомянутой выше прямой с прямой, ей перпендикулярной и проходящей через точку p.
        double x0 = (getI() + a * (getJ() - b)) / (a * a + 1);
        double y0 = (b + a * (getI() + a * getJ())) / (a * a + 1);

        return sqrt(pow(getI() - x0, 2) + pow(getJ() - y0, 2));
    }

    /**
     * Проверяет принадлежность текущей точки отрезку с концами {@code p1} и {@code p2} (если inclusive[0] равен
     * {@code true}) или внутренности этого отрезка (если inclusive[0] равен {@code false} или отсутствует).
     *
     * @throws IllegalArgumentException если среди текущей точки и аргументов присутствуют разные типы
     */
    public boolean inSegment(AbstractPoint p1, AbstractPoint p2, boolean... inclusive) {
        if (!getClass().equals(p1.getClass()) || !p1.getClass().equals(p2.getClass()))
            throw new IllegalArgumentException("Среди текущей точки и аргументов присутствуют разные типы.");

        boolean include = inclusive.length > 0 && inclusive[0];

        int[] ii = findMinAndMax(new AbstractPoint[]{p1, p2}, AbstractPoint::getI);
        int[] jj = findMinAndMax(new AbstractPoint[]{p1, p2}, AbstractPoint::getJ);

        // Случай вертикальной прямой.
        if (p1.getI() == p2.getI())
            return p1.getI() == getI() && (include ?
                    jj[0] <= getJ() && getJ() <= jj[1] :
                    jj[0] < getJ() && getJ() < jj[1]);

        // Случай невертикальной прямой.
        // y=a*x+b - уравнение прямой, проходящей через точки p1 и p2.
        double a = (p2.getJ() - p1.getJ()) / (p2.getI() - p1.getI() + 0.);
        double b = p1.getJ() - a * p1.getI();

        return a * getI() + b == getJ() && (include ?
                ii[0] <= getI() && getI() <= ii[1] :
                ii[0] < getI() && getI() < ii[1]);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass())
            return false;
        return i == ((AbstractPoint) obj).getI() && j == ((AbstractPoint) obj).getJ();
    }

    @Override
    public String toString() {
        return getClass().getName() + toShortString();
    }

    public String toShortString() {
        return "(" + i + ", " + j + ")";
    }
}
package main;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.signum;
import static java.lang.Math.atan2;


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
     * Определяет принадлежность текущей точки отрезку с концами {@code p1} и {@code p2}.
     */
    public <T extends AbstractPoint> boolean isInLine(T p1, T p2) {
        if (p1.getI() == p2.getI())
            return i == p1.getI() && min(p1.getJ(), p2.getJ()) <= j && j <= max(p1.getJ(), p2.getJ());
        // Прямая, содержащая отрезок (в случае невертикального отрезка): y = a*x + b.
        double a = (p1.getJ() - p2.getJ()) / (p1.getI() - p2.getI() + 0.);
        double b = p1.getJ() - a * p1.getI();
        return j == a * i + b && min(p1.getI(), p2.getI()) <= i && i <= max(p1.getI(), p2.getI());
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
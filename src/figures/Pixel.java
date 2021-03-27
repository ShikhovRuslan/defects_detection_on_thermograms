package figures;

import static java.lang.Math.max;
import static java.lang.Math.min;


/**
 * Используется для хранения координат точек в системе координат c'x'y'.
 * <p>
 * С термограммой связана система c'x'y'z' пиксельных координат. Центр c' находится в нижней левой точке термограммы.
 * Ось c'x' направлена вдоль нижней стороны термограммы, ось c'y' - вдоль левой стороны термограммы, а ось c'z' - вверх.
 */
public class Pixel extends AbstractPoint {
    public Pixel(int i, int j) {
        super(i, j);
    }

    public Pixel(double iD, double jD) {
        super(iD, jD);
    }

    @Override
    public Pixel create(int i, int j) {
        return new Pixel(i, j);
    }

    /**
     * Конвертирует текущую точку из системы координат c'x'y' в систему координат Oxy.
     */
    public Point toPoint(int resY) {
        return new Point(resY - 1 - getJ(), getI());
    }

    /**
     * Возвращает точку пересечения отрезков [p1, p2] и [p3, p4] в случае, если они пересекаются по единственной точке и
     * эта точка является внутренней для каждого из этих отрезков. В противном случае возвращает точку
     * {@code ({@link Integer#MIN_VALUE},{@link Integer#MIN_VALUE})}.
     * <p>
     * (https://vscode.ru/prog-lessons/nayti-tochku-peresecheniya-otrezkov.html)
     */
    public static Pixel findIntersection(Pixel p1, Pixel p2, Pixel p3, Pixel p4) {
        Pixel no = new Pixel(Integer.MIN_VALUE, Integer.MIN_VALUE);

        // Добиваемся, чтобы было p1.i <= p2.i и p3.i <= p4.i.
        if (p2.getI() < p1.getI()) {
            Pixel tmp = p1;
            p1 = p2;
            p2 = tmp;
        }
        if (p4.getI() < p3.getI()) {
            Pixel tmp = p3;
            p3 = p4;
            p4 = tmp;
        }

        // У отрезков нет общей абсциссы или ординаты.
        if ((p2.getI() < p3.getI() || p4.getI() < p1.getI()) ||
                (max(p1.getJ(), p2.getJ()) < min(p3.getJ(), p4.getJ()) ||
                        max(p3.getJ(), p4.getJ()) < min(p1.getJ(), p2.getJ())))
            return no;

        // Отрезки либо оба вертикальные, либо оба горизонтальные.
        if (p1.getI() == p2.getI() && p3.getI() == p4.getI() || p1.getJ() == p2.getJ() && p3.getJ() == p4.getJ())
            return no;

        // Если второй отрезок вертикальный, то его делаем первым.
        if (p3.getI() == p4.getI()) {
            Pixel tmp1 = p1;
            Pixel tmp2 = p2;
            p1 = p3;
            p2 = p4;
            p3 = tmp1;
            p4 = tmp2;
        }

        //
        // Здесь второй отрезок невертикальный.
        //

        // Прямые, содержащие отрезки (в случае невертикальных отрезков):
        // y = a1*x + b1,
        // y = a2*x + b2.

        double x0, y0; // (x0, y0) - точка пересечения прямых, содержащих отрезки.
        double a2 = (p3.getJ() - p4.getJ()) / (p3.getI() - p4.getI() + 0.);
        double b2 = p3.getJ() - a2 * p3.getI();

        if (p1.getI() == p2.getI()) { // Первый отрезок вертикальный.
            x0 = p1.getI();
            y0 = a2 * x0 + b2;
            if (p3.getI() < x0 && x0 < p4.getI() &&
                    Math.min(p1.getJ(), p2.getJ()) < y0 && y0 < Math.max(p1.getJ(), p2.getJ()))
                return new Pixel(x0, y0);
        } else { // Первый отрезок невертикальный.
            double a1 = (p1.getJ() - p2.getJ()) / (p1.getI() - p2.getI() + 0.);
            double b1 = p1.getJ() - a1 * p1.getI();

            // Прямые параллельны (возможно совпадение прямых).
            if (a1 == a2)
                return no;

            x0 = (b2 - b1) / (a1 - a2);

            // Точка x0 находится на пересечении проекций внутренностей отрезков на ось абсцисс.
            if (Math.max(p1.getI(), p3.getI()) < x0 && x0 < Math.min(p2.getI(), p4.getI()))
                return new Pixel(x0, a1 * x0 + b1);
        }
        return no;
    }
}
package tk.ivybits.neural.network;

import static java.lang.Math.*;

/**
 * Standard operations on vectors.
 */
public class VecMath {
    public static double bipolar(double d) {
        return d <= 0 ? -1 : 1;
    }

    public static double clamp(double d, double l, double u) {
        return max(min(d, u), l);
    }

    public static double magnitudeOf(double v[]) {
        return sqrt(max(vectorLength(v), 1.E-30));
    }

    public static void normalize(double v[]) {
        double m = magnitudeOf(v);
        for (int i = 0; i < v.length; v[i++] /= m) ;
    }

    public static double vectorLength(double v[]) {
        double rtn = 0.0;
        for (double aV : v) rtn += aV * aV;
        return rtn;
    }

    public static double dot(double a[], double b[]) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }
}

package com.aero.control.testsuite;

import java.lang.reflect.Array;

/**
 * LINPACK benchmark for measuring floating-point performance. Solves a dense
 * system of linear equations and reports MFLOPS (millions of floating-point
 * operations per second). Used in the TestSuite to benchmark CPU performance.
 */
public class Linpack {
    private double mMFlops;
    private double mRuns;
    private double mTimePassed;
    double second_orig = -1.0d;

    private final double[][] a = (double[][]) Array.newInstance((Class<?>) Double.TYPE, 200, 201);
    private final double[] b = new double[200];
    private final double[] x = new double[200];
    private final int[] ipvt = new int[200];

    /**
     * Returns the total time spent in benchmarks.
     *
     * @return total benchmark time in seconds
     */
    public double getTimePassed() {
        return this.mTimePassed;
    }

    /**
     * Returns the average MFLOPS across all benchmark runs.
     *
     * @return average MFLOPS
     */
    public double getMFlops() {
        return this.mMFlops / this.mRuns;
    }

    /**
     * Resets all benchmark counters to zero.
     */
    public void resetBenchmark() {
        this.mTimePassed = 0.0d;
        this.mMFlops = 0.0d;
        this.mRuns = 0.0d;
    }

    public static void main(String[] args) {
        Linpack l = new Linpack();
        l.run_benchmark();
    }

    final double abs(double d) {
        return d >= 0.0d ? d : -d;
    }

    double second() {
        if (this.second_orig == -1.0d) {
            this.second_orig = System.nanoTime();
        }
        return (System.nanoTime() - this.second_orig) / 1.0E9d;
    }

    /**
     * Runs a single iteration of the LINPACK benchmark and updates timing/MFLOPS statistics.
     */
    public void run_benchmark() {
        double[][] a = this.a;
        double[] b = this.b;
        double[] x = this.x;
        int[] ipvt = this.ipvt;
        double ops = ((2.0d * ((double) 1000000)) / 3.0d) + (2.0d * ((double) 10000));
        matgen(a, 201, 100, b);
        double time = second();
        dgefa(a, 201, 100, ipvt);
        dgesl(a, 201, 100, ipvt, b, 0);
        double total = second() - time;
        for (int i = 0; i < 100; i++) {
            x[i] = b[i];
        }
        double norma = matgen(a, 201, 100, b);
        for (int i2 = 0; i2 < 100; i2++) {
            b[i2] = -b[i2];
        }
        dmxpy(100, b, 100, 201, x, a);
        double resid = 0.0d;
        double normx = 0.0d;
        for (int i3 = 0; i3 < 100; i3++) {
            if (resid <= abs(b[i3])) {
                resid = abs(b[i3]);
            }
            if (normx <= abs(x[i3])) {
                normx = abs(x[i3]);
            }
        }
        double eps_result = epslon(1.0d);
        double residn_result = resid / (((((double) 100) * norma) * normx) * eps_result);
        double d = ((double) ((int) (100.0d * (residn_result + 0.005d)))) / 100.0d;
        double time_result = total + 0.005d;
        double mflops_result = ops / (1000000.0d * total);
        this.mMFlops = this.mMFlops + (((double) ((int) (1000.0d * (mflops_result + 5.0E-4d)))) / 1000.0d);
        this.mTimePassed += ((double) ((int) (100.0d * time_result))) / 100.0d;
        this.mRuns += 1.0d;
    }

    final double matgen(double[][] a, int lda, int n, double[] b) {
        int init = 1325;
        double norma = 0.0d;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                init = (init * 3125) % 65536;
                a[j][i] = (((double) init) - 32768.0d) / 16384.0d;
                if (a[j][i] > norma) {
                    norma = a[j][i];
                }
            }
        }
        for (int i2 = 0; i2 < n; i2++) {
            b[i2] = 0.0d;
        }
        for (int j2 = 0; j2 < n; j2++) {
            for (int i3 = 0; i3 < n; i3++) {
                b[i3] = b[i3] + a[j2][i3];
            }
        }
        return norma;
    }

    final int dgefa(double[][] a, int lda, int n, int[] ipvt) {
        int info = 0;
        int nm1 = n - 1;
        if (nm1 >= 0) {
            for (int k = 0; k < nm1; k++) {
                double[] col_k = a[k];
                int kp1 = k + 1;
                int l = idamax(n - k, col_k, k, 1) + k;
                ipvt[k] = l;
                if (col_k[l] != 0.0d) {
                    if (l != k) {
                        double t = col_k[l];
                        col_k[l] = col_k[k];
                        col_k[k] = t;
                    }
                    dscal(n - kp1, (-1.0d) / col_k[k], col_k, kp1, 1);
                    for (int j = kp1; j < n; j++) {
                        double[] col_j = a[j];
                        double t2 = col_j[l];
                        if (l != k) {
                            col_j[l] = col_j[k];
                            col_j[k] = t2;
                        }
                        daxpy(n - kp1, t2, col_k, kp1, 1, col_j, kp1, 1);
                    }
                } else {
                    info = k;
                }
            }
        }
        ipvt[n - 1] = n - 1;
        return a[n + (-1)][n + (-1)] == 0.0d ? n - 1 : info;
    }

    final void dgesl(double[][] a, int lda, int n, int[] ipvt, double[] b, int job) {
        int nm1 = n - 1;
        if (job == 0) {
            if (nm1 >= 1) {
                int k = 0;
                while (true) {
                    int k2 = k;
                    if (k2 >= nm1) {
                        break;
                    }
                    int l = ipvt[k2];
                    double t = b[l];
                    if (l != k2) {
                        b[l] = b[k2];
                        b[k2] = t;
                    }
                    int kp1 = k2 + 1;
                    daxpy(n - kp1, t, a[k2], kp1, 1, b, kp1, 1);
                    k = k2 + 1;
                }
            }
            for (int kb = 0; kb < n; kb++) {
                int k3 = n - (kb + 1);
                b[k3] = b[k3] / a[k3][k3];
                double t2 = -b[k3];
                daxpy(k3, t2, a[k3], 0, 1, b, 0, 1);
            }
            return;
        }
        for (int k4 = 0; k4 < n; k4++) {
            double t3 = ddot(k4, a[k4], 0, 1, b, 0, 1);
            b[k4] = (b[k4] - t3) / a[k4][k4];
        }
        if (nm1 >= 1) {
            for (int kb2 = 1; kb2 < nm1; kb2++) {
                int k5 = n - (kb2 + 1);
                int kp12 = k5 + 1;
                b[k5] = b[k5] + ddot(n - kp12, a[k5], kp12, 1, b, kp12, 1);
                int l2 = ipvt[k5];
                if (l2 != k5) {
                    double t4 = b[l2];
                    b[l2] = b[k5];
                    b[k5] = t4;
                }
            }
        }
    }

    final void daxpy(int n, double da, double[] dx, int dx_off, int incx, double[] dy, int dy_off, int incy) {
        if (n > 0 && da != 0.0d) {
            if (incx != 1 || incy != 1) {
                int ix = incx < 0 ? ((-n) + 1) * incx : 0;
                int iy = incy < 0 ? ((-n) + 1) * incy : 0;
                for (int i = 0; i < n; i++) {
                    int i2 = iy + dy_off;
                    dy[i2] = dy[i2] + (dx[ix + dx_off] * da);
                    ix += incx;
                    iy += incy;
                }
                return;
            }
            for (int i3 = 0; i3 < n; i3++) {
                int i4 = i3 + dy_off;
                dy[i4] = dy[i4] + (dx[i3 + dx_off] * da);
            }
        }
    }

    final double ddot(int n, double[] dx, int dx_off, int incx, double[] dy, int dy_off, int incy) {
        double dtemp = 0.0d;
        if (n > 0) {
            if (incx != 1 || incy != 1) {
                int ix = incx < 0 ? ((-n) + 1) * incx : 0;
                int iy = incy < 0 ? ((-n) + 1) * incy : 0;
                for (int i = 0; i < n; i++) {
                    dtemp += dx[ix + dx_off] * dy[iy + dy_off];
                    ix += incx;
                    iy += incy;
                }
            } else {
                for (int i2 = 0; i2 < n; i2++) {
                    dtemp += dx[i2 + dx_off] * dy[i2 + dy_off];
                }
            }
        }
        return dtemp;
    }

    final void dscal(int n, double da, double[] dx, int dx_off, int incx) {
        if (n > 0) {
            if (incx != 1) {
                int nincx = n * incx;
                int i = 0;
                while (i < nincx) {
                    int i2 = i + dx_off;
                    dx[i2] = dx[i2] * da;
                    i += incx;
                }
                return;
            }
            for (int i3 = 0; i3 < n; i3++) {
                int i4 = i3 + dx_off;
                dx[i4] = dx[i4] * da;
            }
        }
    }

    final int idamax(int n, double[] dx, int dx_off, int incx) {
        int itemp = 0;
        if (n < 1) {
            return -1;
        }
        if (n == 1) {
            return 0;
        }
        if (incx != 1) {
            double dmax = abs(dx[dx_off + 0]);
            int ix = incx + 1;
            for (int i = 1; i < n; i++) {
                double dtemp = abs(dx[ix + dx_off]);
                if (dtemp > dmax) {
                    itemp = i;
                    dmax = dtemp;
                }
                ix += incx;
            }
            return itemp;
        }
        int itemp2 = 0;
        double dmax2 = abs(dx[dx_off + 0]);
        for (int i2 = 1; i2 < n; i2++) {
            double dtemp2 = abs(dx[i2 + dx_off]);
            if (dtemp2 > dmax2) {
                itemp2 = i2;
                dmax2 = dtemp2;
            }
        }
        return itemp2;
    }

    final double epslon(double x) {
        double eps = 0.0d;
        while (eps == 0.0d) {
            double b = 1.3333333333333333d - 1.0d;
            double c = b + b + b;
            eps = abs(c - 1.0d);
        }
        return abs(x) * eps;
    }

    final void dmxpy(int n1, double[] y, int n2, int ldm, double[] x, double[][] m) {
        for (int j = 0; j < n2; j++) {
            for (int i = 0; i < n1; i++) {
                y[i] = y[i] + (x[j] * m[j][i]);
            }
        }
    }
}

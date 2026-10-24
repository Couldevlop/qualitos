package com.openlab.qualitos.quality.spc.gagerr.domain;

/**
 * La loi de Fisher-Snedecor, ce qu'il en faut pour lire une ANOVA : la
 * probabilité de dépasser une valeur F.
 *
 * <p>Sans bibliothèque statistique : P(F > f) = I<sub>x</sub>(d2/2, d1/2) avec
 * x = d2 / (d2 + d1·f), où I est la fonction bêta incomplète régularisée,
 * calculée par fraction continue (algorithme de Lentz) et ln Γ par
 * l'approximation de Lanczos. Précision relative de l'ordre de 1e-12, bien
 * au-delà de ce qu'une décision de pooling à 0,25 demande.
 */
public final class FDistribution {

    private static final int MAX_ITERATIONS = 500;
    private static final double EPSILON = 1e-15;
    private static final double TINY = 1e-300;
    private static final double[] LANCZOS = {
            0.99999999999980993, 676.5203681218851, -1259.1392167224028, 771.32342877765313,
            -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6,
            1.5056327351493116e-7};

    private FDistribution() {}

    /**
     * P(F > f) pour une loi F à (d1, d2) degrés de liberté.
     *
     * @return 1 pour f ≤ 0, 0 pour f infini
     */
    public static double survival(double f, double d1, double d2) {
        if (!(d1 > 0) || !(d2 > 0) || Double.isNaN(f)) {
            throw new IllegalArgumentException("F distribution needs positive degrees of freedom and a number");
        }
        if (f <= 0) {
            return 1.0;
        }
        if (Double.isInfinite(f)) {
            return 0.0;
        }
        return regularizedBeta(d2 / (d2 + d1 * f), d2 / 2.0, d1 / 2.0);
    }

    /** I<sub>x</sub>(a, b), la bêta incomplète régularisée, pour 0 ≤ x ≤ 1. */
    static double regularizedBeta(double x, double a, double b) {
        if (x <= 0) {
            return 0.0;
        }
        if (x >= 1) {
            return 1.0;
        }
        double lnFront = lnGamma(a + b) - lnGamma(a) - lnGamma(b) + a * Math.log(x) + b * Math.log1p(-x);
        double front = Math.exp(lnFront);
        // La fraction continue converge vite pour x < (a + 1) / (a + b + 2) ; au-delà, la symétrie.
        if (x < (a + 1) / (a + b + 2)) {
            return front * continuedFraction(x, a, b) / a;
        }
        return 1.0 - front * continuedFraction(1 - x, b, a) / b;
    }

    /** La fraction continue de la bêta incomplète (Numerical Recipes, betacf, méthode de Lentz). */
    private static double continuedFraction(double x, double a, double b) {
        double qab = a + b;
        double qap = a + 1;
        double qam = a - 1;
        double c = 1.0;
        double d = 1.0 - qab * x / qap;
        d = Math.abs(d) < TINY ? TINY : d;
        d = 1.0 / d;
        double h = d;
        boolean converge = false;
        for (int m = 1; m <= MAX_ITERATIONS && !converge; m++) {
            int m2 = 2 * m;
            double aa = m * (b - m) * x / ((qam + m2) * (a + m2));
            d = 1.0 + aa * d;
            d = Math.abs(d) < TINY ? TINY : d;
            c = 1.0 + aa / c;
            c = Math.abs(c) < TINY ? TINY : c;
            d = 1.0 / d;
            h *= d * c;
            aa = -(a + m) * (qab + m) * x / ((a + m2) * (qap + m2));
            d = 1.0 + aa * d;
            d = Math.abs(d) < TINY ? TINY : d;
            c = 1.0 + aa / c;
            c = Math.abs(c) < TINY ? TINY : c;
            d = 1.0 / d;
            double del = d * c;
            h *= del;
            converge = Math.abs(del - 1.0) < EPSILON;
        }
        return h;
    }

    /** ln Γ(z) pour z > 0 (Lanczos, g = 7). */
    static double lnGamma(double z) {
        if (z < 0.5) {
            // Formule des compléments : Γ(z) Γ(1 − z) = π / sin(πz).
            return Math.log(Math.PI / Math.abs(Math.sin(Math.PI * z))) - lnGamma(1 - z);
        }
        double zz = z - 1;
        double s = LANCZOS[0];
        for (int i = 1; i < LANCZOS.length; i++) {
            s += LANCZOS[i] / (zz + i);
        }
        double t = zz + 7.5;
        return 0.5 * Math.log(2 * Math.PI) + (zz + 0.5) * Math.log(t) - t + Math.log(s);
    }
}

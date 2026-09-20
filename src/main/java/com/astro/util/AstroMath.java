package com.astro.util;

import java.time.OffsetDateTime;

/**
 * Core astrological mathematics.
 *
 * Sources:
 *   Jean Meeus, "Astronomical Algorithms", 2nd ed. (Willmann-Bell, 1998)
 *   Swiss Ephemeris documentation (Astrodienst)
 *   Fliegel & Van Flandern, Comm. ACM 11(10):657, 1968 (JDN formula)
 */
public final class AstroMath {

    private AstroMath() {}

    public static double julianDay(OffsetDateTime dateTime) {
        return dateTime.toEpochSecond() / 86400.0 + 2440587.5;
    }

    // ─── Basic trig helpers ───────────────────────────────────────────────────

    public static double toRad(double deg)  { return Math.toRadians(deg); }
    public static double toDeg(double rad)  { return Math.toDegrees(rad); }

    /** Normalise angle to [0, 360). */
    public static double norm360(double deg) {
        deg = deg % 360.0;
        return deg < 0 ? deg + 360.0 : deg;
    }

    /** Normalise angle to (-180, 180]. */
    public static double norm180(double deg) {
        deg = norm360(deg);
        return deg > 180.0 ? deg - 360.0 : deg;
    }

    // ─── Julian Day (Fliegel & Van Flandern / Meeus Ch.7) ────────────────────

    /**
     * Gregorian calendar → Julian Day Number (noon).
     * All integer divisions floor toward zero (Java int division semantics).
     */
    public static int julianDayNumber(int year, int month, int day) {
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    }

    /**
     * Full Julian Day including fractional day for UT time.
     * JD epoch: noon 1 Jan 4713 BC proleptic Julian = JD 0.0
     */
    public static double julianDay(int year, int month, int day,
                                   int hour, int minute, int second) {
        int jdn = julianDayNumber(year, month, day);
        // JDN is for noon; subtract 0.5 to anchor at midnight, then add time fraction.
        return jdn - 0.5 + hour / 24.0 + minute / 1440.0 + second / 86400.0;
    }

    /** Julian centuries since J2000.0 (JD 2451545.0 = 12h UT 1 Jan 2000). */
    public static double julianCenturies(double jd) {
        return (jd - 2451545.0) / 36525.0;
    }

    // ─── Ecliptic obliquity (Meeus Ch.22) ─────────────────────────────────────

    /**
     * Mean obliquity of the ecliptic (degrees), J2000.0-based.
     * Accuracy: ~1″ for several centuries around J2000.
     */
    public static double obliquity(double T) {
        return 23.439291111
             - 0.013004167 * T
             - 0.000000164 * T * T
             + 0.000000504 * T * T * T;
    }

    // ─── Greenwich Mean Sidereal Time (Meeus Ch.12) ───────────────────────────

    /**
     * GMST at the given Julian Day (UT), returned in degrees [0, 360).
     * Uses Meeus Eq.12.4 (accurate to ~0.1″ for centuries around J2000).
     */
    public static double gmst(double jd) {
        double T  = julianCenturies(jd);
        double θ  = 280.46061837
                  + 360.98564736629 * (jd - 2451545.0)
                  + 0.000387933 * T * T
                  - T * T * T / 38710000.0;
        return norm360(θ);
    }

    /**
     * Local Sidereal Time = GMST + observer's east longitude (degrees).
     */
    public static double localSiderealTime(double jd, double longitudeEast) {
        return norm360(gmst(jd) + longitudeEast);
    }

    // ─── Ascendant & MC (Meeus Ch.24) ────────────────────────────────────────

    /**
     * Midheaven (MC) ecliptic longitude.
     * @param ramc  RAMC = LST in degrees
     * @param eps   obliquity in degrees
     */
    public static double midheaven(double ramc, double eps) {
        double mc = toDeg(Math.atan2(Math.sin(toRad(ramc)),
            Math.cos(toRad(ramc)) * Math.cos(toRad(eps))));
        // atan2 result is in (-180,180]; place MC in correct quadrant
        return norm360(mc);
    }

    /**
     * Ascendant ecliptic longitude.
     * Formula: Asc = atan2(-cos(RAMC), sin(RAMC)·cos ε + tan φ·sin ε)
     *
     * @param ramc      RAMC = LST (degrees)
     * @param eps       obliquity (degrees)
     * @param latitude  geographic latitude (degrees, north positive)
     */
    public static double ascendant(double ramc, double eps, double latitude) {
        double cosRamc = Math.cos(toRad(ramc));
        double sinRamc = Math.sin(toRad(ramc));
        double cosEps  = Math.cos(toRad(eps));
        double sinEps  = Math.sin(toRad(eps));
        double tanLat  = Math.tan(toRad(latitude));

        double y = -cosRamc;
        double x =  sinRamc * cosEps + tanLat * sinEps;

        double asc = toDeg(Math.atan2(y, x));
        return norm360(asc + 180.0);
    }

    public static double[] placidusCusps(double ramc, double eps, double latitude) {
        if (!Double.isFinite(ramc) || !Double.isFinite(eps) || !Double.isFinite(latitude) ||
            eps <= 0 || Math.abs(latitude) + eps >= 90) {
            throw new IllegalArgumentException("Placidus houses require a non-polar latitude and valid obliquity");
        }
        ramc = norm360(ramc);
        double[] cusps = new double[12];
        cusps[0] = ascendant(ramc, eps, latitude);
        cusps[9] = midheaven(ramc, eps);
        cusps[3] = norm360(cusps[9] + 180);
        cusps[6] = norm360(cusps[0] + 180);
        cusps[10] = placidusCusp(ramc, eps, latitude, 1.0 / 3, false);
        cusps[11] = placidusCusp(ramc, eps, latitude, 2.0 / 3, false);
        cusps[1] = placidusCusp(ramc, eps, latitude, 2.0 / 3, true);
        cusps[2] = placidusCusp(ramc, eps, latitude, 1.0 / 3, true);
        for (int i : new int[]{1, 2, 10, 11}) {
            cusps[(i + 6) % 12] = norm360(cusps[i] + 180);
        }
        return cusps;
    }

    private static double placidusCusp(double ramc, double eps, double latitude,
                                      double fraction, boolean nocturnal) {
        double asc = toRad(ascendant(ramc, eps, latitude));
        double ascRa = toDeg(Math.atan2(Math.cos(toRad(eps)) * Math.sin(asc), Math.cos(asc)));
        double ascOffset = norm360(ascRa - ramc);
        double low = nocturnal ? ascOffset : 0;
        double high = nocturnal ? 180 : ascOffset;
        for (int i = 0; i < 80; i++) {
            double offset = (low + high) / 2;
            double ra = toRad(ramc + offset);
            double tanDeclination = Math.sin(ra) * Math.tan(toRad(eps));
            double ad = toDeg(Math.asin(Math.tan(toRad(latitude)) * tanDeclination));
            double target = nocturnal ? 180 - fraction * (90 - ad) : fraction * (90 + ad);
            if (offset > target) high = offset;
            else low = offset;
        }
        return midheaven(ramc + (low + high) / 2, eps);
    }

    // ─── Lahiri (Chitrapaksha) Ayanamsa ──────────────────────────────────────

    /**
     * Lahiri ayanamsa in degrees for the given JD.
     *
     * Calibrated to the 1985-revised IAU value of 23°15′00.658″ on 1956-03-21 0h ET.
     * Rate: 50.2388475 arcseconds/tropical year (IAU 1976 precession constant).
     *
     * Accuracy: within ~1–2 arcminutes of Swiss Ephemeris output for dates 1900–2100.
     */
    public static double lahiriAyanamsa(double jd) {
        // Reference epoch: 1956-03-21 0h ET → JD ≈ 2435553.5
        final double JD_REF      = 2435553.5;
        final double AYANAMSA_REF = 23.25018; // degrees at reference epoch
        final double RATE         = 50.2388475 / 3600.0 / 365.25; // deg/day
        return AYANAMSA_REF + (jd - JD_REF) * RATE;
    }

    // ─── Coordinate conversion helpers ───────────────────────────────────────

    /** RA (degrees) + declination (degrees) → ecliptic longitude (degrees). */
    public static double raDecToEclipticLon(double ra, double dec, double eps) {
        double sinDec = Math.sin(toRad(dec));
        double cosDec = Math.cos(toRad(dec));
        double sinRa  = Math.sin(toRad(ra));
        double cosEps = Math.cos(toRad(eps));
        double sinEps = Math.sin(toRad(eps));

        double y = sinRa * cosEps + Math.tan(toRad(dec)) * sinEps;
        double x = Math.cos(toRad(ra));
        return norm360(toDeg(Math.atan2(y, x)));
    }

    // ─── Kepler's equation solver ────────────────────────────────────────────

    /**
     * Solve Kepler's equation M = E − e·sin(E) for eccentric anomaly E.
     * Iterative Newton-Raphson; converges to 1e-10 rad in <10 iterations.
     *
     * @param M  mean anomaly (radians)
     * @param e  eccentricity
     * @return   eccentric anomaly (radians)
     */
    public static double solveKepler(double M, double e) {
        double E = M; // initial guess
        for (int i = 0; i < 50; i++) {
            double delta = (M - E + e * Math.sin(E)) / (1.0 - e * Math.cos(E));
            E += delta;
            if (Math.abs(delta) < 1e-10) break;
        }
        return E;
    }

    /**
     * True anomaly from eccentric anomaly and eccentricity.
     * @return true anomaly (radians)
     */
    public static double trueAnomaly(double E, double e) {
        return 2.0 * Math.atan2(
            Math.sqrt(1.0 + e) * Math.sin(E / 2.0),
            Math.sqrt(1.0 - e) * Math.cos(E / 2.0));
    }
}

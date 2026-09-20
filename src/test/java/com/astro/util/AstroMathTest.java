package com.astro.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AstroMathTest {

    @ParameterizedTest
    @CsvSource({
        "0,0,90,0", "90,19.076,180,90", "180,28.6139,257.756617234,180",
        "270,34.0837,0,270", "123.45,19.076,211.398657059,121.220781729",
        "359.9,34.0837,104.978629964,359.891006064", "223.5,-33.86,321.083763515,225.966351555"
    })
    void anglesMatchSwissEphemeris21003(double ramc, double latitude, double ascendant, double midheaven) {
        assertThat(AstroMath.norm180(AstroMath.ascendant(ramc, 23.439291111, latitude) - ascendant))
            .isCloseTo(0, within(1e-7));
        assertThat(AstroMath.norm180(AstroMath.midheaven(ramc, 23.439291111) - midheaven))
            .isCloseTo(0, within(1e-7));
    }

    @ParameterizedTest
    @CsvSource({"2448724.5,133.176821116", "2451545.0,223.323775438", "2447906.375,159.634234298", "2460000.5,38.647746602"})
    void lunarLongitudeMatchesReferenceWithinTruncatedSeriesTolerance(double jd, double longitude) {
        assertThat(AstroMath.norm180(Ephemeris.moonLongitude(jd) - longitude)).isCloseTo(0, within(0.05));
    }

    @Test
    void placidusCuspsMatchIndependentNorthernSouthernAndEquatorialFixtures() {
        double[][] fixtures = {
            {0, 0, 90,117.910549786,147.818740832,180,212.181259168,242.089450214,270,297.910549786,327.818740832,0,32.181259168,62.089450214},
            {123.45,19.076,211.398657059,240.702737759,270.528378667,301.220781729,332.716263673,3.418106594,31.398657059,60.702737759,90.528378667,121.220781729,152.716263673,183.418106594},
            {223.5,-33.86,321.083763515,344.969511054,13.383598207,45.966351555,79.916958092,112.022857287,141.083763515,164.969511054,193.383598207,225.966351555,259.916958092,292.022857287}
        };
        for (double[] fixture : fixtures) {
            double[] actual = AstroMath.placidusCusps(fixture[0], 23.439291111, fixture[1]);
            for (int i = 0; i < 12; i++) {
                assertThat(AstroMath.norm180(actual[i] - fixture[i + 2])).isCloseTo(0, within(1e-7));
            }
        }
    }

    @Test
    void rejectsUndefinedPolarHouses() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> AstroMath.placidusCusps(0, 23.439, 70))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void julianDayMatchesJ2000Epoch() {
        assertThat(AstroMath.julianDay(2000, 1, 1, 12, 0, 0)).isEqualTo(2451545.0);
    }
}

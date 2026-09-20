package com.astro.service;

import com.astro.model.*;
import com.astro.util.AstroMath;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ChartCalculationTest {

    private final CityService cities = new CityService();
    private final VedicService vedic = new VedicService(cities);
    private final WesternService western = new WesternService(cities);
    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @CsvSource({
        "1990-01-15,02:30", "2000-01-01,00:00:01", "2000-03-01,05:29:59",
        "1900-03-01,00:00", "2000-02-29,05:30", "1990-01-15,14:30:59", "2024-12-31,23:59:59"
    })
    void bothChartsUseTheSamePreciseUtcInstant(String date, String time) {
        BirthRequest request = request(date, time);
        var v = vedic.compute(request).getInput();
        var w = western.compute(request).getInput();
        var instant = LocalDateTime.parse(date + "T" + time).toInstant(ZoneOffset.ofHoursMinutes(5, 30));
        double expectedJd = instant.getEpochSecond() / 86400.0 + 2440587.5;
        assertThat(v.utcDateTime()).isEqualTo(instant.toString());
        assertThat(w.utcDateTime()).isEqualTo(instant.toString());
        assertThat(v.julianDay()).isCloseTo(expectedJd, within(1e-8));
        assertThat(w.julianDay()).isCloseTo(expectedJd, within(1e-8));
    }

    @Test
    void placidusCuspsMatchSwissEphemeris21003ForMumbai() {
        var chart = western.compute(request("1990-01-15", "14:30"));
        double[] expected = {61.976891416, 88.326364745, 113.193262436, 140.021311936,
            171.326901916, 206.770387424, 241.976891416, 268.326364745,
            293.193262436, 320.021311936, 351.326901916, 26.770387424};
        for (int i = 0; i < 12; i++) {
            assertThat(AstroMath.norm180(chart.getHouses().get(i).degree() - expected[i]))
                .as("cusp %s", i + 1).isCloseTo(0, within(0.0051));
        }
        assertThat(chart.getAscendant().sign()).isEqualTo("Gemini");
    }

    @Test
    void westernPlanetsBelongToTheirActualCuspIntervals() {
        for (int hour = 0; hour < 24; hour++) {
            var chart = western.compute(request("1990-01-15", "%02d:30".formatted(hour)));
            for (var planet : chart.getPlanets()) {
                int index = planet.getHouse() - 1;
                double start = chart.getHouses().get(index).degree();
                double end = chart.getHouses().get((index + 1) % 12).degree();
                assertThat(AstroMath.norm360(planet.getTropicalLongitude() - start))
                    .as("%s at hour %s", planet.getName(), hour).isLessThan(AstroMath.norm360(end - start));
            }
        }
    }

    @Test
    void lagnaNakshatraAndPadaComeFromAscendantRatherThanMoon() {
        var lagna = vedic.compute(request("1990-01-15", "14:30")).getLagna();
        double longitude = (lagna.rashi() - 1) * 30.0 + lagna.degree();
        assertThat(lagna.pada()).isEqualTo((int) ((longitude % (360.0 / 27)) / (360.0 / 108)) + 1);
        assertThat(lagna.nakshatra()).isEqualTo("Krittika");
    }

    @Test
    void ashtakavargaMatchesRamanStandardHoroscopeSunTable() {
        String[] names = {"Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn"};
        int[] signs = {6, 11, 8, 7, 3, 6, 5};
        List<PlanetPosition> planets = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            PlanetPosition planet = new PlanetPosition();
            planet.setName(names[i]);
            planet.setRashi(signs[i]);
            planets.add(planet);
        }
        Map<String, Object> summary = ReflectionTestUtils.invokeMethod(vedic, "ashtakavargaSummary", planets, 9);
        JsonNode json = mapper.valueToTree(summary);
        assertThat(json.path("bhinnashtakavarga").path("Sun"))
            .isEqualTo(mapper.valueToTree(List.of(5, 3, 5, 4, 4, 4, 3, 5, 5, 0, 5, 5)));
        assertThat(json.path("sarvashtakavargaTotal").asInt()).isEqualTo(337);
    }

    @ParameterizedTest
    @CsvSource({"1990-01-15,14:30", "2000-02-29,02:00", "1985-07-12,23:59"})
    void ashtakavargaHasPersonalizedSignScoresAndConservedTotals(String date, String time) {
        JsonNode summary = mapper.valueToTree(vedic.compute(request(date, time)).getAshtakavargaSummary());
        int[] sav = new int[12];
        int[] totals = {48, 49, 39, 54, 56, 52, 39};
        String[] names = {"Sun", "Moon", "Mars", "Mercury", "Jupiter", "Venus", "Saturn"};
        for (int i = 0; i < names.length; i++) {
            JsonNode scores = summary.path("bhinnashtakavarga").path(names[i]);
            assertThat(scores.size()).isEqualTo(12);
            int sum = 0;
            for (int sign = 0; sign < 12; sign++) {
                int score = scores.get(sign).asInt();
                assertThat(score).isBetween(0, 8);
                sav[sign] += score;
                sum += score;
            }
            assertThat(sum).isEqualTo(totals[i]);
            assertThat(summary.path("bhinnashtakavargaTotals").path(names[i]).asInt()).isEqualTo(sum);
        }
        assertThat(summary.path("sarvashtakavarga")).isEqualTo(mapper.valueToTree(sav));
        assertThat(java.util.Arrays.stream(sav).sum()).isEqualTo(337);
        var other = request(date, "08:00");
        assertThat(summary.path("bhinnashtakavarga"))
            .isNotEqualTo(mapper.valueToTree(vedic.compute(other).getAshtakavargaSummary()).path("bhinnashtakavarga"));
    }

    @Test
    void westernHouseBoundariesAreStartInclusiveAndWrapAtAries() {
        double[] cusps = AstroMath.placidusCusps(123.45, 23.439291111, 19.076);
        for (int i = 0; i < 12; i++) {
            assertThat(WesternService.houseOf(cusps[i], cusps)).isEqualTo(i + 1);
            assertThat(WesternService.houseOf(AstroMath.norm360(cusps[i] - 0.000001), cusps))
                .isEqualTo(i == 0 ? 12 : i);
        }
        assertThat(WesternService.houseOf(0, cusps)).isEqualTo(5);
    }

    @Test
    void westernAspectsReportWhetherTheirOrbIsShrinking() {
        var chart = western.compute(request("1990-01-15", "14:30"));
        var later = western.compute(request("1990-01-15", "14:31"));
        Map<String, Double> laterLongitudes = new java.util.HashMap<>();
        later.getPlanets().forEach(p -> laterLongitudes.put(p.getName(), p.getTropicalLongitude()));
        Map<String, Double> angles = Map.of("Conjunction", 0.0, "Sextile", 60.0, "Square", 90.0,
            "Trine", 120.0, "Quincunx", 150.0, "Opposition", 180.0);
        for (var aspect : chart.getAspects()) {
            double nextSeparation = Math.abs(AstroMath.norm180(laterLongitudes.get(aspect.planet2()) - laterLongitudes.get(aspect.planet1())));
            double nextOrb = Math.abs(nextSeparation - angles.get(aspect.type()));
            double first = chart.getPlanets().stream().filter(p -> p.getName().equals(aspect.planet1())).findFirst().orElseThrow().getTropicalLongitude();
            double second = chart.getPlanets().stream().filter(p -> p.getName().equals(aspect.planet2())).findFirst().orElseThrow().getTropicalLongitude();
            double orb = Math.abs(Math.abs(AstroMath.norm180(second - first)) - angles.get(aspect.type()));
            assertThat(aspect.applying()).as("%s %s %s", aspect.planet1(), aspect.type(), aspect.planet2())
                .isEqualTo(nextOrb < orb);
        }
    }

    @Test
    void birthDashasUseElapsedMahaRatherThanCurrentDate() {
        for (String date : List.of("1990-01-15", "2090-01-15")) {
            DashaInfo dasha = ReflectionTestUtils.invokeMethod(vedic, "computeDasha", 100.0, date);
            assertThat(dasha.getBirthMahadasha()).isEqualTo("Saturn");
            assertThat(dasha.getBirthAntardasha()).isEqualTo("Venus");
            assertThat(dasha.getBirthPratyantardasha()).isEqualTo("Mercury");
        }
    }

    @Test
    void dashaChildrenPartitionTheirParentsWithoutGapsOrOverflow() {
        var sequence = vedic.compute(request("1990-01-15", "14:30")).getDasha().getDashaSequence();
        String cursor = "1990-01-15";
        for (var maha : sequence) {
            assertThat(maha.start()).isEqualTo(cursor);
            String antarCursor = maha.start();
            for (var antar : maha.antardashas()) {
                assertThat(antar.start()).isEqualTo(antarCursor);
                String ptCursor = antar.start();
                for (var pt : antar.pratyantardashas()) {
                    assertThat(pt.start()).isEqualTo(ptCursor);
                    assertThat(LocalDate.parse(pt.end())).isAfter(LocalDate.parse(pt.start()));
                    ptCursor = pt.end();
                }
                assertThat(ptCursor).isEqualTo(antar.end());
                antarCursor = antar.end();
            }
            assertThat(antarCursor).isEqualTo(maha.end());
            cursor = maha.end();
        }
        assertThat(cursor).isEqualTo(LocalDate.parse("1990-01-15").plusDays(43830).toString());
    }

    @ParameterizedTest
    @CsvSource({"0", "13.333333333332", "100", "359.99999999999"})
    void dashaBoundaryBirthsHaveNonemptyNestedPeriods(double moonLongitude) {
        DashaInfo dasha = ReflectionTestUtils.invokeMethod(vedic, "computeDasha", moonLongitude, "2000-01-01");
        assertThat(dasha.getBirthAntardasha()).isNotBlank();
        assertThat(dasha.getBirthPratyantardasha()).isNotBlank();
        String cursor = "2000-01-01";
        for (var maha : dasha.getDashaSequence()) {
            assertThat(maha.start()).isEqualTo(cursor);
            assertThat(maha.antardashas()).isNotEmpty();
            assertThat(maha.antardashas().getFirst().start()).isEqualTo(maha.start());
            assertThat(maha.antardashas().getLast().end()).isEqualTo(maha.end());
            for (var antar : maha.antardashas()) {
                assertThat(antar.pratyantardashas()).isNotEmpty();
                assertThat(antar.pratyantardashas().getFirst().start()).isEqualTo(antar.start());
                assertThat(antar.pratyantardashas().getLast().end()).isEqualTo(antar.end());
            }
            cursor = maha.end();
        }
        assertThat(cursor).isEqualTo(LocalDate.parse("2000-01-01").plusDays(43830).toString());
    }

    @ParameterizedTest
    @CsvSource({"9,Own", "10,Own", "11,Neutral"})
    void saturnOwnsCapricornAndAquariusButNotPisces(int signIndex, String expected) {
        String dignity = ReflectionTestUtils.invokeMethod(vedic, "dignity", "Saturn", signIndex);
        assertThat(dignity).isEqualTo(expected);
    }

    @Test
    void everySupportedCityProducesFiniteCharts() {
        for (String city : cities.listCities()) {
            var request = request("2000-02-29", "05:29:59");
            request.setCity(city);
            var v = vedic.compute(request);
            var w = western.compute(request);
            assertThat(v.getPlanets()).hasSize(9);
            assertThat(w.getHouses()).hasSize(12);
            w.getHouses().forEach(h -> assertThat(h.degree()).isBetween(0.0, Math.nextDown(360.0)));
            v.getPlanets().forEach(p -> {
                assertThat(p.getHouse()).isBetween(1, 12);
                assertThat(p.getNakshatra()).isBetween(1, 27);
                assertThat(p.getPada()).isBetween(1, 4);
            });
        }
    }

    @Test
    void predictionUpcomingDashasExcludePastPeriods() {
        var request = request("1990-01-15", "14:30");
        var engine = new com.astro.prediction.PredictionEngine(vedic);
        String section = engine.generate(request).getSections().get("Vimshottari Dasha — Timing of Life");
        String upcoming = section.substring(section.indexOf("UPCOMING MAHADASHAS:"));
        LocalDate today = LocalDate.now(ZoneOffset.ofHoursMinutes(5, 30));
        for (var period : vedic.compute(request).getDasha().getDashaSequence()) {
            if (!LocalDate.parse(period.start()).isAfter(today)) assertThat(upcoming).doesNotContain(period.start());
        }
    }

    private static BirthRequest request(String date, String time) {
        BirthRequest request = new BirthRequest();
        request.setDob(date);
        request.setTime(time);
        request.setCity("Mumbai");
        return request;
    }
}

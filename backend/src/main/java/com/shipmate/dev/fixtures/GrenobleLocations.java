package com.shipmate.dev.fixtures;

import java.util.List;
import java.util.Random;

public final class GrenobleLocations {

    public record GeoPoint(double lat, double lng, String label) {}

    public static final GeoPoint CENTRE =
            new GeoPoint(45.188529, 5.724524, "Grenoble Centre");

    public static final GeoPoint MEYLAN =
            new GeoPoint(45.209000, 5.778000, "Meylan");

    public static final GeoPoint ECHIROLLES =
            new GeoPoint(45.145000, 5.719000, "Échirolles");

    public static final GeoPoint SAINT_MARTIN =
            new GeoPoint(45.171000, 5.762000, "Saint-Martin-d'Hères");

    public static final GeoPoint VOIRON =
            new GeoPoint(45.364000, 5.590000, "Voiron");

    public static final List<GeoPoint> ALL = List.of(
            CENTRE,
            MEYLAN,
            ECHIROLLES,
            SAINT_MARTIN,
            VOIRON
    );

    private static final Random RANDOM = new Random();

    public static GeoPoint random() {
        return ALL.get(RANDOM.nextInt(ALL.size()));
    }

    private GrenobleLocations() {}
}

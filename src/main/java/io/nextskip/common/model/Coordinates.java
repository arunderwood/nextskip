package io.nextskip.common.model;

/**
 * Geographic coordinates in decimal degrees.
 *
 * @param latitude  Latitude in decimal degrees (-90 to +90)
 * @param longitude Longitude in decimal degrees (-180 to +180)
 */
public record Coordinates(
        double latitude,
        double longitude
) {
    public Coordinates {
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }
    }

    /**
     * Calculate distance to another coordinate using the Haversine formula.
     *
     * @param other The other coordinates
     * @return Distance in kilometers
     */
    public double distanceTo(Coordinates other) {
        final double earthRadiusKm = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(other.latitude - this.latitude);
        double lonDistance = Math.toRadians(other.longitude - this.longitude);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }
}

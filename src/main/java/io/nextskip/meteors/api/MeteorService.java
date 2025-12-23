package io.nextskip.meteors.api;

import io.nextskip.meteors.model.MeteorShower;

import java.util.List;
import java.util.Optional;

/**
 * Public API for meteor shower data.
 *
 * <p>This is the module's public contract. Other modules should depend on this
 * interface rather than internal implementations.
 */
public interface MeteorService {

    /**
     * Get all relevant meteor showers (active + upcoming within lookahead period).
     *
     * @return list of meteor showers sorted by peak date
     */
    List<MeteorShower> getMeteorShowers();

    /**
     * Get the currently most significant meteor shower.
     *
     * <p>Returns the shower with the highest score (if any are active/upcoming).
     *
     * @return the most significant shower, or empty if none relevant
     */
    Optional<MeteorShower> getPrimaryShower();

    /**
     * Get active showers (currently within visibility window).
     *
     * @return list of currently active meteor showers
     */
    List<MeteorShower> getActiveShowers();

    /**
     * Get upcoming showers (not yet started but within lookahead).
     *
     * @return list of upcoming meteor showers
     */
    List<MeteorShower> getUpcomingShowers();
}

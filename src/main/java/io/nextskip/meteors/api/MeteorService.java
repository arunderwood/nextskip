package io.nextskip.meteors.api;

import io.nextskip.meteors.model.MeteorShower;

import java.util.List;

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

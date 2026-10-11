package com.gamecoach.datasource;

import com.gamecoach.model.CareerProfile;

import java.util.Optional;

/**
 * For games that only expose career aggregates (Overwatch).
 * Implementations wrap one provider, e.g. a self-hosted OverFast instance.
 */
public interface CareerStatsSource {

    /** Empty if the player is not found or their profile is private. */
    Optional<CareerProfile> getCareerProfile(String battleTag);
}

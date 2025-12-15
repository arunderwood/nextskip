package io.nextskip.common.api;

/**
 * Common interface for all activity data that can be scored and evaluated for favorability.
 *
 * <p>This interface establishes a contract for all domain models representing ham radio
 * activities (propagation, activations, contests, satellites, etc.) to provide consistent
 * scoring and favorability assessment across the dashboard.
 *
 * <p>Implementing classes should provide deterministic scoring based on their specific
 * activity conditions, enabling the dashboard to prioritize and visually highlight the
 * most favorable opportunities for radio operations.
 */
public interface Scoreable {

    /**
     * Determines if current conditions are favorable for this activity.
     *
     * <p>This is a simplified boolean assessment used as a strong signal in the
     * priority calculation. When true, this significantly boosts the activity's
     * dashboard priority.
     *
     * @return true if conditions are favorable, false otherwise
     */
    boolean isFavorable();

    /**
     * Provides a numeric score representing the quality or favorability of current conditions.
     *
     * <p>Scores should be normalized to a 0-100 scale where:
     * <ul>
     *     <li>0 = Poor/unfavorable conditions</li>
     *     <li>50 = Moderate conditions</li>
     *     <li>100 = Excellent/optimal conditions</li>
     * </ul>
     *
     * <p>This score is combined with the favorable flag and other factors to calculate
     * the final dashboard priority for the activity.
     *
     * @return score from 0-100 representing condition quality
     */
    int getScore();
}

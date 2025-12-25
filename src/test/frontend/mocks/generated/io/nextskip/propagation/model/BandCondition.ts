/**
 * Mock stub for Hilla-generated BandCondition interface.
 * Used in tests to avoid dependency on Gradle code generation.
 */
import type FrequencyBand from '../../../common/model/FrequencyBand';
import type BandConditionRating from './BandConditionRating';

interface BandCondition {
  band?: FrequencyBand;
  rating?: BandConditionRating;
  confidence: number;
  notes?: string;
  score: number;
  favorable: boolean;
}
export default BandCondition;

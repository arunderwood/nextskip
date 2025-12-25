/**
 * Mock stub for Hilla-generated PropagationResponse interface.
 * Used in tests to avoid dependency on Gradle code generation.
 */
import type BandCondition from '../model/BandCondition';
import type SolarIndices from '../model/SolarIndices';

interface PropagationResponse {
  solarIndices?: SolarIndices;
  bandConditions?: Array<BandCondition | undefined>;
  timestamp?: string;
}
export default PropagationResponse;

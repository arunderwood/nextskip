/**
 * Mock stub for Hilla-generated Activation interface.
 * Used in tests to avoid dependency on Gradle code generation.
 */
import type ActivationLocation from './ActivationLocation';
import type ActivationType from './ActivationType';

interface Activation {
  spotId?: string;
  activatorCallsign?: string;
  type?: ActivationType;
  frequency?: number;
  mode?: string;
  spottedAt?: string;
  qsoCount?: number;
  source?: string;
  location?: ActivationLocation;
  score: number;
  favorable: boolean;
}
export default Activation;

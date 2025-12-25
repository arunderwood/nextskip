/**
 * Mock stub for Hilla-generated SolarIndices interface.
 * Used in tests to avoid dependency on Gradle code generation.
 */
interface SolarIndices {
  solarFluxIndex: number;
  aIndex: number;
  kIndex: number;
  sunspotNumber: number;
  timestamp?: string;
  source?: string;
  geomagneticActivity?: string;
  score: number;
  favorable: boolean;
  solarFluxLevel?: string;
}
export default SolarIndices;

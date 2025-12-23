/**
 * Solar Indices Utilities
 *
 * Shared utilities for working with solar propagation indices.
 * Used by SolarIndicesCard and SolarIndicesContent components.
 */

/**
 * Get solar flux level classification and styling
 */
export function getSolarFluxLevel(sfi: number): { label: string; className: string } {
  if (sfi >= 200) return { label: 'Very High', className: 'status-good' };
  if (sfi >= 150) return { label: 'High', className: 'status-good' };
  if (sfi >= 100) return { label: 'Moderate', className: 'status-fair' };
  if (sfi >= 70) return { label: 'Low', className: 'status-poor' };
  return { label: 'Very Low', className: 'status-poor' };
}

/**
 * Get geomagnetic K-index level classification and styling
 */
export function getGeomagneticLevel(kIndex: number): { label: string; className: string } {
  if (kIndex === 0) return { label: 'Quiet', className: 'status-good' };
  if (kIndex <= 2) return { label: 'Settled', className: 'status-good' };
  if (kIndex <= 4) return { label: 'Unsettled', className: 'status-fair' };
  if (kIndex <= 6) return { label: 'Active', className: 'status-fair' };
  if (kIndex <= 8) return { label: 'Storm', className: 'status-poor' };
  return { label: 'Severe Storm', className: 'status-poor' };
}

/**
 * Extended type for ActivationLocation with actual properties.
 *
 * Hilla generates an empty interface for Java interfaces, so we manually
 * declare the fields that are available on the concrete implementations (Park and Summit).
 */
export interface ActivationLocationExt {
  reference?: string;
  name?: string;
  regionCode?: string;
  /** SOTA association code (e.g., "W7W", "JA"), only present for Summit locations */
  associationCode?: string;
}

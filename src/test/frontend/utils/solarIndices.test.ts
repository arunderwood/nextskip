import { describe, it, expect } from 'vitest';
import { getSolarFluxLevel, getGeomagneticLevel } from 'Frontend/utils/solarIndices';

describe('solarIndices utilities', () => {
  describe('getSolarFluxLevel', () => {
    it('should return Very High for SFI >= 200', () => {
      expect(getSolarFluxLevel(200)).toEqual({
        label: 'Very High',
        className: 'status-good',
      });
      expect(getSolarFluxLevel(250)).toEqual({
        label: 'Very High',
        className: 'status-good',
      });
    });

    it('should return High for SFI >= 150', () => {
      expect(getSolarFluxLevel(150)).toEqual({
        label: 'High',
        className: 'status-good',
      });
      expect(getSolarFluxLevel(199)).toEqual({
        label: 'High',
        className: 'status-good',
      });
    });

    it('should return Moderate for SFI >= 100', () => {
      expect(getSolarFluxLevel(100)).toEqual({
        label: 'Moderate',
        className: 'status-fair',
      });
      expect(getSolarFluxLevel(149)).toEqual({
        label: 'Moderate',
        className: 'status-fair',
      });
    });

    it('should return Low for SFI >= 70', () => {
      expect(getSolarFluxLevel(70)).toEqual({
        label: 'Low',
        className: 'status-poor',
      });
      expect(getSolarFluxLevel(99)).toEqual({
        label: 'Low',
        className: 'status-poor',
      });
    });

    it('should return Very Low for SFI < 70', () => {
      expect(getSolarFluxLevel(69)).toEqual({
        label: 'Very Low',
        className: 'status-poor',
      });
      expect(getSolarFluxLevel(50)).toEqual({
        label: 'Very Low',
        className: 'status-poor',
      });
      expect(getSolarFluxLevel(0)).toEqual({
        label: 'Very Low',
        className: 'status-poor',
      });
    });

    it('should handle negative values', () => {
      expect(getSolarFluxLevel(-10)).toEqual({
        label: 'Very Low',
        className: 'status-poor',
      });
    });
  });

  describe('getGeomagneticLevel', () => {
    it('should return Quiet for K-index 0', () => {
      expect(getGeomagneticLevel(0)).toEqual({
        label: 'Quiet',
        className: 'status-good',
      });
    });

    it('should return Settled for K-index 1-2', () => {
      expect(getGeomagneticLevel(1)).toEqual({
        label: 'Settled',
        className: 'status-good',
      });
      expect(getGeomagneticLevel(2)).toEqual({
        label: 'Settled',
        className: 'status-good',
      });
    });

    it('should return Unsettled for K-index 3-4', () => {
      expect(getGeomagneticLevel(3)).toEqual({
        label: 'Unsettled',
        className: 'status-fair',
      });
      expect(getGeomagneticLevel(4)).toEqual({
        label: 'Unsettled',
        className: 'status-fair',
      });
    });

    it('should return Active for K-index 5-6', () => {
      expect(getGeomagneticLevel(5)).toEqual({
        label: 'Active',
        className: 'status-fair',
      });
      expect(getGeomagneticLevel(6)).toEqual({
        label: 'Active',
        className: 'status-fair',
      });
    });

    it('should return Storm for K-index 7-8', () => {
      expect(getGeomagneticLevel(7)).toEqual({
        label: 'Storm',
        className: 'status-poor',
      });
      expect(getGeomagneticLevel(8)).toEqual({
        label: 'Storm',
        className: 'status-poor',
      });
    });

    it('should return Severe Storm for K-index >= 9', () => {
      expect(getGeomagneticLevel(9)).toEqual({
        label: 'Severe Storm',
        className: 'status-poor',
      });
      expect(getGeomagneticLevel(10)).toEqual({
        label: 'Severe Storm',
        className: 'status-poor',
      });
    });

    it('should handle negative values as Settled', () => {
      // Negative values are <= 2, so they fall into Settled category
      expect(getGeomagneticLevel(-1)).toEqual({
        label: 'Settled',
        className: 'status-good',
      });
    });
  });
});

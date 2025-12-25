/**
 * Propagation Help Content - Help registration for Solar Indices and Band Conditions.
 */

import React from 'react';
import { Sun, Radio } from 'lucide-react';
import { registerHelp } from '../../help/HelpRegistry';
import type { HelpDefinition } from '../../help/types';

/**
 * Solar Indices Help Content
 */
function SolarIndicesHelpContent() {
  return (
    <div className="help-content">
      <p>
        Real-time solar activity measurements (SFI, sunspots, K-index, A-index) that directly affect HF radio
        propagation conditions.
      </p>
      <p>
        <strong>Hot when:</strong> High SFI (120+) with low K-index (0-2) indicates excellent propagation.
      </p>
    </div>
  );
}

/**
 * Band Conditions Help Content
 */
function BandConditionsHelpContent() {
  return (
    <div className="help-content">
      <p>
        Current propagation quality for each HF amateur radio band, with separate day and night predictions rated as
        Good, Fair, or Poor.
      </p>
      <p>
        <strong>Hot when:</strong> Multiple bands showing Good conditions, especially the higher bands (15m, 10m) which
        indicate strong solar activity.
      </p>
    </div>
  );
}

// Register help definitions
const solarIndicesHelp: HelpDefinition = {
  id: 'solar-indices',
  title: 'Solar Indices',
  icon: <Sun size={16} />,
  order: 10,
  Content: SolarIndicesHelpContent,
};

const bandConditionsHelp: HelpDefinition = {
  id: 'band-conditions',
  title: 'Band Conditions',
  icon: <Radio size={16} />,
  order: 20,
  Content: BandConditionsHelpContent,
};

registerHelp(solarIndicesHelp);
registerHelp(bandConditionsHelp);

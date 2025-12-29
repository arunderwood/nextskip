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
 *
 * Rating definitions based on HamQSL/N0NBH calculations:
 * @see https://www.hamqsl.com/FAQ.html
 * @see https://www.sdra.do/en/2024/04/25/entendiendo-el-banner-de-condiciones-en-las-bandas-de-hf/
 */
function BandConditionsHelpContent() {
  return (
    <div className="help-content">
      <p>
        Each HF band is displayed as an individual card, sorted by propagation quality. Bands with the best conditions
        appear first on the dashboard.
      </p>
      <ul>
        <li>
          <strong>Good:</strong> Long-distance DX via multiple ionospheric hops
        </li>
        <li>
          <strong>Fair:</strong> Regional contacts possible with 1-2 hops
        </li>
        <li>
          <strong>Poor:</strong> Skywave propagation largely unavailable
        </li>
      </ul>
      <p>
        <strong>Hot when:</strong> Band shows Good conditions, indicating strong ionospheric support for long-distance
        contacts.
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
  id: 'band-condition',
  title: 'Band Conditions',
  icon: <Radio size={16} />,
  order: 20,
  Content: BandConditionsHelpContent,
};

registerHelp(solarIndicesHelp);
registerHelp(bandConditionsHelp);

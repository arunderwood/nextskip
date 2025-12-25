/**
 * Meteor Showers Help Content - Help registration for Meteor Shower tracking.
 */

import React from 'react';
import { Sparkles } from 'lucide-react';
import { registerHelp } from '../../help/HelpRegistry';
import type { HelpDefinition } from '../../help/types';

/**
 * Meteor Showers Help Content
 */
function MeteorShowersHelpContent() {
  return (
    <div className="help-content">
      <p>
        Current and upcoming meteor showers that enable VHF propagation via meteor scatter. Meteors leave ionized trails
        that briefly reflect radio signals for contacts on 6m and 2m.
      </p>
      <p>
        <strong>Hot when:</strong> Active showers near peak dates, especially major showers (Perseids, Geminids) with
        high ZHR.
      </p>
    </div>
  );
}

// Register help definition
const meteorShowersHelp: HelpDefinition = {
  id: 'meteor-showers',
  title: 'Meteor Showers',
  icon: <Sparkles size={16} />,
  order: 50,
  Content: MeteorShowersHelpContent,
};

registerHelp(meteorShowersHelp);

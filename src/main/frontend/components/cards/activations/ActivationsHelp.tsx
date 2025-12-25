/**
 * Activations Help Content - Help registration for POTA and SOTA activations.
 */

import React from 'react';
import { TreePine, Mountain } from 'lucide-react';
import { registerHelp } from '../../help/HelpRegistry';
import type { HelpDefinition } from '../../help/types';

/**
 * POTA Activations Help Content
 */
function PotaActivationsHelpContent() {
  return (
    <div className="help-content">
      <p>
        Parks on the Air (POTA) activations currently live. Operators set up portable stations in
        parks and protected areas while hunters try to contact them.
      </p>
      <p>
        <strong>Hot when:</strong> Many activations on bands with good propagation and fresh spots.
      </p>
    </div>
  );
}

// Register POTA help definition
const potaActivationsHelp: HelpDefinition = {
  id: 'pota-activations',
  title: 'POTA Activations',
  icon: <TreePine size={16} />,
  order: 30,
  Content: PotaActivationsHelpContent,
};

registerHelp(potaActivationsHelp);

/**
 * SOTA Activations Help Content
 */
function SotaActivationsHelpContent() {
  return (
    <div className="help-content">
      <p>
        Summits on the Air (SOTA) activations currently live. Operators hike to mountain summits and
        make contacts while chasers work them from home or other locations.
      </p>
      <p>
        <strong>Hot when:</strong> Active summits on bands with good propagation. Recent spots rank
        higher since SOTA activations are typically brief.
      </p>
    </div>
  );
}

// Register SOTA help definition
const sotaActivationsHelp: HelpDefinition = {
  id: 'sota-activations',
  title: 'SOTA Activations',
  icon: <Mountain size={16} />,
  order: 35,
  Content: SotaActivationsHelpContent,
};

registerHelp(sotaActivationsHelp);

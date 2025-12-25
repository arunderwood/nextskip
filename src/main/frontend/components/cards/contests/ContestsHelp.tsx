/**
 * Contests Help Content - Help registration for Contest calendar.
 */

import React from 'react';
import { Trophy } from 'lucide-react';
import { registerHelp } from '../../help/HelpRegistry';
import type { HelpDefinition } from '../../help/types';

/**
 * Contests Help Content
 */
function ContestsHelpContent() {
  return (
    <div className="help-content">
      <p>
        Upcoming and active amateur radio contests. Contests are timed events where operators exchange brief information
        and try to make as many contacts as possible.
      </p>
      <p>
        <strong>Hot when:</strong> Major contests are active, especially international events that pack the bands with
        stations.
      </p>
    </div>
  );
}

// Register help definition
const contestsHelp: HelpDefinition = {
  id: 'contests',
  title: 'Contests',
  icon: <Trophy size={16} />,
  order: 40,
  Content: ContestsHelpContent,
};

registerHelp(contestsHelp);

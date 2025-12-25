/**
 * PotaActivationsContent - Content component for POTA activations card
 *
 * Thin wrapper around ActivationsContent for POTA-specific configuration.
 */

import React from 'react';
import type Activation from 'Frontend/generated/io/nextskip/activations/model/Activation';
import ActivationsContent from './ActivationsContent';

interface Props {
  activations: Activation[];
}

function PotaActivationsContent({ activations }: Props) {
  return <ActivationsContent activations={activations} type="pota" emptyMessage="No current POTA activations" />;
}

export default PotaActivationsContent;

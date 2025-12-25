/**
 * SotaActivationsContent - Content component for SOTA activations card
 *
 * Thin wrapper around ActivationsContent for SOTA-specific configuration.
 */

import React from 'react';
import type Activation from 'Frontend/generated/io/nextskip/activations/model/Activation';
import ActivationsContent from './ActivationsContent';

interface Props {
  activations: Activation[];
}

function SotaActivationsContent({ activations }: Props) {
  return <ActivationsContent activations={activations} type="sota" emptyMessage="No current SOTA activations" />;
}

export default SotaActivationsContent;

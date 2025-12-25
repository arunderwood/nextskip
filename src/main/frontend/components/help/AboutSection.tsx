/**
 * AboutSection - Static content explaining NextSkip.
 *
 * This is always the first section in the help modal.
 */

import React from 'react';
import './HelpSection.css';

export function AboutSection() {
  return (
    <section
      id="help-section-about"
      data-section-id="about"
      className="help-section"
      aria-labelledby="help-section-about-title"
    >
      <h3 id="help-section-about-title" className="help-section__title">
        About NextSkip
      </h3>

      <div className="help-section__content">
        <p>
          <strong>NextSkip</strong> is a real-time HF propagation dashboard designed for amateur radio operators. It
          aggregates data from multiple trusted sources to help you make the most of current band conditions.
        </p>

        <h4>Why &ldquo;NextSkip&rdquo;?</h4>
        <p>
          In amateur radio, &ldquo;skip&rdquo; refers to radio waves that bounce off the ionosphere, allowing
          long-distance communication that would otherwise be impossible. NextSkip helps you find your next great skip
          opening&mdash;whether that&apos;s working DX on 10 meters or catching a POTA activation on the other side of
          the country.
        </p>

        <h4>Understanding the Dashboard</h4>
        <p>
          Activity cards are automatically sorted by &ldquo;hotness&rdquo;&mdash;how favorable conditions are right now.
          Cards with better conditions appear first and glow to catch your attention.
        </p>

        <ul>
          <li>
            <strong>Excellent</strong> (green glow) &mdash; Best conditions, act now!
          </li>
          <li>
            <strong>Good</strong> (orange tint) &mdash; Favorable conditions
          </li>
          <li>
            <strong>Moderate</strong> (blue tint) &mdash; Average conditions
          </li>
          <li>
            <strong>Limited</strong> (gray) &mdash; Conditions below normal
          </li>
        </ul>

        <h4>Data Sources</h4>
        <p>
          NextSkip aggregates data from NOAA Space Weather Prediction Center, HamQSL, POTA, and other trusted sources.
          Data refreshes every 5 minutes to keep you up to date without overwhelming the source servers.
        </p>

        <h4>Open Source</h4>
        <p>NextSkip is an open source project. Contributions, bug reports, and feature requests are welcome!</p>
      </div>
    </section>
  );
}

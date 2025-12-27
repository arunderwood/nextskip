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
          <strong>NextSkip</strong> shows you what&apos;s happening across amateur radio right now&mdash;propagation
          conditions, portable activations, contests, and more&mdash;so you can decide where to spend your time on the
          air.
        </p>

        <h4>Why &ldquo;NextSkip&rdquo;?</h4>
        <p>
          In amateur radio, &ldquo;skip&rdquo; means radio waves bouncing off the ionosphere for long-distance contacts.
          But many hams also &ldquo;skip&rdquo; between activities&mdash;chasing DX one day, hunting POTA the next,
          jumping into a contest on the weekend. NextSkip surfaces the best opportunities so you can find your next
          skip, whatever that means for you.
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
          NextSkip aggregates data from trusted amateur radio sources including NOAA, HamQSL, POTA, and more. Data
          refreshes automatically to keep you current.
        </p>

        <h4>Open Source</h4>
        <p>
          NextSkip is an open source project. Contributions, bug reports, and feature requests are welcome! Visit the{' '}
          <a
            href="https://github.com/arunderwood/nextskip"
            target="_blank"
            rel="noopener noreferrer"
            className="help-section__link"
          >
            GitHub repository
          </a>{' '}
          to get involved.
        </p>
      </div>
    </section>
  );
}

/**
 * ContestsContent - Content component for contests card
 *
 * Displays upcoming amateur radio contests with:
 * - Count of active contests and upcoming (24h) contests
 * - List of contests with name, timing, and status
 */

import React from 'react';
import type Contest from 'Frontend/generated/io/nextskip/contests/model/Contest';
import EventStatus from 'Frontend/generated/io/nextskip/common/model/EventStatus';
import { formatTimeRemaining } from 'Frontend/utils/formatTime';
import './ContestsContent.module.css';

interface Props {
  contests: Contest[];
  activeCount: number;
  upcomingCount: number;
}

function ContestsContent({ contests, activeCount, upcomingCount }: Props) {
  const formatDateTime = (timestamp: string | undefined): string => {
    if (!timestamp) return 'Unknown';

    const date = new Date(timestamp);
    const now = new Date();
    const isToday = date.toDateString() === now.toDateString();
    const isTomorrow = date.toDateString() === new Date(now.getTime() + 86400000).toDateString();

    const timeStr = date.toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
      timeZoneName: 'short',
    });

    if (isToday) return `Today ${timeStr}`;
    if (isTomorrow) return `Tomorrow ${timeStr}`;

    return date.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    });
  };

  const getStatusBadge = (contest: Contest): { label: string; className: string } => {
    if (contest.status === EventStatus.ACTIVE) {
      const remaining = formatTimeRemaining(contest.timeRemainingSeconds);
      return {
        label: contest.endingSoon ? `Ending in ${remaining}` : `Active (${remaining})`,
        className: contest.endingSoon ? 'status-ending-soon' : 'status-active',
      };
    }

    if (contest.status === EventStatus.UPCOMING) {
      const remaining = formatTimeRemaining(contest.timeRemainingSeconds);
      return {
        label: `Starts in ${remaining}`,
        className: 'status-upcoming',
      };
    }

    return { label: 'Ended', className: 'status-ended' };
  };

  // Show up to 6 contests
  const displayContests = contests.slice(0, 6);

  return (
    <div className="contests-content">
      <div className="contests-summary">
        <div className="summary-item">
          <div className="summary-number">{activeCount}</div>
          <div className="summary-label">active now</div>
        </div>
        <div className="summary-divider">|</div>
        <div className="summary-item">
          <div className="summary-number">{upcomingCount}</div>
          <div className="summary-label">starting soon</div>
        </div>
      </div>

      {contests.length === 0 ? (
        <div className="no-contests">
          <p>No contests in the next 8 days</p>
        </div>
      ) : (
        <ul className="contests-list">
          {displayContests.map((contest, index) => {
            const status = getStatusBadge(contest);

            /* eslint-disable react/no-array-index-key */
            return (
              <li key={`${contest.name}-${index}`} className="contest-item">
                <div className="contest-header">
                  <div className="contest-name">
                    {contest.calendarSourceUrl ? (
                      <a
                        href={contest.calendarSourceUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="contest-link"
                      >
                        {contest.name}
                      </a>
                    ) : (
                      <span>{contest.name}</span>
                    )}
                  </div>
                  <span className={`contest-status ${status.className}`}>{status.label}</span>
                </div>
                <div className="contest-timing">
                  <span className="start-time">
                    {contest.status === EventStatus.ACTIVE
                      ? `Ends ${formatDateTime(contest.endTime)}`
                      : `Starts ${formatDateTime(contest.startTime)}`}
                  </span>
                </div>
              </li>
            );
          })}
        </ul>
      )}

      {contests.length > 6 && <div className="more-contests">+{contests.length - 6} more contests</div>}
    </div>
  );
}

export default ContestsContent;

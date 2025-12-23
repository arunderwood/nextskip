/**
 * EventCard - Generic card component for any Event type
 *
 * Displays time-bound events (contests, meteor showers, field days, etc.)
 * following the Open-Closed Principle. New event types can be added without
 * modifying this component.
 *
 * SOLID Compliance:
 * - Liskov Substitution: All Event implementations are interchangeable
 * - Open-Closed: Extensible via eventType parameter and content mapping
 * - Dependency Inversion: Depends on Event abstraction, not concrete types
 */

import React from 'react';
import { Trophy, Sparkles, Tent } from 'lucide-react';
import type { ActivityCardConfig } from 'Frontend/types/activity';
import type Contest from 'Frontend/generated/io/nextskip/contests/model/Contest';
import EventStatus from 'Frontend/generated/io/nextskip/common/model/EventStatus';
import { formatTimeRemaining } from 'Frontend/utils/formatTime';
import { ActivityCard } from '../../activity';
import './EventCard.module.css';

/**
 * Event interface matching the backend Event abstraction.
 * Used for type safety until Hilla generates the Event interface.
 */
interface Event {
  name?: string;
  startTime?: string;
  endTime?: string;
  status?: typeof EventStatus[keyof typeof EventStatus];
  endingSoon?: boolean;
}

/**
 * Supported event types - extend as new event implementations are added
 */
export type EventType = 'contest' | 'meteor-shower' | 'field-day';

interface EventCardProps {
  event: Event;
  eventType: EventType;
  config: ActivityCardConfig;
}

/**
 * Get status badge configuration based on event status
 */
function getStatusBadge(event: Event): { label: string; className: string } {
  const timeRemaining = (event as any).timeRemainingSeconds;

  if (event.status === EventStatus.ACTIVE) {
    const remaining = formatTimeRemaining(timeRemaining);
    return {
      label: event.endingSoon ? `Ending in ${remaining}` : `Active (${remaining})`,
      className: event.endingSoon ? 'status-ending-soon' : 'status-active'
    };
  }

  if (event.status === EventStatus.UPCOMING) {
    const remaining = formatTimeRemaining(timeRemaining);
    return {
      label: `Starts in ${remaining}`,
      className: 'status-upcoming'
    };
  }

  return { label: 'Ended', className: 'status-ended' };
}

/**
 * Get event-type-specific metadata
 */
function getEventMetadata(eventType: EventType) {
  switch (eventType) {
    case 'contest':
      return {
        icon: <Trophy size={20} />,
        typeLabel: 'Contest',
        description: 'Amateur radio competition'
      };
    case 'meteor-shower':
      return {
        icon: <Sparkles size={20} />,
        typeLabel: 'Meteor Shower',
        description: 'Meteor scatter propagation opportunity'
      };
    case 'field-day':
      return {
        icon: <Tent size={20} />,
        typeLabel: 'Field Day',
        description: 'Special operating event'
      };
  }
}

/**
 * Render contest-specific details
 */
function ContestDetails({ event }: { event: Contest }) {
  return (
    <div className="event-details">
      {event.sponsor && (
        <div className="detail-row">
          <span className="detail-label">Sponsor:</span>
          <span className="detail-value">{event.sponsor}</span>
        </div>
      )}
      {event.modes && event.modes.length > 0 && (
        <div className="detail-row">
          <span className="detail-label">Modes:</span>
          <span className="detail-value">{Array.from(event.modes).join(', ')}</span>
        </div>
      )}
      {event.bands && event.bands.length > 0 && (
        <div className="detail-row">
          <span className="detail-label">Bands:</span>
          <span className="detail-value">
            {Array.from(event.bands).map((band: any) => band.name || band).join(', ')}
          </span>
        </div>
      )}
    </div>
  );
}

/**
 * Generic EventCard component
 */
export function EventCard({ event, eventType, config }: EventCardProps) {
  const metadata = getEventMetadata(eventType);
  const status = getStatusBadge(event);

  // Get the calendar source URL if available (contests have this)
  const sourceUrl = (event as any).calendarSourceUrl;

  return (
    <ActivityCard
      config={config}
      title={event.name || 'Unknown Event'}
      icon={metadata.icon}
      subtitle={metadata.typeLabel}
    >
      <div className="event-card-content">
        <div className="event-status">
          <span className={`status-badge ${status.className}`}>
            {status.label}
          </span>
        </div>

        {/* Render event-type-specific details */}
        {eventType === 'contest' && <ContestDetails event={event as Contest} />}

        {/* Link to more info if available */}
        {sourceUrl && (
          <div className="event-link">
            <a
              href={sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="details-link"
            >
              View Details →
            </a>
          </div>
        )}
      </div>
    </ActivityCard>
  );
}

export default EventCard;

# Component Patterns

This document defines the conventions and patterns for React components in NextSkip.

## File Structure

### Component Organization

```
frontend/
├── components/           # Reusable components
│   ├── ComponentName.tsx
│   ├── ComponentName.css
│   └── ComponentName.test.tsx
├── views/               # Page-level components (routes)
│   ├── DashboardView.tsx
│   ├── DashboardView.css
│   └── DashboardView.test.tsx
└── docs/                # Documentation (this file)
```

### Naming Conventions

- **Files**: PascalCase with `.tsx` extension (`SolarIndicesCard.tsx`)
- **CSS**: Same name as component (`SolarIndicesCard.css`)
- **Tests**: Same name with `.test.tsx` (`SolarIndicesCard.test.tsx`)
- **Components**: Function name matches filename
- **Props interface**: Named `Props` (local to file)

## Component Template

### Basic Component Structure

```typescript
import React from 'react';
import './ComponentName.css';

interface Props {
  required: string;
  optional?: number;
}

function ComponentName({ required, optional }: Props) {
  return (
    <div className="component-name">
      {/* Component content */}
    </div>
  );
}

export default ComponentName;
```

### Component with Hilla Integration

```typescript
import React, { useEffect, useState } from 'react';
import { EndpointName } from 'Frontend/generated/endpoints';
import type { ResponseType } from 'Frontend/generated/path/to/endpoint';
import './ComponentName.css';

interface Props {
  // Props if needed
}

function ComponentName({  }: Props) {
  const [data, setData] = useState<ResponseType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const response = await EndpointName.methodName();
        // Hilla returns T | undefined - handle explicitly
        if (response) {
          setData(response);
        }
      } catch (err) {
        console.error('Error fetching data:', err);
        setError('Failed to fetch data');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  if (loading) {
    return <div className="loading">Loading...</div>;
  }

  if (error) {
    return <div className="error">{error}</div>;
  }

  return (
    <div className="component-name">
      {/* Use data */}
    </div>
  );
}

export default ComponentName;
```

## Props Interface Conventions

### TypeScript Strictness

```typescript
// ✅ Good - explicit types, no any
interface Props {
  name: string;
  count: number;
  isActive?: boolean;
  onAction: () => void;
  data: SomeType | null;
}

// ❌ Bad - uses any
interface Props {
  data: any;  // Never use any!
}
```

### Optional Props

Use `?` for truly optional props, `| null` for nullable data:

```typescript
interface Props {
  // Optional - may not be provided at all
  optional?: string;

  // Nullable - always provided, but may be null
  nullable: string | null;

  // Both - may be missing or null
  optional?: string | null;
}
```

## State Management

### Local State (useState)

```typescript
// Simple state
const [count, setCount] = useState(0);

// State with type
const [data, setData] = useState<DataType | null>(null);

// State with initial value
const [loading, setLoading] = useState(true);
const [error, setError] = useState<string | null>(null);
```

### Effect Hooks (useEffect)

```typescript
// Data fetching
useEffect(() => {
  fetchData();
}, []); // Empty deps - run once on mount

// Interval/polling
useEffect(() => {
  const interval = setInterval(fetchData, 5 * 60 * 1000);
  return () => clearInterval(interval);  // Cleanup
}, []);

// Dependent effect
useEffect(() => {
  if (userId) {
    fetchUserData(userId);
  }
}, [userId]);  // Re-run when userId changes
```

## CSS Conventions

### Component-Scoped Classes

Use BEM-like naming with component prefix:

```css
/* ComponentName.css */

.component-name {
  /* Component root */
}

.component-name__element {
  /* Component child element */
}

.component-name__element--modifier {
  /* Element variant */
}
```

### Design Token Usage

```css
/* ✅ Good - uses design tokens */
.component-name {
  background: var(--surface-color);
  padding: calc(var(--spacing-unit) * 2);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
}

/* ❌ Bad - hardcoded values */
.component-name {
  background: #ffffff;
  padding: 16px;
  border-radius: 4px;
}
```

### Global Utility Classes

Use pre-defined classes from `global.css`:

```tsx
<div className="card">              {/* Standard card */}
<div className="card card-elevated"> {/* Elevated card */}
<div className="loading">            {/* Loading state */}
<div className="error">              {/* Error state */}
<span className="status-good">      {/* Green text */}
<span className="status-fair">      {/* Orange text */}
<span className="status-poor">      {/* Red text */}
```

## Error Handling Patterns

### Loading and Error States

```typescript
// Pattern used in DashboardView.tsx
if (loading && !data) {
  return <div className="loading">Loading...</div>;
}

if (error && !data) {
  return <div className="error">{error}</div>;
}

// Partial error (data exists, but error on refresh)
{error && (
  <div className="error-banner">{error}</div>
)}
```

### Null/Undefined Handling

```typescript
// ✅ Good - uses optional chaining and nullish coalescing
<div>{data?.name ?? 'N/A'}</div>

// ✅ Good - explicit null check
{data && <Component data={data} />}

// ❌ Bad - will crash if data is null
<div>{data.name}</div>
```

## Vaadin Hilla Integration

### Generated TypeScript Clients

```typescript
// Import endpoint
import { PropagationEndpoint } from 'Frontend/generated/endpoints';

// Import types
import type { PropagationResponse } from 'Frontend/generated/io/nextskip/propagation/api/PropagationEndpoint';
import type { SolarIndices } from 'Frontend/generated/io/nextskip/propagation/model/SolarIndices';
```

### Handling Undefined Returns

Hilla endpoints return `T | undefined`:

```typescript
const response = await EndpointName.methodName();

// ✅ Handle undefined explicitly
if (response) {
  setData(response);
} else {
  setError('No data returned');
}

// ✅ Or use nullish coalescing
const data = response ?? defaultValue;
```

## Component Composition

### Presentation vs Container Components

**Presentation Component** (receives data via props):
```typescript
// SolarIndicesCard.tsx - pure presentation
interface Props {
  solarIndices: SolarIndices;
}

function SolarIndicesCard({ solarIndices }: Props) {
  return <div>{/* Render solarIndices */}</div>;
}
```

**Container Component** (fetches data, manages state):
```typescript
// DashboardView.tsx - data fetching + composition
function DashboardView() {
  const [data, setData] = useState<PropagationResponse | null>(null);
  // ... fetch data logic ...

  return (
    <div>
      {data?.solarIndices && (
        <SolarIndicesCard solarIndices={data.solarIndices} />
      )}
    </div>
  );
}
```

## Accessibility Patterns

### Semantic HTML

```tsx
// ✅ Good - semantic elements
<button onClick={handleClick}>Action</button>
<nav>...</nav>
<main>...</main>

// ❌ Bad - div soup
<div onClick={handleClick}>Action</div>
```

### ARIA Attributes

```tsx
// Button with accessible label
<button
  onClick={handleRefresh}
  disabled={loading}
  title="Refresh data"
  aria-label="Refresh propagation data"
>
  Refresh
</button>

// Status messages
<div role="status" aria-live="polite">
  Last updated: {lastUpdate.toLocaleTimeString()}
</div>
```

## Testing Patterns

### Component Test Template

```typescript
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect } from 'vitest';
import ComponentName from './ComponentName';

describe('ComponentName', () => {
  it('renders with required props', () => {
    render(<ComponentName required="value" />);
    expect(screen.getByText('value')).toBeInTheDocument();
  });

  it('handles user interaction', async () => {
    const user = userEvent.setup();
    const mockFn = vi.fn();

    render(<ComponentName onAction={mockFn} />);

    await user.click(screen.getByRole('button'));
    expect(mockFn).toHaveBeenCalled();
  });
});
```

## Performance Patterns

### Memoization

```typescript
// Expensive computation
const computedValue = useMemo(() => {
  return expensiveCalculation(data);
}, [data]);

// Stable callback
const handleAction = useCallback(() => {
  doSomething(data);
}, [data]);

// Pure component
const MemoizedComponent = React.memo(Component);
```

## Common Patterns

### Auto-Refresh Pattern

```typescript
useEffect(() => {
  // Initial fetch
  fetchData();

  // Auto-refresh every 5 minutes
  const interval = setInterval(fetchData, 5 * 60 * 1000);

  // Cleanup on unmount
  return () => clearInterval(interval);
}, []);
```

### Refresh Button Pattern

```typescript
const handleRefresh = () => {
  setLoading(true);
  fetchData();
};

return (
  <button
    onClick={handleRefresh}
    disabled={loading}
    className="refresh-button"
  >
    {loading ? '⟳' : '↻'} Refresh
  </button>
);
```

## Bento Grid Patterns

### Creating a Bento Card

Bento cards use a wrapper + content pattern:

```typescript
import { BentoCard, usePriorityCalculation } from '../components/bento';
import type { BentoCardConfig } from '../types/bento';

// 1. Calculate priority from data
const { priority, hotness } = usePriorityCalculation({
  favorable: data.favorable,
  score: data.score,
  rating: data.rating,
});

// 2. Create card configuration
const config: BentoCardConfig = {
  id: 'unique-id',
  type: 'activity-type',
  size: 'standard', // or 'wide', 'tall', 'hero'
  priority,
  hotness,
};

// 3. Render with BentoCard wrapper
<BentoCard
  config={config}
  title="Card Title"
  icon="📊"
  subtitle="Optional subtitle"
  footer={<>Optional footer content</>}
>
  <YourContentComponent data={data} />
</BentoCard>
```

### Card Size Selection Guide

Choose card size based on content needs:

- **standard (1x1)**: Simple metrics (single number, status indicator)
- **wide (2x1)**: Tables, charts, horizontal lists
- **tall (1x2)**: Vertical lists, activity feeds
- **hero (2x2)**: Rich dashboards, featured metrics with charts

### Content Component Pattern

Content components display data without card wrapper:

```typescript
// components/cards/MyActivityContent.tsx
import React from 'react';

interface Props {
  data: MyActivityData;
}

function MyActivityContent({ data }: Props) {
  return (
    <div className="my-activity-content">
      {/* Content only - no card wrapper */}
    </div>
  );
}

export default MyActivityContent;
```

**Key principle**: Content components are **pure presentation** - BentoCard handles the wrapper, header, footer, and styling.

### useDashboardCards Hook Pattern

Orchestrate card configurations in a custom hook:

```typescript
// hooks/useDashboardCards.ts
import { useMemo } from 'react';
import { usePriorityCalculation } from '../components/bento';
import type { BentoCardConfig } from '../types/bento';

export function useDashboardCards(data: DashboardData | null): BentoCardConfig[] {
  // Create config for each activity
  const activityConfig = useMemo(() => {
    if (!data?.activity) return null;

    const { priority, hotness } = usePriorityCalculation({
      favorable: data.activity.favorable,
      score: data.activity.score,
      rating: data.activity.rating,
    });

    return {
      id: 'activity',
      type: 'activity-type',
      size: 'standard',
      priority,
      hotness,
    };
  }, [data?.activity]);

  // Return array of configs (filter out null)
  return useMemo(() => {
    return [activityConfig /* , otherConfigs... */].filter(
      (config): config is BentoCardConfig => config !== null
    );
  }, [activityConfig]);
}
```

### BentoGrid Usage

```typescript
import { BentoGrid } from '../components/bento';

function DashboardView() {
  const cardConfigs = useDashboardCards(data);

  const bentoCards = cardConfigs.map((config) => ({
    config,
    component: <BentoCard config={config} {...cardProps}>
      <ContentComponent />
    </BentoCard>
  }));

  return <BentoGrid cards={bentoCards} />;
}
```

### Hotness Visual Indicators

Cards automatically display hotness via:
- **Border glow**: Hot cards have green glow, warm have orange tint
- **Header badge**: Shows "Excellent" / "Good" / "Moderate" / "Limited"
- **Pulse animation**: Hot cards have pulsing indicator dot

No manual styling needed - it's automatic based on priority score.

### Priority Calculation Guidelines

**Favorable flag** (40% weight):
- Use for binary favorable/unfavorable conditions
- Example: `solarIndices.favorable`

**Score** (35% weight):
- Numeric value 0-100
- Example: `bandConditions.score`, `solarFluxIndex`

**Rating** (20% weight):
- GOOD/FAIR/POOR/UNKNOWN enum
- Example: `bandCondition.rating`

**Recency** (5% weight):
- Optional timestamp for time-sensitive data
- Decays from full weight at 0 minutes to 0 weight at 60 minutes

### Future Activities

Adding new activity cards:

1. Create content component in `components/cards/`
2. Add type to `ActivityType` in `types/bento.ts`
3. Add case in `useDashboardCards` hook
4. Add switch case in `DashboardView` to render

Example:
```typescript
case 'satellite-passes':
  component = (
    <BentoCard config={config} title="Satellite Passes" icon="🛰️">
      <SatellitePassesContent passes={data.satellitePasses} />
    </BentoCard>
  );
  break;
```

## Do's and Don'ts

### Do's ✅
- Export default for single component per file
- Use TypeScript strict mode (no `any`)
- Handle loading and error states
- Use design tokens for all styling
- Component-scope CSS classes
- Handle Hilla `undefined` returns explicitly
- Clean up effects (intervals, subscriptions)
- Use semantic HTML elements

### Don'ts ❌
- Export multiple components from one file
- Use `any` type
- Hardcode colors or spacing values
- Use global CSS selectors
- Ignore loading/error states
- Forget to clean up effects
- Use `<div>` for clickable elements (use `<button>`)
- Skip prop validation

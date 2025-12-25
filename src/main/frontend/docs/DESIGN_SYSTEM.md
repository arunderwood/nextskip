# NextSkip Design System

This document defines the design tokens, spacing system, and visual style for the NextSkip frontend.

## Color Tokens

### Brand Colors

```css
--primary-color: #1976d2; /* Blue - primary actions, links */
--secondary-color: #dc004e; /* Pink - accents, highlights */
```

### Semantic Colors

```css
--success-color: #4caf50; /* Green - success states, "good" conditions */
--warning-color: #ff9800; /* Orange - warnings, "fair" conditions */
--error-color: #f44336; /* Red - errors, "poor" conditions */
```

### Neutral Colors

```css
--background-color: #f5f5f5; /* Light gray - app background */
--surface-color: #ffffff; /* White - card/component backgrounds */
--border-color: #e0e0e0; /* Medium gray - borders, dividers */
```

### Text Colors

```css
--text-primary: rgba(0, 0, 0, 0.87); /* Primary text - high emphasis */
--text-secondary: rgba(0, 0, 0, 0.6); /* Secondary text - medium emphasis */
```

**Accessibility**: All text colors meet WCAG 2.1 AA contrast ratios (4.5:1 minimum) when used on their intended backgrounds.

## Spacing System

**Base Unit**: 8px

All spacing should use multiples of the base unit:

```css
--spacing-unit: 8px;
```

### Common Spacing Values

- **0.5x** (4px): Tight spacing, icon padding
- **1x** (8px): Default spacing between related elements
- **1.5x** (12px): Moderate spacing
- **2x** (16px): Standard padding, margins between sections
- **3x** (24px): Large spacing between major sections
- **4x** (32px): Extra large spacing, page margins

### Usage in CSS

```css
padding: calc(var(--spacing-unit) * 2); /* 16px */
margin-bottom: calc(var(--spacing-unit) * 3); /* 24px */
gap: var(--spacing-unit); /* 8px */
```

## Typography

### Font Sizes

```css
h1 {
  font-size: 2rem;
} /* 32px - page title */
h2 {
  font-size: 1.5rem;
} /* 24px - section heading */
h3 {
  font-size: 1.25rem;
} /* 20px - subsection heading */
body {
  font-size: 1rem;
} /* 16px - base text */
```

### Font Weights

```css
h1,
h2,
h3,
h4,
h5,
h6 {
  font-weight: 500;
} /* Medium - headings */
body {
  font-weight: 400;
} /* Normal - body text */
```

### Line Heights

- **Headings**: Use browser default (typically 1.2)
- **Body text**: Use browser default (typically 1.5)

## Shadows

### Standard Shadow

```css
--shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
```

**Usage**: Default card shadow, subtle elevation

### Elevated Shadow

```css
--shadow-elevated: 0 4px 8px rgba(0, 0, 0, 0.15);
```

**Usage**: Hover states, important cards, modals

## Border Radius

```css
--border-radius: 4px;
```

**Usage**: Cards, buttons, inputs - creates subtle rounded corners

## Responsive Breakpoints

```css
/* Mobile first - no breakpoint needed */
@media (min-width: 768px) {
  /* Tablet and above */
}

@media (min-width: 1024px) {
  /* Desktop and above */
}
```

**Current Implementation**: Limited responsive design. Expand as needed.

## Usage Guidelines

### Do's ✅

- **Always use CSS custom properties** (design tokens) for colors, spacing, shadows
- **Use spacing multipliers** (`calc(var(--spacing-unit) * N)`) for consistency
- **Follow semantic color meanings** (success = green, error = red, etc.)
- **Verify color contrast** using browser DevTools or WebAIM checker

### Don'ts ❌

- **Never hardcode color values** (e.g., `color: #1976d2`)
- **Never use arbitrary spacing** (e.g., `margin: 13px`)
- **Never override semantic colors** (e.g., don't use error color for success)
- **Never skip accessibility checks** for new color combinations

## Status Classes

Pre-defined utility classes for band condition ratings:

```css
.status-good      /* Green text (--success-color) */
.status-fair      /* Orange text (--warning-color) */
.status-poor      /* Red text (--error-color) */
.status-unknown   /* Gray text (--text-secondary) */
```

## Component Patterns

### Cards

```css
.card {
  background: var(--surface-color);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
  padding: calc(var(--spacing-unit) * 2);
  margin-bottom: calc(var(--spacing-unit) * 2);
}

.card-elevated {
  box-shadow: var(--shadow-elevated);
}
```

### Loading States

```css
.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
  color: var(--text-secondary);
}
```

### Error States

```css
.error {
  color: var(--error-color);
  padding: calc(var(--spacing-unit) * 2);
  background: rgba(244, 67, 54, 0.1); /* 10% opacity error background */
  border-radius: var(--border-radius);
  border-left: 4px solid var(--error-color);
}
```

## Activity Grid System

The activity grid is a layout system where cards are arranged by "hotness" - how favorable conditions are for each activity at the moment.

### Grid Configuration

```css
--activity-columns-desktop: 4; /* 4 columns on desktop */
--activity-columns-tablet: 2; /* 2 columns on tablet */
--activity-columns-mobile: 1; /* 1 column on mobile */
--activity-gap: calc(var(--spacing-unit) * 3); /* 24px gap */
--activity-card-min-height: 200px;
```

### Card Sizes

Cards can span multiple grid cells:

| Size               | Grid Span          | Use Case                       |
| ------------------ | ------------------ | ------------------------------ |
| **standard** (1x1) | 1 column × 1 row   | Single metrics, utilities      |
| **wide** (2x1)     | 2 columns × 1 row  | Charts, tables                 |
| **tall** (1x2)     | 1 column × 2 rows  | Lists, activity feeds          |
| **hero** (2x2)     | 2 columns × 2 rows | Primary KPIs, featured metrics |

### Hotness Levels

Cards automatically display visual indicators based on condition quality:

```css
/* Hot (70-100 priority): Green glow, "Excellent" badge */
--hotness-hot-border: rgba(76, 175, 80, 0.6);
--hotness-hot-glow: rgba(76, 175, 80, 0.3);

/* Warm (45-69 priority): Orange tint, "Good" badge */
--hotness-warm-border: rgba(255, 152, 0, 0.5);
--hotness-warm-glow: rgba(255, 152, 0, 0.25);

/* Neutral (20-44 priority): Blue tint, "Moderate" badge */
--hotness-neutral-border: rgba(25, 118, 210, 0.3);
--hotness-neutral-glow: rgba(25, 118, 210, 0.15);

/* Cool (0-19 priority): Gray, "Limited" badge */
--hotness-cool-border: var(--border-color);
--hotness-cool-glow: rgba(0, 0, 0, 0.05);
```

### Priority Calculation

Priority is calculated from data conditions (0-100 scale):

- **Favorable flag**: 40% weight
- **Numeric score**: 35% weight
- **Rating (GOOD/FAIR/POOR)**: 20% weight
- **Recency**: 5% weight

Higher priority cards appear first in the grid (top-left position).

### Animation

```css
--activity-transition-duration: 300ms;
--activity-transition-timing: cubic-bezier(0.4, 0, 0.2, 1); /* Material ease-out */
```

Cards smoothly transition when:

- Priority changes (reordering)
- Hovering (lift effect)
- Focusing (accessibility ring)

### Usage Example

```tsx
import { ActivityGrid, ActivityCard } from '../components/activity';

<ActivityGrid
  cards={[
    {
      config: { id: 'solar', type: 'solar-indices', size: 'standard', priority: 85, hotness: 'hot' },
      component: (
        <ActivityCard config={config} title="Solar Indices" icon="☀️">
          <SolarIndicesContent data={solarData} />
        </ActivityCard>
      ),
    },
  ]}
/>;
```

### Responsive Behavior

- **Desktop (>1024px)**: 4-column grid
- **Tablet (768-1024px)**: 2-column grid, wide/hero cards span full width
- **Mobile (<768px)**: Single column, all cards full width

## Extending the Design System

When adding new design tokens:

1. **Define in `:root`** in `frontend/styles/global.css`
2. **Document here** with usage guidelines
3. **Update related components** to use new token
4. **Verify accessibility** if adding colors

## References

- **WCAG 2.1 AA Contrast**: 4.5:1 for normal text, 3:1 for large text
- **WebAIM Contrast Checker**: https://webaim.org/resources/contrastchecker/
- **Material Design Color System**: Inspiration for semantic colors

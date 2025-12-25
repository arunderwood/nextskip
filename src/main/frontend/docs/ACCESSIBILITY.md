# Accessibility Guidelines

This document defines the accessibility standards and testing procedures for NextSkip components.

## WCAG 2.1 AA Compliance

All components must meet **WCAG 2.1 Level AA** standards. This is non-negotiable.

## Color Contrast Requirements

### Text Contrast (WCAG 2.1 Success Criterion 1.4.3)

- **Normal text** (< 18pt or < 14pt bold): Minimum 4.5:1 contrast ratio
- **Large text** (≥ 18pt or ≥ 14pt bold): Minimum 3:1 contrast ratio
- **UI components** (icons, borders): Minimum 3:1 contrast ratio

### Current Token Compliance

All design tokens in `DESIGN_SYSTEM.md` meet AA contrast requirements when used on their intended backgrounds:

| Token              | Background           | Contrast | Status        |
| ------------------ | -------------------- | -------- | ------------- |
| `--text-primary`   | `--background-color` | 13.4:1   | ✅ AAA        |
| `--text-primary`   | `--surface-color`    | 14.6:1   | ✅ AAA        |
| `--text-secondary` | `--background-color` | 7.5:1    | ✅ AAA        |
| `--text-secondary` | `--surface-color`    | 8.3:1    | ✅ AAA        |
| `--success-color`  | `--surface-color`    | 3.4:1    | ✅ AA Large   |
| `--warning-color`  | `--surface-color`    | 2.0:1    | ⚠️ Fails AA\* |
| `--error-color`    | `--surface-color`    | 4.0:1    | ⚠️ Borderline |

**Note**: `--warning-color` (#ff9800) should only be used for large text or with background modifications.

### Testing Contrast

**Browser DevTools**:

1. Inspect element
2. Click color swatch in Styles panel
3. Check contrast ratio in color picker

**WebAIM Contrast Checker**:
https://webaim.org/resources/contrastchecker/

**Command Line** (with npm package):

```bash
npm install -g wcag-contrast
wcag-contrast "#ff9800" "#ffffff"  # Returns contrast ratio
```

## Keyboard Navigation

### Requirements (WCAG 2.1 Success Criterion 2.1.1)

- **All interactive elements** must be keyboard accessible
- **Tab order** must be logical and match visual layout
- **Focus indicators** must be clearly visible
- **No keyboard traps** - user can always navigate away

### Interactive Elements

```tsx
// ✅ Good - native button (keyboard accessible by default)
<button onClick={handleClick}>Action</button>

// ✅ Good - link (keyboard accessible by default)
<a href="/dashboard">Dashboard</a>

// ❌ Bad - div is not keyboard accessible
<div onClick={handleClick}>Action</div>

// ⚠️ Acceptable - div with keyboard handlers
<div
  role="button"
  tabIndex={0}
  onClick={handleClick}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      handleClick();
    }
  }}
>
  Action
</div>
```

### Focus Indicators

```css
/* ✅ Good - visible focus indicator */
button:focus {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}

/* ❌ Bad - removes focus indicator */
button:focus {
  outline: none; /* Never do this! */
}
```

### Tab Order

Use native HTML elements in logical order. Only use `tabIndex` when absolutely necessary:

- `tabIndex="0"` - Element is focusable in normal tab order
- `tabIndex="-1"` - Element is focusable programmatically, but not via Tab key
- `tabIndex="1+"` - ❌ Avoid - creates custom tab order (confusing for users)

## ARIA (Accessible Rich Internet Applications)

### When to Use ARIA

**First Rule of ARIA**: Don't use ARIA if a native HTML element works.

```tsx
// ✅ Good - native button (no ARIA needed)
<button onClick={handleClick}>Submit</button>

// ❌ Bad - unnecessary ARIA
<button role="button" onClick={handleClick}>Submit</button>

// ✅ Good - ARIA adds clarity for div button
<div role="button" tabIndex={0} onClick={handleClick}>
  Action
</div>
```

### Common ARIA Patterns

#### Buttons

```tsx
// Icon-only button needs label
<button
  onClick={handleRefresh}
  aria-label="Refresh propagation data"
  title="Refresh data"
>
  ↻
</button>

// Button with visible text (no aria-label needed)
<button onClick={handleSave}>
  Save Changes
</button>
```

#### Live Regions

```tsx
// Status updates that screen readers should announce
<div role="status" aria-live="polite">
  Last updated: {lastUpdate.toLocaleTimeString()}
</div>

// Error messages (assertive for immediate attention)
<div role="alert" aria-live="assertive">
  {error}
</div>
```

#### Loading States

```tsx
// Loading spinner
<div className="loading" role="status" aria-label="Loading propagation data">
  <div className="spinner" aria-hidden="true"></div>
  <p>Loading...</p>
</div>
```

#### Disabled State

```tsx
<button onClick={handleSubmit} disabled={loading} aria-disabled={loading}>
  {loading ? 'Submitting...' : 'Submit'}
</button>
```

### ARIA Landmarks

```tsx
<nav aria-label="Main navigation">...</nav>
<main>...</main>
<aside aria-label="Solar indices">...</aside>
<footer>...</footer>
```

## Semantic HTML

### Use Native Elements

```tsx
// ✅ Good - semantic HTML
<header>
  <h1>NextSkip Propagation Dashboard</h1>
  <nav>...</nav>
</header>
<main>
  <section>
    <h2>Current Conditions</h2>
    ...
  </section>
</main>

// ❌ Bad - div soup
<div class="header">
  <div class="title">NextSkip Propagation Dashboard</div>
  <div class="nav">...</div>
</div>
```

### Heading Hierarchy

```tsx
// ✅ Good - logical heading structure
<h1>NextSkip Dashboard</h1>
  <h2>Solar Indices</h2>
    <h3>Solar Flux Index</h3>
  <h2>Band Conditions</h2>

// ❌ Bad - skips heading levels
<h1>NextSkip Dashboard</h1>
  <h3>Solar Indices</h3>  {/* Skipped h2! */}
```

## Form Accessibility

### Labels

```tsx
// ✅ Good - explicit label association
<label htmlFor="callsign">Callsign:</label>
<input id="callsign" type="text" />

// ✅ Good - implicit label association
<label>
  Callsign:
  <input type="text" />
</label>

// ❌ Bad - no label
<input type="text" placeholder="Callsign" />
```

### Required Fields

```tsx
<label htmlFor="email">
  Email <span aria-label="required">*</span>
</label>
<input
  id="email"
  type="email"
  required
  aria-required="true"
/>
```

### Error Messages

```tsx
<label htmlFor="grid-square">Grid Square:</label>
<input
  id="grid-square"
  type="text"
  aria-invalid={hasError}
  aria-describedby={hasError ? 'grid-error' : undefined}
/>
{hasError && (
  <div id="grid-error" className="error" role="alert">
    Please enter a valid grid square (e.g., CN87).
  </div>
)}
```

## Images and Icons

### Alt Text

```tsx
// ✅ Good - descriptive alt text
<img src="logo.png" alt="NextSkip logo" />

// ✅ Good - decorative image (empty alt)
<img src="decorative.png" alt="" />

// ❌ Bad - missing alt
<img src="logo.png" />
```

### Icon-Only Elements

```tsx
// ✅ Good - icon with accessible label
<button aria-label="Refresh data">
  <span aria-hidden="true">↻</span>
</button>

// ✅ Good - icon with visible text
<button>
  <span aria-hidden="true">↻</span> Refresh
</button>
```

## Testing Procedures

### Automated Testing (jest-axe)

**Install**:

```bash
npm install --save-dev jest-axe
```

**Test Example**:

```typescript
import { axe, toHaveNoViolations } from 'jest-axe';

expect.extend(toHaveNoViolations);

it('has no accessibility violations', async () => {
  const { container } = render(<ComponentName />);
  const results = await axe(container);
  expect(results).toHaveNoViolations();
});
```

### Manual Testing Checklist

#### Keyboard Navigation

- [ ] Tab through all interactive elements
- [ ] Verify tab order is logical
- [ ] Ensure focus indicators are visible
- [ ] Test Enter and Space on buttons
- [ ] Test Escape to close modals/dialogs
- [ ] Verify no keyboard traps

#### Screen Reader (macOS VoiceOver)

- [ ] Enable VoiceOver (Cmd + F5)
- [ ] Navigate page with VO keys (Ctrl + Option + arrow keys)
- [ ] Verify all text is announced
- [ ] Verify ARIA labels are read correctly
- [ ] Verify interactive elements announce role
- [ ] Test forms (field labels, error messages)

#### Visual

- [ ] Check color contrast with DevTools
- [ ] Verify text is readable at 200% zoom
- [ ] Test with dark mode (if supported)
- [ ] Verify focus indicators are visible
- [ ] Check touch targets are ≥44×44px (mobile)

#### Testing Tools

- **Chrome DevTools Lighthouse**: Automated accessibility audit
- **axe DevTools Extension**: Real-time accessibility checks
- **WAVE**: Web accessibility evaluation tool
- **NVDA/JAWS**: Windows screen readers
- **VoiceOver**: macOS/iOS screen reader

## Common Issues and Fixes

### Issue: Low Contrast Text

**Problem**: Warning color (#ff9800) on white has 2.0:1 contrast.

**Fix**: Use warning color only for large text or add background:

```css
.status-fair {
  color: var(--warning-color);
  font-size: 1.25rem; /* Make text larger */
  font-weight: 500;
}

/* Or add background for better contrast */
.warning-badge {
  background: var(--warning-color);
  color: #000; /* Black text on orange: 5.9:1 */
}
```

### Issue: Missing Focus Indicator

**Problem**: User can't see which element has focus.

**Fix**: Add visible focus styles:

```css
button:focus,
a:focus,
input:focus {
  outline: 2px solid var(--primary-color);
  outline-offset: 2px;
}
```

### Issue: Div Acting as Button

**Problem**: `<div onClick={...}>` not keyboard accessible.

**Fix 1**: Use native button:

```tsx
<button onClick={handleClick}>Action</button>
```

**Fix 2**: Add keyboard support to div:

```tsx
<div
  role="button"
  tabIndex={0}
  onClick={handleClick}
  onKeyDown={(e) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      handleClick();
    }
  }}
>
  Action
</div>
```

### Issue: Icon-Only Button

**Problem**: Screen reader announces "Button" with no context.

**Fix**: Add `aria-label`:

```tsx
<button aria-label="Refresh propagation data" onClick={handleRefresh}>
  ↻
</button>
```

## Resources

### WCAG 2.1 Quick Reference

https://www.w3.org/WAI/WCAG21/quickref/

### MDN Accessibility Guide

https://developer.mozilla.org/en-US/docs/Web/Accessibility

### WebAIM Resources

- **Contrast Checker**: https://webaim.org/resources/contrastchecker/
- **WAVE Tool**: https://wave.webaim.org/
- **Screen Reader Guide**: https://webaim.org/articles/voiceover/

### ARIA Authoring Practices

https://www.w3.org/WAI/ARIA/apg/

### Testing Tools

- **axe DevTools**: Browser extension for accessibility testing
- **Lighthouse**: Built into Chrome DevTools
- **jest-axe**: Automated accessibility testing for React components

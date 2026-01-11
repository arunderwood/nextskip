# Quickstart: Admin Feed Manager

**Feature**: 001-admin-feed-manager
**Date**: 2026-01-09

## Prerequisites

1. **GitHub OAuth App**: Create OAuth app in GitHub settings
2. **Admin Email**: Add your GitHub email to the allowlist
3. **Running NextSkip**: Application must be running (`./gradlew bootRun`)

---

## Configuration

### 1. Create GitHub OAuth App

1. Go to [GitHub Developer Settings](https://github.com/settings/developers)
2. Click "OAuth Apps" → "New OAuth App"
3. Fill in:
   - **Application name**: `NextSkip Admin (Dev)`
   - **Homepage URL**: `http://localhost:8080`
   - **Authorization callback URL**: `http://localhost:8080/login/oauth2/code/github`
4. Click "Register application"
5. Copy the **Client ID**
6. Click "Generate a new client secret" and copy it

### 2. Add OAuth2 Credentials

Create or update `src/main/resources/application-local.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          github:
            client-id: ${GITHUB_CLIENT_ID}
            client-secret: ${GITHUB_CLIENT_SECRET}
            scope: user:email
```

Set environment variables:
```bash
export GITHUB_CLIENT_ID="your-client-id"
export GITHUB_CLIENT_SECRET="your-client-secret"
```

### 3. Configure Admin Allowlist

Add to `application.yml` (or use environment variable):

```yaml
nextskip:
  admin:
    allowed-emails:
      - your-github-email@example.com
```

Or via environment variable:
```bash
export NEXTSKIP_ADMIN_ALLOWED_EMAILS="your-github-email@example.com"
```

**Note**: Use the email associated with your GitHub account. If your GitHub email is private, you may need to make it public or use the GitHub API to fetch private emails.

---

## Running the Feature

### Start Application

```bash
./gradlew bootRun
```

### Access Admin Area

1. Navigate to `http://localhost:8080/admin`
2. You'll be redirected to GitHub login
3. Authorize the "NextSkip Admin" OAuth app
4. After authentication, you'll see the Admin Landing Page
5. Click "Feed Manager" to view feed statuses

---

## Testing the Feature

### Backend Tests

```bash
# Run all admin module tests
./gradlew test --tests "io.nextskip.admin.*"

# Run specific test class
./gradlew test --tests "AdminFeedEndpointTest"
```

### Frontend Tests

```bash
# Run admin component tests
npm test -- --grep "admin"

# Run with coverage
npm run test:coverage -- --grep "admin"
```

### E2E Tests

```bash
# Start app first
./gradlew bootRun &

# Run E2E tests
npm run e2e -- --grep "admin"
```

---

## API Quick Reference

### Check Feed Statuses (via Browser Console)

```javascript
// Requires being logged in as admin
const { getFeedStatuses } = await import('/frontend/generated/AdminFeedEndpoint');
const statuses = await getFeedStatuses();
console.log(statuses);
```

### Trigger Feed Refresh

```javascript
const { triggerFeedRefresh } = await import('/frontend/generated/AdminFeedEndpoint');
const result = await triggerFeedRefresh('pota-refresh');
console.log(result);
```

---

## Troubleshooting

### "Access Denied" After Login

Your email is not in the allowlist. Check:
```bash
echo $NEXTSKIP_ADMIN_ALLOWED_EMAILS
# Or check application.yml nextskip.admin.allowed-emails
```

### OAuth2 Redirect Error

1. Verify OAuth2 credentials are set correctly (`GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`)
2. Check that callback URL is configured in GitHub OAuth App settings:
   - `http://localhost:8080/login/oauth2/code/github`
3. Ensure `scope: user:email` is set in config (required to read email)

### Feed Status Not Loading

1. Check browser console for errors
2. Verify you have ADMIN role (check network tab for 403 errors)
3. Ensure db-scheduler is running (check `scheduled_tasks` table)

### Cannot Trigger Refresh

1. Check if feed is already refreshing (`isCurrentlyRefreshing: true`)
2. Verify feed name matches exactly (case-sensitive)
3. Check application logs for scheduler errors

---

## Key Files

| File | Purpose |
|------|---------|
| `SecurityConfig.java` | OAuth2 + route protection |
| `AdminFeedEndpoint.java` | Feed status & refresh API |
| `AdminAuthEndpoint.java` | User info & logout |
| `FeedStatusService.java` | Aggregates scheduled + subscription status |
| `AdminLayout.tsx` | Auth wrapper + navigation |
| `FeedManagerView.tsx` | Feed list UI |

---

## Next Steps After Implementation

1. Run full validation: `npm run validate && ./gradlew build`
2. Start app and test manually: `./gradlew bootRun`
3. Run E2E tests: `npm run e2e`
4. Create PR for review

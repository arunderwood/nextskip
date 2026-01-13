import { configureAuth } from '@vaadin/hilla-react-auth';
import { UserInfoService } from './generated/endpoints';

/**
 * Configure Hilla authentication for admin access.
 *
 * Uses the UserInfoService endpoint to get the current authenticated user.
 * Returns null if the user is not authenticated.
 */
const auth = configureAuth(UserInfoService.getCurrentUser);

export const useAuth = auth.useAuth;
export const AuthProvider = auth.AuthProvider;

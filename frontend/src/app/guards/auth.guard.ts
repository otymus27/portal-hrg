import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { map, take } from 'rxjs/operators';

export const AuthGuard: CanActivateFn = (routes, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isLoggedIn$.pipe(
    take(1),
    map((isLoggedIn) => {
      if (!isLoggedIn) {
        router.navigate(['/login']);
        return false;
      }

      const requiredRoles = routes.data['roles'] as string[];
      if (requiredRoles && requiredRoles.length > 0) {
        const userRoles = authService.getLoggedInRoles();
        const hasRequiredRole = userRoles.some((role) =>
          requiredRoles.includes(role)
        );

        if (!hasRequiredRole) {
          // Se não tiver role necessária, volta pro dashboard
          router.navigate(['/admin/dashboard']);
          return false;
        }
      }

      return true;
    })
  );
};

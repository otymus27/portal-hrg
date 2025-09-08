import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service'; // Importe o AuthService
import { map, take } from 'rxjs/operators'; // Importe os operadores map e take

export const AuthGuard: CanActivateFn = (routes, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // ✅ 1. Observe o estado de login e obtenha o valor
  return authService.isLoggedIn$.pipe(
    take(1),
    map((isLoggedIn) => {
      // Se não estiver logado, redireciona para a tela de login
      if (!isLoggedIn) {
        router.navigate(['/login']);
        return false;
      }

      // ✅ 2. Se estiver logado, verifica a permissão
      const requiredRoles = routes.data['roles'] as string[];
      if (requiredRoles && requiredRoles.length > 0) {
        const userRoles = authService.getLoggedInRoles();
        const hasRequiredRole = userRoles.some(role => requiredRoles.includes(role));

        // Se não tiver a role necessária, bloqueia o acesso
        if (!hasRequiredRole) {
          router.navigate(['/admin/home']); // Redireciona para uma rota permitida
          return false;
        }
      }

      // Se tudo estiver OK, permite a navegação
      return true;
    })
  );
};

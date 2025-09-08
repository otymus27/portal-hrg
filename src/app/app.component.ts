// app.component.ts
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router'; // Geralmente usado para renderizar rotas

@Component({
  selector: 'app-root',
  // Se você tiver um template básico, talvez apenas <router-outlet></router-outlet>
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
  // 'imports' e 'providers' não são necessários aqui se você estiver usando AppModule
  // RouterOutlet pode ser importado aqui se o AppComponent o utilizar diretamente
  imports: [RouterOutlet] // Manter RouterOutlet aqui se ele for usado no template do AppComponent
})
export class AppComponent {
  title = 'app-carro';
}
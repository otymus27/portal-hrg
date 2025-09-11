import { Component } from '@angular/core';
import {RouterOutlet} from '@angular/router';
import { SidebarComponent } from "../sidebar/sidebar.component";
import { HeaderComponent } from "../header/header.component";
import { FooterComponent } from "../footer/footer.component";
import { AdminExplorerComponent } from "../../../features/admin/pages/admin-explorer/admin-explorer.component";

@Component({
  selector: 'app-principal',
  imports: [RouterOutlet, SidebarComponent, HeaderComponent, FooterComponent, AdminExplorerComponent],
  templateUrl: './principal.html',
  styleUrl: './principal.scss'
})
export class Principal {

}

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminExplorerComponent } from './admin-explorer.component';

describe('AdminExplorerComponent', () => {
  let component: AdminExplorerComponent;
  let fixture: ComponentFixture<AdminExplorerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminExplorerComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminExplorerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

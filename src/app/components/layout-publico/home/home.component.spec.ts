import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HomeComponentPublico } from './home.component';

describe('HomeComponent', () => {
  let component: HomeComponentPublico;
  let fixture: ComponentFixture<HomeComponentPublico>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HomeComponentPublico],
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponentPublico);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

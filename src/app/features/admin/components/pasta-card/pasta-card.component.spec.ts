import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PastaCardComponent } from './pasta-card.component';

describe('PastaCardComponent', () => {
  let component: PastaCardComponent;
  let fixture: ComponentFixture<PastaCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PastaCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PastaCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

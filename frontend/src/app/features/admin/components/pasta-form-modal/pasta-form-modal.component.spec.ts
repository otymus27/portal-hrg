import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PastaFormModalComponent } from './pasta-form-modal.component';

describe('PastaFormModalComponent', () => {
  let component: PastaFormModalComponent;
  let fixture: ComponentFixture<PastaFormModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PastaFormModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PastaFormModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

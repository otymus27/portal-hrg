import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PastaItemComponent } from './pasta-item.component';

describe('PastaItemComponent', () => {
  let component: PastaItemComponent;
  let fixture: ComponentFixture<PastaItemComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PastaItemComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PastaItemComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ArquivoCardComponent } from './arquivo-card.component';

describe('ArquivoCardComponent', () => {
  let component: ArquivoCardComponent;
  let fixture: ComponentFixture<ArquivoCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ArquivoCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ArquivoCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

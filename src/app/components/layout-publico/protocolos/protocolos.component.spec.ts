import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProtocolosComponent } from './protocolos.component';

describe('ProtocolosComponent', () => {
  let component: ProtocolosComponent;
  let fixture: ComponentFixture<ProtocolosComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProtocolosComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ProtocolosComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

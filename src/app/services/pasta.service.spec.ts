import { TestBed } from '@angular/core/testing';

import { PastaService } from './pasta.service';

describe('PastaService', () => {
  let service: PastaService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PastaService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

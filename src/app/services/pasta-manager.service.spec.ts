import { TestBed } from '@angular/core/testing';

import { PastaManagerService } from './pasta-manager.service';

describe('PastaManagerService', () => {
  let service: PastaManagerService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(PastaManagerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});

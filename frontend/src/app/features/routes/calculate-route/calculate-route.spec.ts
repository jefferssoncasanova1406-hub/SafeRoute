import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CalculateRoute } from './calculate-route';

describe('CalculateRoute', () => {
  let component: CalculateRoute;
  let fixture: ComponentFixture<CalculateRoute>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CalculateRoute],
    }).compileComponents();

    fixture = TestBed.createComponent(CalculateRoute);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

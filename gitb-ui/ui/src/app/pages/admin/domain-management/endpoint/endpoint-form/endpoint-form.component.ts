import { Component, Input } from '@angular/core';
import { Endpoint } from 'src/app/types/endpoint';

@Component({
  selector: 'app-endpoint-form',
  templateUrl: './endpoint-form.component.html',
  styles: [
  ]
})
export class EndpointFormComponent {

  @Input() endpoint!: Partial<Endpoint>

  constructor() { }

}

import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ConformanceEndpoint } from 'src/app/pages/organisation/conformance-statement/conformance-endpoint';
import { DataService } from 'src/app/services/data.service';
import { EndpointParameter } from 'src/app/types/endpoint-parameter';

@Component({
  selector: 'app-endpoint-display',
  templateUrl: './endpoint-display.component.html',
  styles: [
  ]
})
export class EndpointDisplayComponent implements OnInit {

  @Input() endpoint!: ConformanceEndpoint
  @Input() showValues = false
  @Input() editable = false
  @Input() canEdit!: (p: EndpointParameter) => boolean
  @Input() hideEndpointInfo = true
  @Output() edit = new EventEmitter<EndpointParameter>()
  @Output() download = new EventEmitter<EndpointParameter>()

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
  }

  onEdit(parameter: EndpointParameter) {
    this.edit.emit(parameter)
  }

  onDownload(parameter: EndpointParameter) {
    this.download.emit(parameter)
  }

}

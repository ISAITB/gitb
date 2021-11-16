import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ConformanceEndpoint } from 'src/app/pages/organisation/conformance-statement/conformance-endpoint';
import { DataService } from 'src/app/services/data.service';
import { SystemConfigurationParameter } from 'src/app/types/system-configuration-parameter';

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
  @Input() canEdit!: (p: SystemConfigurationParameter) => boolean
  @Input() hideEndpointInfo = true
  @Output() edit = new EventEmitter<SystemConfigurationParameter>()
  @Output() download = new EventEmitter<SystemConfigurationParameter>()

  constructor(
    public dataService: DataService
  ) { }

  ngOnInit(): void {
  }

  onEdit(parameter: SystemConfigurationParameter) {
    this.edit.emit(parameter)
  }

  onDownload(parameter: SystemConfigurationParameter) {
    this.download.emit(parameter)
  }

}

import { Component, Input, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { SUTConfiguration } from 'src/app/types/sutconfiguration';
import { AnyContent } from 'src/app/components/diagram/any-content';
import { ActorInfo } from '../diagram/actor-info';
import { BsModalRef } from 'ngx-bootstrap/modal';

@Component({
  selector: 'app-simulated-configuration-display-modal',
  templateUrl: './simulated-configuration-display-modal.component.html',
  styleUrls: ['./simulated-configuration-display-modal.component.less']
})
export class SimulatedConfigurationDisplayModalComponent implements OnInit {

  @Input() configurations!: SUTConfiguration[]
  @Input() actorInfo!: ActorInfo[]

  items: AnyContent[] = []

  constructor(
    private dataService: DataService,
    private modalRef: BsModalRef
  ) { }

  ngOnInit(): void {
    for (let config of this.configurations) {
      this.items.push(this.configToAnyContent(config))
    }
  }

  private configToAnyContent(config: SUTConfiguration) {
    const actorContent: AnyContent = {name: "Configurations for " + this.getActorName(config.actor)}
    actorContent.item = []
    for (let configActor of config.configs) {
      const configActorContent: AnyContent = {name: this.getActorName(configActor.actor)}
      configActorContent.item = []
      for (let parameter of configActor.config) {
        const parameterContent: AnyContent = {
          name: parameter.name,
          valueToUse: parameter.value
        }
        if (this.dataService.isDataURL(parameter.value)) {
          parameterContent.embeddingMethod = "BASE64"
        } else {
          parameterContent.embeddingMethod = "STRING"
        }
        configActorContent.item.push(parameterContent)
      }
      actorContent.item.push(configActorContent)
    }
    return actorContent
  }

  getActorName(actorId: string) {
    let actorName = actorId
    for (let info of this.actorInfo) {
      if (actorId == info.id) {
        if (info.name != undefined) {
          actorName = info.name
        } else {
          actorName = info.id
        }
        break
      }
    }
    return actorName
  }

  close() {
    this.modalRef.hide()
  }

}
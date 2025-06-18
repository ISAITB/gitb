/*
 * Copyright (C) 2025 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://interoperable-europe.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an
 * "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for
 * the specific language governing permissions and limitations under the Licence.
 */

import { Component, Input, OnInit } from '@angular/core';
import { DataService } from 'src/app/services/data.service';
import { SUTConfiguration } from 'src/app/types/sutconfiguration';
import { AnyContent } from 'src/app/components/diagram/any-content';
import { ActorInfo } from '../diagram/actor-info';
import { BsModalRef } from 'ngx-bootstrap/modal';

@Component({
    selector: 'app-simulated-configuration-display-modal',
    templateUrl: './simulated-configuration-display-modal.component.html',
    styleUrls: ['./simulated-configuration-display-modal.component.less'],
    standalone: false
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

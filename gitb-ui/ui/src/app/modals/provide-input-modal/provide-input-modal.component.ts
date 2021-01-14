import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { DataService } from 'src/app/services/data.service';
import { FileData } from 'src/app/types/file-data.type';
import { UserInteraction } from 'src/app/types/user-interaction';
import { UserInteractionInput } from 'src/app/types/user-interaction-input';

@Component({
  selector: 'app-provide-input-modal',
  templateUrl: './provide-input-modal.component.html',
  styles: [
  ]
})
export class ProvideInputModalComponent implements OnInit {

  @Input() interactions!: UserInteraction[]
  @Input() inputTitle = 'Server interaction'
  @Output() result = new EventEmitter<UserInteractionInput[]>()
  needsInput = false

  constructor(
    private modalRef: BsModalRef,
    public dataService: DataService
  ) { }

  ngOnInit(): void {
		let i = 0
    let firstTextIndex:number|undefined
    this.needsInput = this.interactionNeedsInput()
		for (let interaction of this.interactions) {
			if (interaction.type == "request") {
				if (interaction.options != undefined) {
					const optionValues = interaction.options.split(',').map((item) =>
						item.trim()
					)
          let optionLabelValues: string[]
          if (interaction.optionLabels == undefined) {
            optionLabelValues = optionValues
          } else {
            optionLabelValues = interaction.optionLabels!.split(',').map((item) =>
              item.trim()
            )
          }
					interaction.optionData = []
          let j = 0
					for (let optionValue of optionValues) {
						interaction.optionData.push({
							value: optionValue,
							label: optionLabelValues[j]
						})
            j += 1
          }
        } else {
          if (firstTextIndex == undefined && interaction.contentType != 'BASE64') {
            firstTextIndex = i
          }
        }
      }
			i += 1
    }
		if (firstTextIndex != undefined) {
			this.dataService.focus('input-'+firstTextIndex)
    }
  }

  reset() {
    for (let interaction of this.interactions) {
      delete interaction.data
      delete interaction.selectedOption
      delete interaction.file
    }
  }

  hideInput() {
    const inputs:UserInteractionInput[] = []
    for (let interaction of this.interactions) {
      if (interaction.type == "request") {
        const inputData: Partial<UserInteractionInput> = {
          id: interaction.id,
          name: interaction.name,
          type: interaction.variableType,
          embeddingMethod: interaction.contentType
        }
        if (interaction.optionData != undefined && interaction.data != undefined) {
          if (interaction.multiple) {
            if (Array.isArray(interaction.data)) {
              const values = interaction.data.map((item) => { return item.value })
              inputData.value = values.join()
            }
          } else {
            inputData.value = interaction.selectedOption!.value
          }
        } else {
          inputData.value = interaction.data
        }
        inputs.push(inputData as UserInteractionInput)
      }
    }
    this.result.emit(inputs)
    this.modalRef.hide()
  }

  onFileSelect(request: UserInteraction, file: FileData) {
    request.file = file
    request.data = file.data
  }

  download(interaction: UserInteraction) {
    const blob = this.dataService.b64toBlob(interaction.value!)
    if (interaction.name != undefined) {
      saveAs(blob, interaction.name)
    } else {
      this.dataService.getFileInfo(blob).subscribe((data) => {
        saveAs(blob, data.filename)
      })
    }
  }

  private interactionNeedsInput() {
    for (let interaction of this.interactions) {
      if (interaction.type == "request") {
        return true
      }
    }
    return false
  }

}

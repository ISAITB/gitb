import { AfterViewInit, Component, EventEmitter, Input, OnInit, Output, QueryList, ViewChildren } from '@angular/core';
import { CodemirrorComponent } from '@ctrl/ngx-codemirror';
import { BsModalRef } from 'ngx-bootstrap/modal';
import { AnyContent } from 'src/app/components/diagram/any-content';
import { DataService } from 'src/app/services/data.service';
import { FileData } from 'src/app/types/file-data.type';
import { UserInteraction } from 'src/app/types/user-interaction';
import { UserInteractionInput } from 'src/app/types/user-interaction-input';

@Component({
  selector: 'app-provide-input-modal',
  templateUrl: './provide-input-modal.component.html',
  styleUrls: [ './provide-input-modal.component.less' ]
})
export class ProvideInputModalComponent implements OnInit, AfterViewInit {

  @Input() interactions!: UserInteraction[]
  @Input() inputTitle = 'User interaction'
  @Input() sessionId!: string
  @Output() result = new EventEmitter<UserInteractionInput[]|undefined>()
  @ViewChildren(CodemirrorComponent) codeMirrors?: QueryList<CodemirrorComponent>
  needsInput = false
  firstCodeIndex:number|undefined
  firstTextIndex:number|undefined
  editorFocus: {[key: string]: boolean} = {}

  constructor(
    private modalRef: BsModalRef,
    public dataService: DataService
  ) { }

  ngOnInit(): void {
		let i = 0
    this.needsInput = this.interactionNeedsInput()
		for (let interaction of this.interactions) {
			if (interaction.type == "request") {
        if (interaction.inputType == 'SELECT_SINGLE' || interaction.inputType == 'SELECT_MULTIPLE') {
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
          }
        } else if (interaction.inputType == 'CODE') {
          this.editorFocus['input-'+i] = false
          if (this.firstCodeIndex == undefined) {
            this.firstCodeIndex = i
          }          
          interaction.data = ''
        } else if (interaction.inputType == 'UPLOAD') {
          interaction.reset = new EventEmitter<void>()
        } else {
          if (this.firstTextIndex == undefined) {
            this.firstTextIndex = i
          }
        }
      }
			i += 1
    }
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      if (this.codeMirrors) {
        this.codeMirrors.forEach((codeMirror) => {
          codeMirror.codeMirror!.refresh()
        })
      }
      if (this.firstCodeIndex != undefined && this.firstTextIndex != undefined) {
        if (this.firstCodeIndex < this.firstTextIndex) {
          this.codeEditorForName('input-'+this.firstCodeIndex)?.codeMirror!.focus()
        } else {
          this.dataService.focus('input-'+this.firstTextIndex)
        }
      } else if (this.firstCodeIndex != undefined) {
        this.codeEditorForName('input-'+this.firstCodeIndex)?.codeMirror!.focus()
      } else if (this.firstTextIndex != undefined) {
        this.dataService.focus('input-'+this.firstTextIndex)
      }
    }, 1)
  }

  instructionAsAnyContent(instruction: UserInteraction): AnyContent {
    const content: AnyContent = {
      name: instruction.desc,
      value: instruction.value,
      valueToUse: instruction.value,
      embeddingMethod: (instruction.variableType == 'binary' || instruction.variableType == 'schema' || instruction.variableType == 'object')?'BASE64': 'STRING',
      mimeType: instruction.mimeType
    }
    return content
  }

  private codeEditorForName(editorName: string) {
    if (this.codeMirrors) {
      for (let codeMirror of this.codeMirrors) {
        if (editorName == codeMirror.name) {
          return codeMirror
        }
      }
    }
    return undefined
  }

  codeEditorFocus(focused: boolean, editorName: string) {
    this.editorFocus[editorName] = focused
  }

  reset() {
    let index = 0
    for (let interaction of this.interactions) {
      if (this.editorFocus['input-'+index] == undefined) {
        delete interaction.data
      } else {
        // Code editor.
        interaction.data = ''
      }
      if (interaction.reset) {
        interaction.reset.emit()
      }
      delete interaction.selectedOption
      delete interaction.file
      index += 1
    }
  }

  minimise() {
    this.result.emit(undefined)
    this.modalRef.hide()    
  }

  close() {
    this.result.emit([])
    this.modalRef.hide()    
  }

  submit() {
    const inputs:UserInteractionInput[] = []
    for (let interaction of this.interactions) {
      if (interaction.type == "request") {
        const inputData: Partial<UserInteractionInput> = {
          id: interaction.id,
          name: interaction.name,
          type: interaction.variableType,
          embeddingMethod: interaction.contentType
        }
        if (interaction.optionData != undefined && interaction.selectedOption != undefined) {
          if (interaction.multiple) {
            if (Array.isArray(interaction.selectedOption)) {
              const values = interaction.selectedOption.map((item) => { return item.value })
              inputData.value = values.join()
            }
          } else {
            inputData.value = interaction.selectedOption!.value
          }
        } else if (interaction.optionData == undefined) {
          if (interaction.data != undefined) {
            inputData.value = interaction.data
          } else if (interaction.file?.file) {
            inputData.file = interaction.file.file
          }
        }
        inputs.push(inputData as UserInteractionInput)
      }
    }
    this.result.emit(inputs)
    this.modalRef.hide()
  }

  onFileSelect(request: UserInteraction, file: FileData) {
    request.file = file
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

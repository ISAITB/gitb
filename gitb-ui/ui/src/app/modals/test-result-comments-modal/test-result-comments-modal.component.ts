/*
 * Copyright (C) 2026 European Union
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

import {Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import {Constants} from '../../common/constants';
import {NgbActiveModal} from '@ng-bootstrap/ng-bootstrap';
import {TestResultComments} from '../../types/test-result-comments';
import {DataService} from '../../services/data.service';
import {TestService} from '../../services/test.service';
import {BaseComponent} from '../../pages/base-component.component';
import {PopupService} from '../../services/popup.service';
import {CommentData} from './comment-data';
import {ConfirmationDialogService} from '../../services/confirmation-dialog.service';
import {Observable, of} from 'rxjs';
import {TestResultMinimal} from '../../types/test-result-minimal';

@Component({
  selector: 'app-test-result-comments-modal',
  standalone: false,
  templateUrl: './test-result-comments-modal.component.html',
  styleUrl: './test-result-comments-modal.component.less',
})
export class TestResultCommentsModalComponent extends BaseComponent implements OnInit {

  @Input() sessionId!: string;
  @Input() sessionResult!: string;
  @Input() sessionOutputMessage?: string;
  @Input() sessionOwner?: number;
  @Input() sessionOwnerName?: string
  @Input() comments?: Partial<TestResultComments>;
  @Input() commentsEditable = true;
  @Output() commentUpdate = new EventEmitter<boolean>();
  @Output() updateResult = new EventEmitter<TestResultMinimal>();

  protected commentsToShow: CommentData[] = []
  protected editMode = false;
  protected editingAdminComment = false;
  protected commentContent?: string;
  protected ownerNameToDisplay!: string;
  protected readonly Constants = Constants;
  protected savePending = false;
  protected deletePending = false;
  protected overrideResult = false;
  protected forcedResult?: string
  protected forcedOutputMessage?: string
  protected disableUserComment = false;
  private originalSessionResult!: string;
  private originalSessionOutputMessage?: string;

  constructor(
    private readonly modalInstance: NgbActiveModal,
    protected readonly dataService: DataService,
    private readonly testService: TestService,
    private readonly popupService: PopupService,
    private readonly confirmationDialogService: ConfirmationDialogService
  ) { super(); }

  ngOnInit(): void {
    if (this.sessionOwnerName == undefined) this.sessionOwnerName = ''
    if (this.sessionOwner === this.dataService.vendor?.id) {
      this.ownerNameToDisplay = ((this.dataService.vendor == undefined)?this.sessionOwnerName!:this.dataService.vendor.fname)
    } else {
      this.ownerNameToDisplay = this.sessionOwnerName
    }
    this.forcedResult = this.sessionResult;
    this.forcedOutputMessage = this.sessionOutputMessage;
    this.originalSessionResult = (this.comments?.resultOriginal != undefined)?this.comments.resultOriginal:this.sessionResult;
    this.originalSessionOutputMessage = (this.comments?.outputMessageOriginal != undefined)?this.comments.outputMessageOriginal:this.sessionOutputMessage;
    this.disableUserComment = (this.comments?.userCommentAllowed == undefined)?false:!this.comments.userCommentAllowed
    this.parseCommentData(false);
  }

  protected close() {
    this.modalInstance.dismiss()
  }

  protected editUserComment() {
    this.commentContent = this.comments?.userComment
    this.editMode = true;
    this.editingAdminComment = false;
  }

  protected editAdminComment() {
    this.commentContent = this.comments?.adminComment
    this.editMode = true;
    this.editingAdminComment = true;
    this.overrideResult = this.comments?.resultForced != undefined;
    if (this.comments?.resultForced != undefined) {
      this.forcedResult = this.comments?.resultForced;
      this.forcedOutputMessage = this.comments?.outputMessageForced;
    } else {
      this.forcedResult = this.sessionResult;
      this.forcedOutputMessage = this.sessionOutputMessage;
    }
    this.disableUserComment = (this.comments?.userCommentAllowed == undefined)?false:!this.comments.userCommentAllowed
  }

  protected cancelEdit() {
    this.editMode = false;
    this.overrideResult = false;
    this.forcedResult = undefined;
    this.forcedOutputMessage = undefined;
    this.disableUserComment = false;
  }

  protected saveComment() {
    if (this.saveEnabled()) {
      if (this.editingAdminComment) {
        this.saveAdminComment();
      } else {
        this.saveUserComment();
      }
    }
  }

  private saveUserComment() {
    this.savePending = true;
    let contentToSave = this.commentContent;
    if (!this.textProvided(contentToSave)) contentToSave = undefined;
    this.testService.updateTestSessionUserComment(this.sessionId, contentToSave).subscribe((result) => {
      this.comments = result;
      this.parseCommentData(true);
      this.editMode = false;
      this.popupService.success('Comment saved.')
    }).add(() => {
      this.savePending = false;
    })
  }

  protected saveEnabled() {
    return !this.deletePending && this.visibleHtmlProvided(this.commentContent)
  }

  private saveAdminComment() {
    let proceed$: Observable<any>
    const hasNewForcedResult = (this.overrideResult && (
            (this.comments == undefined && this.forcedResult != undefined) || // No previous comment
            (this.comments != undefined && (this.comments.resultForced != this.forcedResult || this.comments.outputMessageForced != this.forcedOutputMessage)) // Previous comment with different result or message
          )) ||
          (!this.overrideResult && this.comments?.resultForced != undefined);
    if (hasNewForcedResult) {
      let message: string;
      if (!this.overrideResult && this.comments != undefined) {
        message = "Are you sure you want to revert to the test session's original result?";
      } else {
        message = "Are you sure you want to change the test session's result?";
      }
      proceed$ = this.confirmationDialogService.confirmed("Confirm result change", message, "Change result", "Cancel");
    } else {
      proceed$ = of(true);
    }
    proceed$.subscribe(() => {
      this.savePending = true;
      let contentToSave = this.commentContent;
      if (!this.textProvided(contentToSave)) contentToSave = undefined;
      let forcedResultToSave = this.forcedResult;
      let forcedOutputMessageToSave = this.forcedOutputMessage;
      if (!this.overrideResult) {
        forcedResultToSave = undefined;
        forcedOutputMessageToSave = undefined;
      }
      this.testService.updateTestSessionAdminComment(this.sessionId, contentToSave, forcedResultToSave, forcedOutputMessageToSave, !this.disableUserComment).subscribe((result) => {
        this.comments = result;
        this.parseCommentData(true);
        this.editMode = false;
        if (hasNewForcedResult) {
          if (forcedResultToSave != undefined) {
            this.sessionResult = forcedResultToSave;
            this.sessionOutputMessage = forcedOutputMessageToSave;
          } else {
            this.sessionResult = this.originalSessionResult;
            this.sessionOutputMessage = this.originalSessionOutputMessage;
          }
          this.signalResultUpdate();
          this.popupService.success('Comment saved and result changed.');
        } else {
          this.popupService.success('Comment saved.');
        }
      }).add(() => {
        this.savePending = false;
      });
    });
  }

  private signalResultUpdate() {
    if (this.sessionResult == 'SUCCESS' || this.sessionResult == 'FAILURE' || this.sessionResult == 'UNDEFINED') {
      this.updateResult.emit({
        sessionId: this.sessionId,
        result: this.sessionResult,
        outputMessage: this.sessionOutputMessage
      });
    }
  }

  private parseCommentData(fireEvents: boolean) {
    const tempComments: CommentData[] = [];
    if (this.comments) {
      if (this.comments.userComment != undefined) {
        tempComments.push({
          comment: this.comments.userComment,
          commentTime: this.comments.userCommentTime!,
          commentTimeMillis: this.comments.userCommentTimeMillis!,
          admin: false
        });
      }
      if (this.comments.adminComment != undefined) {
        tempComments.push({
          comment: this.comments.adminComment,
          commentTime: this.comments.adminCommentTime!,
          commentTimeMillis: this.comments.adminCommentTimeMillis!,
          admin: true
        })
      }
      tempComments.sort((a, b) => b.commentTimeMillis - a.commentTimeMillis);
    }
    this.commentsToShow = tempComments;
    if (fireEvents) {
      this.commentUpdate.emit(this.commentsToShow.length > 0)
    }
  }

  protected toggleCommentCollapse(comment: CommentData) {
    comment.collapsed = !comment.collapsed;
    if (!comment.collapsed) comment.hidden = false;
  }

  protected selectResult(result: string) {
    this.forcedResult = result;
  }

  protected deleteComment() {
    if (this.editingAdminComment) {
      this.deleteAdminComment();
    } else {
      this.deleteUserComment();
    }
  }

  private deleteUserComment() {
    this.confirmationDialogService.confirmedDangerous("Delete comment", "Are you sure you want to delete this comment?", "Delete", "Cancel").subscribe(() => {
      this.deletePending = true;
      this.testService.updateTestSessionUserComment(this.sessionId, undefined).subscribe((result) => {
        this.comments = result;
        this.parseCommentData(true);
        this.editMode = false;
        this.popupService.success('Comment deleted.')
      }).add(() => {
        this.deletePending = false;
      })
    });
  }

  private deleteAdminComment() {
    let message: string;
    let actionButton: string;
    let popupMessage: string;
    const hasForcedResult = this.comments?.resultForced != undefined;
    if (hasForcedResult) {
      message = "Are you sure you want to delete this comment? Doing so will also revert to the original test session result.";
      actionButton = "Delete and reset result";
      popupMessage = "Comment deleted and result reverted.";
    } else {
      message = "Are you sure you want to delete this comment?";
      actionButton = "Delete";
      popupMessage = "Comment deleted.";
    }
    this.confirmationDialogService.confirmedDangerous("Delete comment", message, actionButton, "Cancel").subscribe(() => {
      this.testService.updateTestSessionAdminComment(this.sessionId, undefined, undefined, undefined, undefined).subscribe((result) => {
        this.comments = result;
        this.sessionResult = this.originalSessionResult;
        this.sessionOutputMessage = this.originalSessionOutputMessage;
        this.parseCommentData(true);
        this.editMode = false;
        this.popupService.success(popupMessage);
        if (hasForcedResult) {
          this.signalResultUpdate();
        }
      }).add(() => {
        this.deletePending = false;
      })
    });
  }

}

import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { HttpClientModule } from '@angular/common/http';
import { CookieService } from 'ngx-cookie-service';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './pages/login/login.component';
import { IndexComponent } from './pages/index/index.component';
import { HtmlComponent } from './modals/html/html.component';
import { ModalModule } from 'ngx-bootstrap/modal';
import { TooltipModule } from 'ngx-bootstrap/tooltip';
import { AlertModule } from 'ngx-bootstrap/alert';
import { CollapseModule } from 'ngx-bootstrap/collapse';
import { BsDatepickerModule } from 'ngx-bootstrap/datepicker';
import { ConfirmationComponent } from './modals/confirmation/confirmation.component'
import { FormsModule } from '@angular/forms';
import { TooltipComponent } from './components/tooltip/tooltip.component';
import { ErrorComponent } from './modals/error/error.component';
import { HomeComponent } from './pages/home/home.component';
import { SettingsComponent } from './pages/settings/settings.component';
import { ProfileComponent } from './pages/settings/profile/profile.component';
import { OrganisationComponent } from './pages/settings/organisation/organisation.component';
import { PasswordComponent } from './pages/settings/password/password.component';
import { SelfRegistrationComponent } from './components/self-registration/self-registration.component';
import { TableComponent } from './components/table/table.component';
import { TableRowComponent } from './components/table-row/table-row.component';
import { SimpleNotificationsModule } from 'angular2-notifications';
import { EditorModule, TINYMCE_SCRIPT_SRC } from '@tinymce/tinymce-angular';
import { ContactSupportComponent } from './modals/contact-support/contact-support.component';
import { EditorComponent } from './components/editor/editor.component';
import { FileSelectComponent } from './components/file-select/file-select.component';
import { CustomPropertyFormComponent } from './components/custom-property-form/custom-property-form.component';
import { ChangePasswordFormComponent } from './components/change-password-form/change-password-form.component';
import { LinkAccountComponent } from './modals/link-account/link-account.component';
import { DisconnectRoleComponent } from './modals/disconnect-role/disconnect-role.component';
import { OptionalCustomPropertyFormComponent } from './components/optional-custom-property-form/optional-custom-property-form.component';
import { AddMemberComponent } from './modals/add-member/add-member.component';
import { AlertDisplayComponent } from './components/alert-display/alert-display.component';
import { PendingBlockComponent } from './components/pending-block/pending-block.component';
import { NoAutoCompleteDirective } from './directives/no-auto-complete.directive';
import { AdminComponent } from './pages/admin/admin.component';
import { SessionDashboardComponent } from './pages/admin/session-dashboard/session-dashboard.component';
import { SessionTableComponent } from './components/session-table/session-table.component';
import { TestFilterComponent } from './components/test-filter/test-filter.component';
import { CustomPropertyFilterComponent } from './components/custom-property-filter/custom-property-filter.component';
import { ToggleComponent } from './components/toggle/toggle.component';
import { TestSessionPresentationComponent } from './components/diagram/test-session-presentation/test-session-presentation.component';
import { SequenceDiagramComponent } from './components/diagram/sequence-diagram/sequence-diagram.component';
import { SequenceDiagramActorComponent } from './components/diagram/sequence-diagram-actor/sequence-diagram-actor.component';
import { SequenceDiagramMessageComponent } from './components/diagram/sequence-diagram-message/sequence-diagram-message.component';
import { SequenceDiagramMessageStatusComponent } from './components/diagram/sequence-diagram-message-status/sequence-diagram-message-status.component';
import { TestStepReportModalComponent } from './components/diagram/test-step-report-modal/test-step-report-modal.component';
import { TestStepReportComponent } from './components/diagram/report/test-step-report/test-step-report.component';
import { TestStepReportSRComponent } from './components/diagram/report/test-step-report-sr/test-step-report-sr.component';
import { TestStepReportDRComponent } from './components/diagram/report/test-step-report-dr/test-step-report-dr.component';
import { TestStepReportTARComponent } from './components/diagram/report/test-step-report-tar/test-step-report-tar.component';
import { AnyContentViewComponent } from './components/diagram/report/any-content-view/any-content-view.component';
import { TestAssertionReportComponent } from './components/diagram/report/test-assertion-report/test-assertion-report.component';
import { CodeEditorModalComponent } from './components/code-editor-modal/code-editor-modal.component';
import { CodemirrorModule } from '@ctrl/ngx-codemirror';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';
import { PendingButtonDirective } from './directives/pending-button.directive';
import { ScrollToTopComponent } from './components/scroll-to-top/scroll-to-top.component';
import { ConformanceDashboardComponent } from './pages/admin/conformance-dashboard/conformance-dashboard.component';
import { ConformanceCertificateModalComponent } from './modals/conformance-certificate-modal/conformance-certificate-modal.component';
import { TestResultStatusDisplayComponent } from './components/test-result-status-display/test-result-status-display.component';
import { PopoverModule } from 'ngx-bootstrap/popover';
import { DomainManagementComponent } from './pages/admin/domain-management/domain-management.component';
import { CreateDomainComponent } from './pages/admin/domain-management/domain/create-domain/create-domain.component';
import { DomainDetailsComponent } from './pages/admin/domain-management/domain/domain-details/domain-details.component';
import { DomainFormComponent } from './pages/admin/domain-management/domain/domain-form/domain-form.component';
import { CreateEditDomainParameterModalComponent } from './modals/create-edit-domain-parameter-modal/create-edit-domain-parameter-modal.component';
import { TestSuiteUploadModalComponent } from './modals/test-suite-upload-modal/test-suite-upload-modal.component';
import { CreateSpecificationComponent } from './pages/admin/domain-management/specification/create-specification/create-specification.component';
import { SpecificationDetailsComponent } from './pages/admin/domain-management/specification/specification-details/specification-details.component';
import { SpecificationFormComponent } from './pages/admin/domain-management/specification/specification-form/specification-form.component';
import { ActorDetailsComponent } from './pages/admin/domain-management/actor/actor-details/actor-details.component';
import { CreateActorComponent } from './pages/admin/domain-management/actor/create-actor/create-actor.component';
import { TestSuiteDetailsComponent } from './pages/admin/domain-management/test-suites/test-suite-details/test-suite-details.component';
import { TestCaseDetailsComponent } from './pages/admin/domain-management/test-suites/test-case-details/test-case-details.component';
import { ActorFormComponent } from './pages/admin/domain-management/actor/actor-form/actor-form.component';
import { CreateEndpointComponent } from './pages/admin/domain-management/endpoint/create-endpoint/create-endpoint.component';
import { EndpointDetailsComponent } from './pages/admin/domain-management/endpoint/endpoint-details/endpoint-details.component';
import { EndpointFormComponent } from './pages/admin/domain-management/endpoint/endpoint-form/endpoint-form.component';
import { CreateParameterModalComponent } from './components/parameters/create-parameter-modal/create-parameter-modal.component';
import { ParameterDetailsModalComponent } from './components/parameters/parameter-details-modal/parameter-details-modal.component';
import { ParameterFormComponent } from './components/parameters/parameter-form/parameter-form.component';
import { TrueFalseValueDirective } from './directives/true-false-value.directive';
import { UserManagementComponent } from './pages/admin/user-management/user-management.component';
import { CreateAdminComponent } from './pages/admin/user-management/system-admin/create-admin/create-admin.component';
import { AdminDetailsComponent } from './pages/admin/user-management/system-admin/admin-details/admin-details.component';
import { CreateCommunityAdminComponent } from './pages/admin/user-management/community-admin/create-community-admin/create-community-admin.component';
import { CommunityAdminDetailsComponent } from './pages/admin/user-management/community-admin/community-admin-details/community-admin-details.component';
import { CreateCommunityComponent } from './pages/admin/user-management/community/create-community/create-community.component';
import { CommunityDetailsComponent } from './pages/admin/user-management/community/community-details/community-details.component';
import { UserFormComponent } from './pages/admin/user-management/user-form/user-form.component';
import { CommunityFormComponent } from './pages/admin/user-management/community/community-form/community-form.component';
import { CreateLegalNoticeComponent } from './pages/admin/user-management/legal-notice/create-legal-notice/create-legal-notice.component';
import { LegalNoticeDetailsComponent } from './pages/admin/user-management/legal-notice/legal-notice-details/legal-notice-details.component';
import { LandingPageDetailsComponent } from './pages/admin/user-management/landing-page/landing-page-details/landing-page-details.component';
import { CreateLandingPageComponent } from './pages/admin/user-management/landing-page/create-landing-page/create-landing-page.component';
import { CreateErrorTemplateComponent } from './pages/admin/user-management/error-template/create-error-template/create-error-template.component';
import { ErrorTemplateDetailsComponent } from './pages/admin/user-management/error-template/error-template-details/error-template-details.component';
import { TriggerComponent } from './pages/admin/user-management/trigger/trigger.component';
import { CreateOrganisationComponent } from './pages/admin/user-management/organisation/create-organisation/create-organisation.component';
import { OrganisationDetailsComponent } from './pages/admin/user-management/organisation/organisation-details/organisation-details.component';
import { CommunityCertificateComponent } from './pages/admin/user-management/community-certificate/community-certificate.component';
import { CommunityPropertiesComponent } from './pages/admin/user-management/community-properties/community-properties.component';
import { CommunityLabelsComponent } from './pages/admin/user-management/community-labels/community-labels.component';
import { PreviewParametersModalComponent } from './modals/preview-parameters-modal/preview-parameters-modal.component';
import { OrganisationFormComponent } from './pages/admin/user-management/organisation/organisation-form/organisation-form.component';
import { CreateUserComponent } from './pages/admin/user-management/user/create-user/create-user.component';
import { UserDetailsComponent } from './pages/admin/user-management/user/user-details/user-details.component';
import { ExportComponent } from './pages/admin/export/export.component';
import { ImportComponent } from './pages/admin/import/import.component';
import { ImportItemPreviewComponent } from './pages/admin/import/import-item-preview/import-item-preview.component';
import { ImportItemGroupPreviewComponent } from './pages/admin/import/import-item-group-preview/import-item-group-preview.component';
import { SystemListComponent } from './pages/organisation/system-list/system-list.component';
import { SystemDetailsComponent } from './pages/organisation/system-details/system-details.component';
import { SystemTestsComponent } from './pages/organisation/system-tests/system-tests.component';
import { OrganisationIndexComponent } from './pages/organisation/organisation-index.component';
import { CreateEditSystemModalComponent } from './modals/create-edit-system-modal/create-edit-system-modal.component';
import { ConformanceStatementsComponent } from './pages/organisation/conformance-statements/conformance-statements.component';
import { SystemInfoComponent } from './pages/organisation/system-info/system-info.component';
import { CreateConformanceStatementComponent } from './pages/organisation/create-conformance-statement/create-conformance-statement.component';
import { ConformanceStatementComponent } from './pages/organisation/conformance-statement/conformance-statement.component';
import { MissingConfigurationModalComponent } from './modals/missing-configuration-modal/missing-configuration-modal.component';
import { TestExecutionComponent } from './pages/test-execution/test-execution.component';
import { EditEndpointConfigurationModalComponent } from './modals/edit-endpoint-configuration-modal/edit-endpoint-configuration-modal.component';
import { EndpointDisplayComponent } from './components/endpoint-display/endpoint-display.component';
import { ParameterDisplayComponent } from './components/parameter-display/parameter-display.component';
import { ProvideInputModalComponent } from './modals/provide-input-modal/provide-input-modal.component';
import { SecretInputComponent } from './components/secret-input/secret-input.component';
import { TabsModule } from 'ngx-bootstrap/tabs';
import { TextFilterComponent } from './components/text-filter/text-filter.component';
import { CopyEnabledTextComponent } from './components/copy-enabled-text/copy-enabled-text.component';
import { SanitizeHtmlPipe } from './pipes/sanitize-html.pipe';
import { ApiKeyTextComponent } from './components/api-key-text/api-key-text.component';
import { ApiKeyInfoComponent } from './components/api-key-info/api-key-info.component';
import { MissingConfigurationDisplayComponent } from './components/missing-configuration-display/missing-configuration-display.component';
import { SimulatedConfigurationDisplayModalComponent } from './components/simulated-configuration-display-modal/simulated-configuration-display-modal.component';
import { SessionLogModalComponent } from './components/session-log-modal/session-log-modal.component';
import { BaseCodeEditorModalComponent } from './components/base-code-editor-modal/base-code-editor-modal.component';
import { MultiSelectFilterComponent } from './components/multi-select-filter/multi-select-filter.component';
import { TestStatusIconsComponent } from './components/test-status-icons/test-status-icons.component';
import { TableColumnContentComponent } from './components/table-row/table-column-content/table-column-content.component';
import { TestTriggerModalComponent } from './pages/admin/user-management/trigger/test-trigger-modal/test-trigger-modal.component';
import { CreateEditCommunityResourceModalComponent } from './modals/create-edit-community-resource-modal/create-edit-community-resource-modal.component';
import { CommunityResourceBulkUploadModalComponent } from './modals/community-resource-bulk-upload-modal/community-resource-bulk-upload-modal.component';
import { LinkSharedTestSuiteModalComponent } from './modals/link-shared-test-suite-modal/link-shared-test-suite-modal.component';
import { TestCaseUpdateListComponent } from './components/test-case-update-list/test-case-update-list.component';
import { TestSuiteUploadSpecificationChoicesComponent } from './components/test-suite-upload-specification-choices/test-suite-upload-specification-choices.component';
import { CreateSpecificationGroupComponent } from './pages/admin/domain-management/specification/group/create-specification-group/create-specification-group.component';
import { SpecificationGroupFormComponent } from './pages/admin/domain-management/specification/group/specification-group-form/specification-group-form.component';
import { SpecificationGroupDetailsComponent } from './pages/admin/domain-management/specification/group/specification-group-details/specification-group-details.component';
import { DomainSpecificationTableRowComponent } from './components/domain-specification-table-row/domain-specification-table-row.component';
import { HiddenIconComponent } from './components/hidden-icon/hidden-icon.component';
import { ConformanceStatementItemDisplayComponent } from './components/conformance-statement-item-display/conformance-statement-item-display.component';
import { ConformanceStatementItemsDisplayComponent } from './components/conformance-statement-items-display/conformance-statement-items-display.component';
import { CollapsingIconComponent } from './components/collapsing-icon/collapsing-icon.component';
import { PlaceholderSelectorComponent } from './components/placeholder-selector/placeholder-selector.component';
import { TestResultRatioComponent } from './components/test-result-ratio/test-result-ratio.component';
import { PrescriptionLevelComponent } from './components/prescription-level/prescription-level.component';
import { CheckboxOptionPanelComponent } from './components/checkbox-option-panel/checkbox-option-panel.component';
import { MenuItemComponent } from './pages/index/menu-item/menu-item.component';
import { MenuGroupComponent } from './pages/index/menu-group/menu-group.component';

@NgModule({
  providers: [ 
    CookieService,
    { provide: TINYMCE_SCRIPT_SRC, useValue: 'tinymce/tinymce.min.js' }
  ],
  declarations: [
    AppComponent,
    LoginComponent,
    IndexComponent,
    HtmlComponent,
    ConfirmationComponent,
    TooltipComponent,
    ErrorComponent,
    HomeComponent,
    SettingsComponent,
    ProfileComponent,
    OrganisationComponent,
    PasswordComponent,
    SelfRegistrationComponent,
    TableComponent,
    TableRowComponent,
    ContactSupportComponent,
    EditorComponent,
    FileSelectComponent,
    CustomPropertyFormComponent,
    ChangePasswordFormComponent,
    LinkAccountComponent,
    DisconnectRoleComponent,
    OptionalCustomPropertyFormComponent,
    AddMemberComponent,
    AlertDisplayComponent,
    PendingBlockComponent,
    NoAutoCompleteDirective,
    AdminComponent,
    SessionDashboardComponent,
    SessionTableComponent,
    TestFilterComponent,
    CustomPropertyFilterComponent,
    ToggleComponent,
    TestSessionPresentationComponent,
    SequenceDiagramComponent,
    SequenceDiagramActorComponent,
    SequenceDiagramMessageComponent,
    SequenceDiagramMessageStatusComponent,
    TestStepReportModalComponent,
    TestStepReportComponent,
    TestStepReportSRComponent,
    TestStepReportDRComponent,
    TestStepReportTARComponent,
    AnyContentViewComponent,
    TestAssertionReportComponent,
    CodeEditorModalComponent,
    PendingButtonDirective,
    ScrollToTopComponent,
    ConformanceDashboardComponent,
    ConformanceCertificateModalComponent,
    TestResultStatusDisplayComponent,
    DomainManagementComponent,
    CreateDomainComponent,
    DomainDetailsComponent,
    DomainFormComponent,
    CreateEditDomainParameterModalComponent,
    TestSuiteUploadModalComponent,
    CreateSpecificationComponent,
    SpecificationDetailsComponent,
    SpecificationFormComponent,
    ActorDetailsComponent,
    CreateActorComponent,
    TestSuiteDetailsComponent,
    TestCaseDetailsComponent,
    ActorFormComponent,
    CreateEndpointComponent,
    EndpointDetailsComponent,
    EndpointFormComponent,
    CreateParameterModalComponent,
    ParameterDetailsModalComponent,
    ParameterFormComponent,
    TrueFalseValueDirective,
    UserManagementComponent,
    CreateAdminComponent,
    AdminDetailsComponent,
    CreateCommunityAdminComponent,
    CommunityAdminDetailsComponent,
    CreateCommunityComponent,
    CommunityDetailsComponent,
    UserFormComponent,
    CommunityFormComponent,
    CreateLegalNoticeComponent,
    LegalNoticeDetailsComponent,
    LandingPageDetailsComponent,
    CreateLandingPageComponent,
    CreateErrorTemplateComponent,
    ErrorTemplateDetailsComponent,
    TriggerComponent,
    CreateOrganisationComponent,
    OrganisationDetailsComponent,
    CommunityCertificateComponent,
    CommunityPropertiesComponent,
    CommunityLabelsComponent,
    PreviewParametersModalComponent,
    OrganisationFormComponent,
    CreateUserComponent,
    UserDetailsComponent,
    ExportComponent,
    ImportComponent,
    ImportItemPreviewComponent,
    ImportItemGroupPreviewComponent,
    SystemListComponent,
    SystemDetailsComponent,
    SystemTestsComponent,
    OrganisationIndexComponent,
    CreateEditSystemModalComponent,
    ConformanceStatementsComponent,
    SystemInfoComponent,
    CreateConformanceStatementComponent,
    ConformanceStatementComponent,
    MissingConfigurationModalComponent,
    TestExecutionComponent,
    EditEndpointConfigurationModalComponent,
    EndpointDisplayComponent,
    ParameterDisplayComponent,
    ProvideInputModalComponent,
    SecretInputComponent,
    TextFilterComponent,
    CopyEnabledTextComponent,
    SanitizeHtmlPipe,
    ApiKeyTextComponent,
    ApiKeyInfoComponent,
    MissingConfigurationDisplayComponent,
    SimulatedConfigurationDisplayModalComponent,
    SessionLogModalComponent,
    BaseCodeEditorModalComponent,
    MultiSelectFilterComponent,
    TestStatusIconsComponent,
    TableColumnContentComponent,
    TestTriggerModalComponent,
    CreateEditCommunityResourceModalComponent,
    CommunityResourceBulkUploadModalComponent,
    LinkSharedTestSuiteModalComponent,
    TestCaseUpdateListComponent,
    TestSuiteUploadSpecificationChoicesComponent,
    CreateSpecificationGroupComponent,
    SpecificationGroupFormComponent,
    SpecificationGroupDetailsComponent,
    DomainSpecificationTableRowComponent,
    HiddenIconComponent,
    ConformanceStatementItemDisplayComponent,
    ConformanceStatementItemsDisplayComponent,
    CollapsingIconComponent,
    PlaceholderSelectorComponent,
    TestResultRatioComponent,
    PrescriptionLevelComponent,
    CheckboxOptionPanelComponent,
    MenuItemComponent,
    MenuGroupComponent],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    EditorModule,
    FormsModule,
    AppRoutingModule,
    HttpClientModule,
    SimpleNotificationsModule.forRoot(),
    ModalModule.forRoot(),
    TooltipModule.forRoot(),
    AlertModule.forRoot(),
    CollapseModule.forRoot(),
    BsDatepickerModule.forRoot(),
    CodemirrorModule,
    BsDropdownModule.forRoot(),
    PopoverModule.forRoot(),
    TabsModule.forRoot()
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
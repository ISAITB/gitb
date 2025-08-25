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

import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NgModule } from '@angular/core';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { MarkdownModule } from 'ngx-markdown';
import { CookieService } from 'ngx-cookie-service';
import { ColorPickerComponent as NgxColorPickerComponent, ColorPickerDirective } from 'ngx-color-picker';
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
import { ProfileComponent } from './pages/settings/profile/profile.component';
import { OrganisationComponent } from './pages/settings/organisation/organisation.component';
import { PasswordComponent } from './pages/settings/password/password.component';
import { SelfRegistrationComponent } from './components/self-registration/self-registration.component';
import { TableComponent } from './components/table/table.component';
import { TableRowComponent } from './components/table-row/table-row.component';
import { SimpleNotificationsModule } from 'angular2-notifications';
import { EditorModule, HUGERTE_SCRIPT_SRC } from '@hugerte/hugerte-angular';
import { ContactSupportComponent } from './modals/contact-support/contact-support.component';
import { EditorComponent } from './components/editor/editor.component';
import { FileSelectComponent } from './components/file-select/file-select.component';
import { CustomPropertyFormComponent } from './components/custom-property-form/custom-property-form.component';
import { ChangePasswordFormComponent } from './components/change-password-form/change-password-form.component';
import { LinkAccountComponent } from './modals/link-account/link-account.component';
import { DisconnectRoleComponent } from './modals/disconnect-role/disconnect-role.component';
import { OptionalCustomPropertyFormComponent } from './components/optional-custom-property-form/optional-custom-property-form.component';
import { AlertDisplayComponent } from './components/alert-display/alert-display.component';
import { PendingBlockComponent } from './components/pending-block/pending-block.component';
import { NoAutoCompleteDirective } from './directives/no-auto-complete.directive';
import { SessionDashboardComponent } from './pages/admin/session-dashboard/session-dashboard.component';
import { SessionTableComponent } from './components/session-table/session-table.component';
import { TestFilterComponent } from './components/test-filter/test-filter.component';
import { CustomPropertyFilterComponent } from './components/custom-property-filter/custom-property-filter.component';
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
import { OrganisationTestsComponent } from './pages/organisation/organisation-tests/organisation-tests.component';
import { ConformanceStatementsComponent } from './pages/organisation/conformance-statements/conformance-statements.component';
import { CreateConformanceStatementComponent } from './pages/organisation/create-conformance-statement/create-conformance-statement.component';
import { ConformanceStatementComponent } from './pages/organisation/conformance-statement/conformance-statement.component';
import { MissingConfigurationModalComponent } from './modals/missing-configuration-modal/missing-configuration-modal.component';
import { TestExecutionComponent } from './pages/test-execution/test-execution.component';
import { ParameterDisplayComponent } from './components/parameter-display/parameter-display.component';
import { ProvideInputModalComponent } from './modals/provide-input-modal/provide-input-modal.component';
import { SecretInputComponent } from './components/secret-input/secret-input.component';
import { TabsModule } from 'ngx-bootstrap/tabs';
import { TextFilterComponent } from './components/text-filter/text-filter.component';
import { CopyEnabledTextComponent } from './components/copy-enabled-text/copy-enabled-text.component';
import { SanitizeHtmlPipe } from './pipes/sanitize-html.pipe';
import { ApiKeyTextComponent } from './components/api-key-text/api-key-text.component';
import { ApiKeyInfoComponent } from './components/api-key-info/api-key-info.component';
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
import { CreateSystemComponent } from './pages/admin/user-management/system/create-system/create-system.component';
import { SystemDetailsComponent } from './pages/admin/user-management/system/system-details/system-details.component';
import { SystemFormComponent } from './pages/admin/user-management/system/system-form/system-form.component';
import { HeaderMenuComponent } from './pages/index/header-menu/header-menu.component';
import { TagComponent } from './components/tag/tag.component';
import { TestCaseDisplayComponent } from './components/test-case-display/test-case-display.component';
import { TestSuiteDisplayComponent } from './components/test-suite-display/test-suite-display.component';
import { TagsDisplayComponent } from './components/tags-display/tags-display.component';
import { CreateEditTagComponent } from './modals/create-edit-tag/create-edit-tag.component';
import { DomainSpecificationDisplayComponent } from './components/domain-specification-display/domain-specification-display.component';
import { SystemAdministrationComponent } from './pages/admin/system-administration/system-administration.component';
import { ConfigurationEntryComponent } from './pages/admin/system-administration/configuration-entry/configuration-entry.component';
import { TabTitleComponent } from './components/tab-title/tab-title.component';
import { ConformanceSnapshotsModalComponent } from './modals/conformance-snapshots-modal/conformance-snapshots-modal.component';
import { ManageBadgesComponent } from './components/manage-badges/manage-badges.component';
import { ManageBadgeComponent } from './components/manage-badges/manage-badge/manage-badge.component';
import { PreviewBadgeModalComponent } from './modals/preview-badge-modal/preview-badge-modal.component';
import { ViewBadgeButtonComponent } from './components/view-badge-button/view-badge-button.component';
import { ResultLabelComponent } from './components/result-label/result-label.component';
import { PreviewLandingPageComponent } from './pages/admin/user-management/landing-page/preview-landing-page/preview-landing-page.component';
import { BreadcrumbComponent } from './components/breadcrumb/breadcrumb.component';
import { TarReportComponent } from './components/tar-report/tar-report.component';
import { CreateThemeComponent } from './pages/admin/system-administration/create-theme/create-theme.component';
import { ThemeDetailsComponent } from './pages/admin/system-administration/theme-details/theme-details.component';
import { ThemeFormComponent } from './pages/admin/system-administration/theme-form/theme-form.component';
import { ColorPickerComponent } from './components/color-picker/color-picker.component';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { FileDropTargetDirective } from './directives/file-drop-target.directive';
import { FormSectionComponent } from './components/form-section/form-section.component';
import { SpecificationReferenceFormComponent } from './components/specification-reference-form/specification-reference-form.component';
import { SpecificationReferenceDisplayComponent } from './components/specification-reference-display/specification-reference-display.component';
import { SortIndicatorComponent } from './components/sort-indicator/sort-indicator.component';
import { CommunityReportsComponent } from './pages/admin/user-management/community-reports/community-reports.component';
import { ConformanceCertificateFormComponent } from './pages/admin/user-management/community-reports/conformance-certificate-form/conformance-certificate-form.component';
import { ConformanceStatementReportFormComponent } from './pages/admin/user-management/community-reports/conformance-statement-report-form/conformance-statement-report-form.component';
import { ConformanceOverviewReportFormComponent } from './pages/admin/user-management/community-reports/conformance-overview-report-form/conformance-overview-report-form.component';
import { TestCaseReportFormComponent } from './pages/admin/user-management/community-reports/test-case-report-form/test-case-report-form.component';
import { TestStepReportFormComponent } from './pages/admin/user-management/community-reports/test-step-report-form/test-step-report-form.component';
import { CommunityKeystoreModalComponent } from './modals/community-keystore-modal/community-keystore-modal.component';
import { ConformanceOverviewCertificateFormComponent } from './pages/admin/user-management/community-reports/conformance-overview-certificate-form/conformance-overview-certificate-form.component';
import { StatementControlsComponent } from './components/statement-controls/statement-controls.component';
import { ConformanceOverviewCertificateModalComponent } from './modals/conformance-overview-certificate-modal/conformance-overview-certificate-modal.component';
import { EndpointParameterTabContentComponent } from './pages/admin/domain-management/endpoint/endpoint-parameter-tab-content/endpoint-parameter-tab-content.component';
import { CustomPropertyPanelComponent } from './components/custom-property-panel/custom-property-panel.component';
import { InvalidFormControlDirective } from './directives/invalid-form-control.directive';
import { PendingDivComponent } from './components/pending-div/pending-div.component';
import { TriggerFireExpressionControlComponent } from './pages/admin/user-management/trigger/trigger-fire-expression-control/trigger-fire-expression-control.component';
import { TriggerFireExpressionModalComponent } from './pages/admin/user-management/trigger/trigger-fire-expression-modal/trigger-fire-expression-modal.component';
import { OutputMessageDisplayComponent } from './components/output-message-display/output-message-display.component';
import { AccountCardComponent } from './components/account-card/account-card.component';
import { ResourceManagementTabComponent } from './components/resource-management-tab/resource-management-tab.component';
import { CommunitySessionDashboardComponent } from './pages/organisation/community-session-dashboard/community-session-dashboard.component';
import { PagingControlsComponent } from './components/paging-controls/paging-controls.component';
import { ServiceHealthDashboardComponent } from './pages/service-health-dashboard/service-health-dashboard.component';
import { ServiceHealthCardComponent } from './components/service-health-card/service-health-card.component';
import { ServiceHealthModalComponent } from './modals/service-health-modal/service-health-modal.component';
import { ServiceHealthIconComponent } from './components/service-health-icon/service-health-icon.component';
import { FilterControlComponent } from './components/filter-control/filter-control.component';
import { TestCaseFilterComponent } from './components/test-case-filter/test-case-filter.component';
import { NavigationControlsComponent } from './components/navigation-controls/navigation-controls.component';
import { ConformanceStatementTableComponent } from './components/conformance-statement-table/conformance-statement-table.component';
import { CreateEditTestServiceModalComponent } from './modals/create-edit-test-service-modal/create-edit-test-service-modal.component';

@NgModule({ declarations: [
        AppComponent,
        LoginComponent,
        IndexComponent,
        HtmlComponent,
        ConfirmationComponent,
        TooltipComponent,
        ErrorComponent,
        HomeComponent,
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
        AlertDisplayComponent,
        PendingBlockComponent,
        NoAutoCompleteDirective,
        SessionDashboardComponent,
        SessionTableComponent,
        TestFilterComponent,
        CustomPropertyFilterComponent,
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
        InvalidFormControlDirective,
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
        OrganisationTestsComponent,
        ConformanceStatementsComponent,
        CreateConformanceStatementComponent,
        ConformanceStatementComponent,
        MissingConfigurationModalComponent,
        TestExecutionComponent,
        ParameterDisplayComponent,
        ProvideInputModalComponent,
        SecretInputComponent,
        TextFilterComponent,
        CopyEnabledTextComponent,
        ApiKeyTextComponent,
        ApiKeyInfoComponent,
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
        HiddenIconComponent,
        ConformanceStatementItemDisplayComponent,
        ConformanceStatementItemsDisplayComponent,
        CollapsingIconComponent,
        PlaceholderSelectorComponent,
        TestResultRatioComponent,
        PrescriptionLevelComponent,
        CheckboxOptionPanelComponent,
        MenuItemComponent,
        MenuGroupComponent,
        CreateSystemComponent,
        SystemFormComponent,
        SystemDetailsComponent,
        HeaderMenuComponent,
        TagComponent,
        TestCaseDisplayComponent,
        TestSuiteDisplayComponent,
        TagsDisplayComponent,
        CreateEditTagComponent,
        DomainSpecificationDisplayComponent,
        SystemAdministrationComponent,
        ConfigurationEntryComponent,
        TabTitleComponent,
        ConformanceSnapshotsModalComponent,
        ManageBadgesComponent,
        ManageBadgeComponent,
        SanitizeHtmlPipe,
        PreviewBadgeModalComponent,
        ViewBadgeButtonComponent,
        ResultLabelComponent,
        PreviewLandingPageComponent,
        BreadcrumbComponent,
        TarReportComponent,
        CreateThemeComponent,
        ThemeDetailsComponent,
        ThemeFormComponent,
        ColorPickerComponent,
        FileDropTargetDirective,
        FormSectionComponent,
        SpecificationReferenceFormComponent,
        SpecificationReferenceDisplayComponent,
        SortIndicatorComponent,
        CommunityReportsComponent,
        ConformanceCertificateFormComponent,
        ConformanceStatementReportFormComponent,
        ConformanceOverviewReportFormComponent,
        TestCaseReportFormComponent,
        TestStepReportFormComponent,
        CommunityKeystoreModalComponent,
        ConformanceOverviewCertificateFormComponent,
        StatementControlsComponent,
        ConformanceOverviewCertificateModalComponent,
        EndpointParameterTabContentComponent,
        CustomPropertyPanelComponent,
        PendingDivComponent,
        TriggerFireExpressionControlComponent,
        TriggerFireExpressionModalComponent,
        OutputMessageDisplayComponent,
        AccountCardComponent,
        ResourceManagementTabComponent,
        CommunitySessionDashboardComponent,
        PagingControlsComponent,
        ServiceHealthDashboardComponent,
        ServiceHealthCardComponent,
        ServiceHealthModalComponent,
        ServiceHealthIconComponent,
        FilterControlComponent,
        TestCaseFilterComponent,
        NavigationControlsComponent,
        ConformanceStatementTableComponent,
        CreateEditTestServiceModalComponent
    ],
    bootstrap: [AppComponent], imports: [
    NgxColorPickerComponent,
    ColorPickerDirective,
    BrowserModule,
    BrowserAnimationsModule,
    EditorModule,
    FormsModule,
    AppRoutingModule,
    DragDropModule,
    SimpleNotificationsModule.forRoot(),
    ModalModule.forRoot(),
    TooltipModule.forRoot(),
    AlertModule.forRoot(),
    CollapseModule.forRoot(),
    BsDatepickerModule.forRoot(),
    CodemirrorModule,
    BsDropdownModule.forRoot(),
    PopoverModule.forRoot(),
    TabsModule.forRoot(),
    MarkdownModule.forRoot()
  ], providers: [
        CookieService,
        { provide: HUGERTE_SCRIPT_SRC, useValue: 'hugerte/hugerte.min.js' },
        provideHttpClient(withInterceptorsFromDi())
    ] })
export class AppModule { }

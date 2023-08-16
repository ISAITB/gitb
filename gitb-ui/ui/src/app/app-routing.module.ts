import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { AdminComponent } from './pages/admin/admin.component';
import { ConformanceDashboardComponent } from './pages/admin/conformance-dashboard/conformance-dashboard.component';
import { ActorDetailsComponent } from './pages/admin/domain-management/actor/actor-details/actor-details.component';
import { CreateActorComponent } from './pages/admin/domain-management/actor/create-actor/create-actor.component';
import { DomainManagementComponent } from './pages/admin/domain-management/domain-management.component';
import { CreateDomainComponent } from './pages/admin/domain-management/domain/create-domain/create-domain.component';
import { DomainDetailsComponent } from './pages/admin/domain-management/domain/domain-details/domain-details.component';
import { CreateEndpointComponent } from './pages/admin/domain-management/endpoint/create-endpoint/create-endpoint.component';
import { EndpointDetailsComponent } from './pages/admin/domain-management/endpoint/endpoint-details/endpoint-details.component';
import { CreateSpecificationComponent } from './pages/admin/domain-management/specification/create-specification/create-specification.component';
import { SpecificationDetailsComponent } from './pages/admin/domain-management/specification/specification-details/specification-details.component';
import { TestCaseDetailsComponent } from './pages/admin/domain-management/test-suites/test-case-details/test-case-details.component';
import { TestSuiteDetailsComponent } from './pages/admin/domain-management/test-suites/test-suite-details/test-suite-details.component';
import { SessionDashboardComponent } from './pages/admin/session-dashboard/session-dashboard.component';
import { CommunityAdminDetailsComponent } from './pages/admin/user-management/community-admin/community-admin-details/community-admin-details.component';
import { CreateCommunityAdminComponent } from './pages/admin/user-management/community-admin/create-community-admin/create-community-admin.component';
import { CommunityDetailsComponent } from './pages/admin/user-management/community/community-details/community-details.component';
import { CreateCommunityComponent } from './pages/admin/user-management/community/create-community/create-community.component';
import { CreateErrorTemplateComponent } from './pages/admin/user-management/error-template/create-error-template/create-error-template.component';
import { ErrorTemplateDetailsComponent } from './pages/admin/user-management/error-template/error-template-details/error-template-details.component';
import { CreateLandingPageComponent } from './pages/admin/user-management/landing-page/create-landing-page/create-landing-page.component';
import { LandingPageDetailsComponent } from './pages/admin/user-management/landing-page/landing-page-details/landing-page-details.component';
import { CreateLegalNoticeComponent } from './pages/admin/user-management/legal-notice/create-legal-notice/create-legal-notice.component';
import { LegalNoticeDetailsComponent } from './pages/admin/user-management/legal-notice/legal-notice-details/legal-notice-details.component';
import { CreateOrganisationComponent } from './pages/admin/user-management/organisation/create-organisation/create-organisation.component';
import { OrganisationDetailsComponent } from './pages/admin/user-management/organisation/organisation-details/organisation-details.component';
import { AdminDetailsComponent } from './pages/admin/user-management/system-admin/admin-details/admin-details.component';
import { CreateAdminComponent } from './pages/admin/user-management/system-admin/create-admin/create-admin.component';
import { TriggerComponent } from './pages/admin/user-management/trigger/trigger.component';
import { UserManagementComponent } from './pages/admin/user-management/user-management.component';
import { HomeComponent } from './pages/home/home.component';
import { IndexComponent } from './pages/index/index.component';
import { LoginComponent } from './pages/login/login.component';
import { OrganisationComponent } from './pages/settings/organisation/organisation.component';
import { PasswordComponent } from './pages/settings/password/password.component';
import { ProfileComponent } from './pages/settings/profile/profile.component';
import { SettingsComponent } from './pages/settings/settings.component';
import { CommunityResolver } from './resolvers/community-resolver';
import { EditOwnOrganisationResolver } from './resolvers/edit-own-organisation-resolver';
import { ErrorTemplateResolver } from './resolvers/error-template-resolver';
import { LandingPageResolver } from './resolvers/landing-page-resolver';
import { LegalNoticeResolver } from './resolvers/legal-notice-resolver';
import { ProfileResolver } from './resolvers/profile-resolver'
import { RouteAuthenticationGuard } from './resolvers/route-authentication.guard';
import { CommunityPropertiesComponent } from './pages/admin/user-management/community-properties/community-properties.component';
import { CommunityCertificateComponent } from './pages/admin/user-management/community-certificate/community-certificate.component';
import { CommunityLabelsComponent } from './pages/admin/user-management/community-labels/community-labels.component';
import { CreateUserComponent } from './pages/admin/user-management/user/create-user/create-user.component';
import { UserDetailsComponent } from './pages/admin/user-management/user/user-details/user-details.component';
import { ExportComponent } from './pages/admin/export/export.component';
import { ImportComponent } from './pages/admin/import/import.component';
import { OrganisationIndexComponent } from './pages/organisation/organisation-index.component';
import { ConformanceStatementsComponent } from './pages/organisation/conformance-statements/conformance-statements.component';
import { CreateConformanceStatementComponent } from './pages/organisation/create-conformance-statement/create-conformance-statement.component';
import { ConformanceStatementComponent } from './pages/organisation/conformance-statement/conformance-statement.component';
import { TestExecutionComponent } from './pages/test-execution/test-execution.component';
import { CreateSpecificationGroupComponent } from './pages/admin/domain-management/specification/group/create-specification-group/create-specification-group.component';
import { SpecificationGroupDetailsComponent } from './pages/admin/domain-management/specification/group/specification-group-details/specification-group-details.component';
import { CreateSystemComponent } from './pages/admin/user-management/system/create-system/create-system.component';
import { SystemDetailsComponent } from './pages/admin/user-management/system/system-details/system-details.component';
import { OrganisationTestsComponent } from './pages/organisation/organisation-tests/organisation-tests.component';

const routes: Routes = [
  { path: '', redirectTo: '/home', pathMatch: 'full' },  
  { 
    path: '',
    component: IndexComponent,
    resolve: {
      profile: ProfileResolver
    },
    canActivate: [RouteAuthenticationGuard],
    children: [
      { path: 'home', component: HomeComponent, resolve: { profile: ProfileResolver } },
      { path: 'login',  component: LoginComponent , resolve: { profile: ProfileResolver } },
      { path: 'settings', component: SettingsComponent, children: [
          { path: 'profile', component: ProfileComponent },
          { path: 'organisation', component: OrganisationComponent, resolve: { canEditOwnOrganisation: EditOwnOrganisationResolver } },
          { path: 'organisation/user/create', component: CreateUserComponent },
          { path: 'organisation/user/:user_id', component: UserDetailsComponent },
          { path: 'organisation/system/create', component: CreateSystemComponent },
          { path: 'organisation/system/:sys_id', component: SystemDetailsComponent },
          { path: 'password', component: PasswordComponent },
      ] },
      { path: 'admin', component: AdminComponent, children: [
          { path: 'sessions', component: SessionDashboardComponent },
          { path: 'conformance', component: ConformanceDashboardComponent },
          { path: 'domains', component: DomainManagementComponent },
          { path: 'domains/create', component: CreateDomainComponent },
          { path: 'domains/:id', component: DomainDetailsComponent },
          { path: 'domains/:id/testsuites/:testsuite_id', component: TestSuiteDetailsComponent },
          { path: 'domains/:id/testsuites/:testsuite_id/testcases/:testcase_id', component: TestCaseDetailsComponent },
          { path: 'domains/:id/specifications/groups/create', component: CreateSpecificationGroupComponent },
          { path: 'domains/:id/specifications/groups/:group_id', component: SpecificationGroupDetailsComponent },
          { path: 'domains/:id/specifications/create', component: CreateSpecificationComponent },
          { path: 'domains/:id/specifications/:spec_id', component: SpecificationDetailsComponent },
          { path: 'domains/:id/specifications/:spec_id/actors/create', component: CreateActorComponent },
          { path: 'domains/:id/specifications/:spec_id/actors/:actor_id', component: ActorDetailsComponent },
          { path: 'domains/:id/specifications/:spec_id/testsuites/:testsuite_id', component: TestSuiteDetailsComponent },
          { path: 'domains/:id/specifications/:spec_id/testsuites/:testsuite_id/testcases/:testcase_id', component: TestCaseDetailsComponent },
          { path: 'domains/:id/specifications/:spec_id/actors/:actor_id/endpoints/create', component: CreateEndpointComponent },
          { path: 'domains/:id/specifications/:spec_id/actors/:actor_id/endpoints/:endpoint_id', component: EndpointDetailsComponent },
          { path: 'users' , component: UserManagementComponent },
          { path: 'users/admin/create' , component: CreateAdminComponent },
          { path: 'users/admin/:id' , component: AdminDetailsComponent },
          { path: 'users/community/create' , component: CreateCommunityComponent },
          { path: 'users/community/:community_id' , component: CommunityDetailsComponent, resolve: { community: CommunityResolver} },
          { path: 'users/community/:community_id/admin/create', component: CreateCommunityAdminComponent },
          { path: 'users/community/:community_id/admin/:admin_id', component: CommunityAdminDetailsComponent },
          { path: 'users/community/:community_id/pages/create', component: CreateLandingPageComponent, resolve: { base: LandingPageResolver } },
          { path: 'users/community/:community_id/pages/:page_id', component: LandingPageDetailsComponent },
          { path: 'users/community/:community_id/notices/create', component: CreateLegalNoticeComponent, resolve: { base: LegalNoticeResolver } },
          { path: 'users/community/:community_id/notices/:notice_id', component: LegalNoticeDetailsComponent },
          { path: 'users/community/:community_id/errortemplates/create', component: CreateErrorTemplateComponent, resolve: { base: ErrorTemplateResolver } },
          { path: 'users/community/:community_id/errortemplates/:template_id', component: ErrorTemplateDetailsComponent },
          { path: 'users/community/:community_id/triggers/create', component: TriggerComponent },
          { path: 'users/community/:community_id/triggers/:trigger_id', component: TriggerComponent },
          { path: 'users/community/:community_id/certificate', component: CommunityCertificateComponent },
          { path: 'users/community/:community_id/parameters', component: CommunityPropertiesComponent },
          { path: 'users/community/:community_id/labels', component: CommunityLabelsComponent },
          { path: 'users/community/:community_id/organisation/create', component: CreateOrganisationComponent },
          { path: 'users/community/:community_id/organisation/:org_id', component: OrganisationDetailsComponent },
          { path: 'users/community/:community_id/organisation/:org_id/conformance', component: ConformanceStatementsComponent },
          { path: 'users/community/:community_id/organisation/:org_id/conformance/system/:sys_id/create', component: CreateConformanceStatementComponent },
          { path: 'users/community/:community_id/organisation/:org_id/conformance/system/:sys_id/actor/:actor_id', component: ConformanceStatementComponent },
          { path: 'users/community/:community_id/organisation/:org_id/user/create', component: CreateUserComponent },
          { path: 'users/community/:community_id/organisation/:org_id/user/:user_id', component: UserDetailsComponent },
          { path: 'users/community/:community_id/organisation/:org_id/system/create', component: CreateSystemComponent },
          { path: 'users/community/:community_id/organisation/:org_id/system/:sys_id', component: SystemDetailsComponent },
          { path: 'users/community/:community_id/organisation/:org_id/test/:sys_id/:actor_id/execute', component: TestExecutionComponent },
          { path: 'export', component: ExportComponent },
          { path: 'import', component: ImportComponent }
      ] },
      { path: 'organisation/:org_id', component: OrganisationIndexComponent, children: [
        { path: 'conformance', component: ConformanceStatementsComponent },
        { path: 'conformance/system/:sys_id/create', component: CreateConformanceStatementComponent },
        { path: 'conformance/system/:sys_id/actor/:actor_id', component: ConformanceStatementComponent },
        { path: 'tests', component: OrganisationTestsComponent },
        { path: 'test/:system_id/:actor_id/execute', component: TestExecutionComponent }
      ] },
    ]
  }
]

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true, paramsInheritanceStrategy: 'always', onSameUrlNavigation: 'reload'})],
  exports: [RouterModule]
})
export class AppRoutingModule { }

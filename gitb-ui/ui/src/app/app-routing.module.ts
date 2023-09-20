import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
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
import { ConformanceStatementsComponent } from './pages/organisation/conformance-statements/conformance-statements.component';
import { CreateConformanceStatementComponent } from './pages/organisation/create-conformance-statement/create-conformance-statement.component';
import { ConformanceStatementComponent } from './pages/organisation/conformance-statement/conformance-statement.component';
import { TestExecutionComponent } from './pages/test-execution/test-execution.component';
import { CreateSpecificationGroupComponent } from './pages/admin/domain-management/specification/group/create-specification-group/create-specification-group.component';
import { SpecificationGroupDetailsComponent } from './pages/admin/domain-management/specification/group/specification-group-details/specification-group-details.component';
import { CreateSystemComponent } from './pages/admin/user-management/system/create-system/create-system.component';
import { SystemDetailsComponent } from './pages/admin/user-management/system/system-details/system-details.component';
import { OrganisationTestsComponent } from './pages/organisation/organisation-tests/organisation-tests.component';
import { Constants } from './common/constants';
import { SystemAdministrationComponent } from './pages/admin/system-administration/system-administration.component';

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
      // My tests
      { path: 'settings/profile', component: ProfileComponent },
      { path: 'settings/organisation', component: OrganisationComponent, resolve: { canEditOwnOrganisation: EditOwnOrganisationResolver } },
      { path: 'settings/organisation/user/create', component: CreateUserComponent },
      { path: 'settings/organisation/user/:'+Constants.NAVIGATION_PATH_PARAM.USER_ID, component: UserDetailsComponent },
      { path: 'settings/organisation/system/create', component: CreateSystemComponent },
      { path: 'settings/organisation/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID, component: SystemDetailsComponent },
      { path: 'settings/password', component: PasswordComponent },
      // Administration
      { path: 'admin/sessions', component: SessionDashboardComponent },
      { path: 'admin/conformance', component: ConformanceDashboardComponent },
      { path: 'admin/domains', component: DomainManagementComponent },
      { path: 'admin/domains/create', component: CreateDomainComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID, component: DomainDetailsComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/testsuites/:'+Constants.NAVIGATION_PATH_PARAM.TEST_SUITE_ID, component: TestSuiteDetailsComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/testsuites/:'+Constants.NAVIGATION_PATH_PARAM.TEST_SUITE_ID+'/testcases/:'+Constants.NAVIGATION_PATH_PARAM.TEST_CASE_ID, component: TestCaseDetailsComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/groups/create', component: CreateSpecificationGroupComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/groups/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_GROUP_ID, component: SpecificationGroupDetailsComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/create', component: CreateSpecificationComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID, component: SpecificationDetailsComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/actors/create', component: CreateActorComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/actors/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID, component: ActorDetailsComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/testsuites/:'+Constants.NAVIGATION_PATH_PARAM.TEST_SUITE_ID, component: TestSuiteDetailsComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/testsuites/:'+Constants.NAVIGATION_PATH_PARAM.TEST_SUITE_ID+'/testcases/:'+Constants.NAVIGATION_PATH_PARAM.TEST_CASE_ID, component: TestCaseDetailsComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/actors/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID+'/endpoints/create', component: CreateEndpointComponent },
      { path: 'admin/domains/:'+Constants.NAVIGATION_PATH_PARAM.DOMAIN_ID+'/specifications/:'+Constants.NAVIGATION_PATH_PARAM.SPECIFICATION_ID+'/actors/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID+'/endpoints/:'+Constants.NAVIGATION_PATH_PARAM.ENDPOINT_ID, component: EndpointDetailsComponent },
      { path: 'admin/users' , component: UserManagementComponent },
      { path: 'admin/users/community/create' , component: CreateCommunityComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID, component: CommunityDetailsComponent, resolve: { community: CommunityResolver} },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/admin/create', component: CreateCommunityAdminComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/admin/:'+Constants.NAVIGATION_PATH_PARAM.USER_ID, component: CommunityAdminDetailsComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/pages/create', component: CreateLandingPageComponent, resolve: { base: LandingPageResolver } },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/pages/:'+Constants.NAVIGATION_PATH_PARAM.LANDING_PAGE_ID, component: LandingPageDetailsComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/notices/create', component: CreateLegalNoticeComponent, resolve: { base: LegalNoticeResolver } },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/notices/:'+Constants.NAVIGATION_PATH_PARAM.LEGAL_NOTICE_ID, component: LegalNoticeDetailsComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/errortemplates/create', component: CreateErrorTemplateComponent, resolve: { base: ErrorTemplateResolver } },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/errortemplates/:'+Constants.NAVIGATION_PATH_PARAM.ERROR_TEMPLATE_ID, component: ErrorTemplateDetailsComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/triggers/create', component: TriggerComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/triggers/:'+Constants.NAVIGATION_PATH_PARAM.TRIGGER_ID, component: TriggerComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/certificate', component: CommunityCertificateComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/parameters', component: CommunityPropertiesComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/labels', component: CommunityLabelsComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/create', component: CreateOrganisationComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID, component: OrganisationDetailsComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/conformance', component: ConformanceStatementsComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/conformance/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/create', component: CreateConformanceStatementComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/conformance/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/actor/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID, component: ConformanceStatementComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/user/create', component: CreateUserComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/user/:'+Constants.NAVIGATION_PATH_PARAM.USER_ID, component: UserDetailsComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/system/create', component: CreateSystemComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID, component: SystemDetailsComponent },
      { path: 'admin/users/community/:'+Constants.NAVIGATION_PATH_PARAM.COMMUNITY_ID+'/organisation/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/test/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/:actor_id/execute', component: TestExecutionComponent },
      { path: 'admin/export', component: ExportComponent },
      { path: 'admin/import', component: ImportComponent },
      // System administration
      { path: 'admin/system', component: SystemAdministrationComponent },
      { path: 'admin/system/admin/create' , component: CreateAdminComponent },
      { path: 'admin/system/admin/:'+Constants.NAVIGATION_PATH_PARAM.USER_ID , component: AdminDetailsComponent },
      { path: 'admin/system/pages/create', component: CreateLandingPageComponent, resolve: { base: LandingPageResolver } },
      { path: 'admin/system/pages/:'+Constants.NAVIGATION_PATH_PARAM.LANDING_PAGE_ID, component: LandingPageDetailsComponent },
      { path: 'admin/system/notices/create', component: CreateLegalNoticeComponent, resolve: { base: LegalNoticeResolver } },
      { path: 'admin/system/notices/:'+Constants.NAVIGATION_PATH_PARAM.LEGAL_NOTICE_ID, component: LegalNoticeDetailsComponent },
      { path: 'admin/system/errortemplates/create', component: CreateErrorTemplateComponent, resolve: { base: ErrorTemplateResolver } },
      { path: 'admin/system/errortemplates/:'+Constants.NAVIGATION_PATH_PARAM.ERROR_TEMPLATE_ID, component: ErrorTemplateDetailsComponent },

      // My organisation
      { path: 'organisation/conformance/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID, component: ConformanceStatementsComponent },
      { path: 'organisation/conformance/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/create', component: CreateConformanceStatementComponent },
      { path: 'organisation/conformance/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/system/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/actor/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID, component: ConformanceStatementComponent },
      { path: 'organisation/tests/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID, component: OrganisationTestsComponent },
      { path: 'organisation/test/:'+Constants.NAVIGATION_PATH_PARAM.ORGANISATION_ID+'/:'+Constants.NAVIGATION_PATH_PARAM.SYSTEM_ID+'/:'+Constants.NAVIGATION_PATH_PARAM.ACTOR_ID+'/execute', component: TestExecutionComponent }
    ]
  }
]

@NgModule({
  imports: [RouterModule.forRoot(routes, {useHash: true, paramsInheritanceStrategy: 'always', onSameUrlNavigation: 'reload'})],
  exports: [RouterModule]
})
export class AppRoutingModule { }

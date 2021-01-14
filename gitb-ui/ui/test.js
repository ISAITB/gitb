var jsRoutes = {}; (function(_root){
var _nS = function(c,f,b){var e=c.split(f||"."),g=b||_root,d,a;for(d=0,a=e.length;d<a;d++){g=g[e[d]]=g[e[d]]||{}}return g}
var _qS = function(items){var qs = ''; for(var i=0;i<items.length;i++) {if(items[i]) qs += (qs ? '&' : '') + items[i]}; return qs ? ('?' + qs) : ''}
var _s = function(p,s){return p+((s===true||(s&&s.secure))?'s':'')+'://'}
var _wA = function(r) {
    return {
        ajax: function(c){
            c=c||{};
            c.url=r.url;
            c.type=r.method;
            return jQuery.ajax(c)
        }, 
        method:r.method,
        type:r.method,
        url:r.url,
        absoluteURL: function(s){ return _s('http',s)+'localhost/gitb'+r.url},webSocketURL: function(s){return _s('wss',s)+'localhost/gitb'+r.url}}}
_nS('controllers.Assets'); _root['controllers']['Assets']['at'] = 
        function(file1) {
          return _wA({method:"GET", url:"/" + "assets/" + (function(k,v) {return v})("file", file1)})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['ownOrganisationHasTests'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/vendor/organisation/hasTests"})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['checkOrganisationParameterValues'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/organizations/orgparam/check/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['getOrganisationParameterValues'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/organizations/orgparam/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['updateOrganisationParameterValues'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/organizations/orgparam/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['getOwnOrganisationParameterValues'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/organizations/own/orgparam"})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['getOrganizationBySystemId'] = 
        function(system_id0) {
          return _wA({method:"GET", url:"/" + "api/organizations/system/" + encodeURIComponent((function(k,v) {return v})("system_id", system_id0))})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['getOrganizationById'] = 
        function(org_id0) {
          return _wA({method:"GET", url:"/" + "api/organizations/" + encodeURIComponent((function(k,v) {return v})("org_id", org_id0))})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['getOrganizationsByCommunity'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/communities/organizations/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['createOrganization'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/organizations/create"})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['getOrganizations'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/organizations/all"})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['deleteOrganization'] = 
        function(id0) {
          return _wA({method:"DELETE", url:"/" + "api/organizations/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.OrganizationService'); _root['controllers']['OrganizationService']['updateOrganization'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/organizations/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.EndPointService'); _root['controllers']['EndPointService']['deleteEndPoint'] = 
        function(endpoint_id0) {
          return _wA({method:"DELETE", url:"/" + "api/endpoints/" + encodeURIComponent((function(k,v) {return v})("endpoint_id", endpoint_id0))})
        }
      ;
_nS('controllers.EndPointService'); _root['controllers']['EndPointService']['updateEndPoint'] = 
        function(endpoint_id0) {
          return _wA({method:"POST", url:"/" + "api/endpoints/" + encodeURIComponent((function(k,v) {return v})("endpoint_id", endpoint_id0))})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getDomainOfSpecification'] = 
        function(spec_id0) {
          return _wA({method:"GET", url:"/" + "api/specDomain" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("spec_id", spec_id0)])})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getConformanceStatusForTestSuite'] = 
        function(actor_id0,sut_id1,testsuite_id2) {
          return _wA({method:"GET", url:"/" + "api/actors/" + encodeURIComponent((function(k,v) {return v})("actor_id", actor_id0)) + "/conformance/" + encodeURIComponent((function(k,v) {return v})("sut_id", sut_id1)) + "/" + encodeURIComponent((function(k,v) {return v})("testsuite_id", testsuite_id2))})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getConformanceOverview'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/domains/conformance"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['resolvePendingTestSuites'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/specs/deploy/resolve"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getDomainsForSystem'] = 
        function(systemId0) {
          return _wA({method:"GET", url:"/" + "api/domains/system" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("systemId", systemId0)])})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getDocumentationForPreview'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/testdocumentation/"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getEndpointsForActor'] = 
        function(actor_id0) {
          return _wA({method:"GET", url:"/" + "api/actors/" + encodeURIComponent((function(k,v) {return v})("actor_id", actor_id0)) + "/endpoints"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getDomainParametersOfCommunity'] = 
        function(community_id0) {
          return _wA({method:"GET", url:"/" + "api/communities/" + encodeURIComponent((function(k,v) {return v})("community_id", community_id0)) + "/domainParameters"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getTestCaseDocumentation'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/testcases/" + encodeURIComponent((function(k,v) {return v})("id", id0)) + "/documentation"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['deleteObsoleteTestResultsForSystem'] = 
        function() {
          return _wA({method:"DELETE", url:"/" + "api/testresults/obsolete/system"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['deleteDomainParameter'] = 
        function(domain_id0,domain_parameter_id1) {
          return _wA({method:"DELETE", url:"/" + "api/domains/" + encodeURIComponent((function(k,v) {return v})("domain_id", domain_id0)) + "/parameters/" + encodeURIComponent((function(k,v) {return v})("domain_parameter_id", domain_parameter_id1))})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['createDomainParameter'] = 
        function(domain_id0) {
          return _wA({method:"POST", url:"/" + "api/domains/" + encodeURIComponent((function(k,v) {return v})("domain_id", domain_id0)) + "/parameters"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['createSpecification'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/specs"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getConformanceStatus'] = 
        function(actor_id0,sut_id1) {
          return _wA({method:"GET", url:"/" + "api/actors/" + encodeURIComponent((function(k,v) {return v})("actor_id", actor_id0)) + "/conformance/" + encodeURIComponent((function(k,v) {return v})("sut_id", sut_id1))})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['updateDomainParameter'] = 
        function(domain_id0,domain_parameter_id1) {
          return _wA({method:"POST", url:"/" + "api/domains/" + encodeURIComponent((function(k,v) {return v})("domain_id", domain_id0)) + "/parameters/" + encodeURIComponent((function(k,v) {return v})("domain_parameter_id", domain_parameter_id1))})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getTestSuiteTestCase'] = 
        function(testCaseId0) {
          return _wA({method:"GET", url:"/" + "api/testcases/" + encodeURIComponent((function(k,v) {return v})("testCaseId", testCaseId0))})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getTestSuiteDocumentation'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/testsuites/" + encodeURIComponent((function(k,v) {return v})("id", id0)) + "/documentation"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getDomainParameters'] = 
        function(domain_id0) {
          return _wA({method:"GET", url:"/" + "api/domains/" + encodeURIComponent((function(k,v) {return v})("domain_id", domain_id0)) + "/parameters"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['deployTestSuiteToSpecifications'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/specs/deploy"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['deleteAllObsoleteTestResults'] = 
        function() {
          return _wA({method:"DELETE", url:"/" + "api/testresults/obsolete/all"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getConformanceCertificateSettings'] = 
        function(community_id0) {
          return _wA({method:"GET", url:"/" + "api/conformancecertificate" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("community_id", community_id0)])})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getSystemConfigurations'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/endpoints/systemConfig"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['testKeystoreSettings'] = 
        function(community_id0) {
          return _wA({method:"POST", url:"/" + "api/conformancecertificate/test" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("community_id", community_id0)])})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getSpecsForSystem'] = 
        function(systemId0) {
          return _wA({method:"GET", url:"/" + "api/specs/system" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("systemId", systemId0)])})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getSpecTestSuites'] = 
        function(spec_id0) {
          return _wA({method:"GET", url:"/" + "api/specs/" + encodeURIComponent((function(k,v) {return v})("spec_id", spec_id0)) + "/suites"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['createParameter'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/parameters"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['deleteDomain'] = 
        function(domain_id0) {
          return _wA({method:"DELETE", url:"/" + "api/domains/" + encodeURIComponent((function(k,v) {return v})("domain_id", domain_id0))})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['createEndpoint'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/endpoints"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getCommunityDomain'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/domains/community"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getActorsForDomain'] = 
        function(domainId0) {
          return _wA({method:"GET", url:"/" + "api/actors/domain" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("domainId", domainId0)])})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getDomainSpecs'] = 
        function(domain_id0) {
          return _wA({method:"GET", url:"/" + "api/domains/" + encodeURIComponent((function(k,v) {return v})("domain_id", domain_id0)) + "/specs"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['deleteTestResults'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/testresults"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getDomainParameter'] = 
        function(domain_id0,domain_parameter_id1) {
          return _wA({method:"GET", url:"/" + "api/domains/" + encodeURIComponent((function(k,v) {return v})("domain_id", domain_id0)) + "/parameters/" + encodeURIComponent((function(k,v) {return v})("domain_parameter_id", domain_parameter_id1))})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getSpecActors'] = 
        function(spec_id0) {
          return _wA({method:"GET", url:"/" + "api/specs/" + encodeURIComponent((function(k,v) {return v})("spec_id", spec_id0)) + "/actors"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getActors'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/actors"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getSpecs'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/specs"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['createActor'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/actors"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['updateDomain'] = 
        function(domain_id0) {
          return _wA({method:"POST", url:"/" + "api/domains/" + encodeURIComponent((function(k,v) {return v})("domain_id", domain_id0))})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getActorsForSystem'] = 
        function(systemId0) {
          return _wA({method:"GET", url:"/" + "api/actors/system" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("systemId", systemId0)])})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['createDomain'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/domains"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getEndpoints'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/endpoints"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['deleteObsoleteTestResultsForCommunity'] = 
        function() {
          return _wA({method:"DELETE", url:"/" + "api/testresults/obsolete/community"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['updateConformanceCertificateSettings'] = 
        function(community_id0) {
          return _wA({method:"POST", url:"/" + "api/conformancecertificate" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("community_id", community_id0)])})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['getDomains'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/domains"})
        }
      ;
_nS('controllers.ConformanceService'); _root['controllers']['ConformanceService']['checkConfigurations'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/endpoints/checkConfig"})
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['getUsersByOrganization'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/organizations/users" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("id", id0)])})
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['getSystemAdministrators'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/admins/all"})
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['updateSystemAdminProfile'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/admins/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['createCommunityAdmin'] = 
        function() {
        
          if (true) {
            return _wA({method:"POST", url:"/" + "api/communities/admins/create"})
          }
        
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['updateCommunityAdminProfile'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/admins/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['getCommunityAdministrators'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/communities/admins/all"})
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['updateUserProfile'] = 
        function(user_id0) {
          return _wA({method:"POST", url:"/" + "api/users/" + encodeURIComponent((function(k,v) {return v})("user_id", user_id0))})
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['createSystemAdmin'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/admins/create"})
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['getUserById'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/admins/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['deleteAdmin'] = 
        function(id0) {
          return _wA({method:"DELETE", url:"/" + "api/admins/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['deleteVendorUser'] = 
        function(user_id0) {
          return _wA({method:"DELETE", url:"/" + "api/users/" + encodeURIComponent((function(k,v) {return v})("user_id", user_id0))})
        }
      ;
_nS('controllers.UserService'); _root['controllers']['UserService']['createUser'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/organizations/" + encodeURIComponent((function(k,v) {return v})("id", id0)) + "/users/create"})
        }
      ;
_nS('controllers.ReportService'); _root['controllers']['ReportService']['getActiveTestResults'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/dashboard/active"})
        }
      ;
_nS('controllers.ReportService'); _root['controllers']['ReportService']['getFinishedTestResults'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/dashboard/finished"})
        }
      ;
_nS('controllers.ReportService'); _root['controllers']['ReportService']['getSystemActiveTestResults'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/reports/active"})
        }
      ;
_nS('controllers.ReportService'); _root['controllers']['ReportService']['getTestResultOfSession'] = 
        function(session_id0) {
          return _wA({method:"GET", url:"/" + "api/reports/" + encodeURIComponent((function(k,v) {return v})("session_id", session_id0))})
        }
      ;
_nS('controllers.ReportService'); _root['controllers']['ReportService']['getTestStepResults'] = 
        function(session_id0) {
          return _wA({method:"GET", url:"/" + "api/reports/" + encodeURIComponent((function(k,v) {return v})("session_id", session_id0)) + "/steps"})
        }
      ;
_nS('controllers.ReportService'); _root['controllers']['ReportService']['getTestResults'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/reports"})
        }
      ;
_nS('controllers.ReportService'); _root['controllers']['ReportService']['createTestReport'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/reports/create"})
        }
      ;
_nS('controllers.TestResultService'); _root['controllers']['TestResultService']['getBinaryMetadata'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/tests/binaryMetadata"})
        }
      ;
_nS('controllers.LegalNoticeService'); _root['controllers']['LegalNoticeService']['getCommunityDefaultLegalNotice'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/notices/default"})
        }
      ;
_nS('controllers.LegalNoticeService'); _root['controllers']['LegalNoticeService']['getTestBedDefaultLegalNotice'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/notices/tbdefault"})
        }
      ;
_nS('controllers.LegalNoticeService'); _root['controllers']['LegalNoticeService']['getLegalNoticesByCommunity'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/communities/notices/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.LegalNoticeService'); _root['controllers']['LegalNoticeService']['updateLegalNotice'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/notices/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.LegalNoticeService'); _root['controllers']['LegalNoticeService']['getLegalNoticeById'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/notices/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.LegalNoticeService'); _root['controllers']['LegalNoticeService']['deleteLegalNotice'] = 
        function(id0) {
          return _wA({method:"DELETE", url:"/" + "api/notices/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.LegalNoticeService'); _root['controllers']['LegalNoticeService']['createLegalNotice'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/communities/notices/create"})
        }
      ;
_nS('controllers.ErrorTemplateService'); _root['controllers']['ErrorTemplateService']['getCommunityDefaultErrorTemplate'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/errortemplates/default"})
        }
      ;
_nS('controllers.ErrorTemplateService'); _root['controllers']['ErrorTemplateService']['getErrorTemplateById'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/errortemplates/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.ErrorTemplateService'); _root['controllers']['ErrorTemplateService']['deleteErrorTemplate'] = 
        function(id0) {
          return _wA({method:"DELETE", url:"/" + "api/errortemplates/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.ErrorTemplateService'); _root['controllers']['ErrorTemplateService']['updateErrorTemplate'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/errortemplates/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.ErrorTemplateService'); _root['controllers']['ErrorTemplateService']['createErrorTemplate'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/communities/errortemplates/create"})
        }
      ;
_nS('controllers.ErrorTemplateService'); _root['controllers']['ErrorTemplateService']['getErrorTemplatesByCommunity'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/communities/errortemplates/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['getSelfRegistrationOptions'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/user/selfreg"})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['createOrganisationParameter'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/communities/orgparam/create"})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['getOrganisationParameters'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/communities/orgparam/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['orderSystemParameters'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/sysparam/order/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['deleteOrganisationParameter'] = 
        function(id0) {
          return _wA({method:"DELETE", url:"/" + "api/communities/orgparam/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['deleteSystemParameter'] = 
        function(id0) {
          return _wA({method:"DELETE", url:"/" + "api/communities/sysparam/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['orderOrganisationParameters'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/orgparam/order/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['createSystemParameter'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/communities/sysparam/create"})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['updateSystemParameter'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/sysparam/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['getSystemParameters'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/communities/sysparam/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['updateOrganisationParameter'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/orgparam/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['getUserCommunity'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/user/community"})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['selfRegister'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/user/selfreg"})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['getCommunityLabels'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/communities/labels/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['deleteCommunity'] = 
        function(id0) {
          return _wA({method:"DELETE", url:"/" + "api/communities/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['getCommunities'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/communities/all"})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['createCommunity'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/communities/create"})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['updateCommunity'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['getCommunityById'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/communities/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.CommunityService'); _root['controllers']['CommunityService']['setCommunityLabels'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/labels/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.ActorService'); _root['controllers']['ActorService']['updateActor'] = 
        function(actor_id0) {
          return _wA({method:"POST", url:"/" + "api/actors/" + encodeURIComponent((function(k,v) {return v})("actor_id", actor_id0))})
        }
      ;
_nS('controllers.ActorService'); _root['controllers']['ActorService']['deleteActor'] = 
        function(actor_id0) {
          return _wA({method:"DELETE", url:"/" + "api/actors/" + encodeURIComponent((function(k,v) {return v})("actor_id", actor_id0))})
        }
      ;
_nS('controllers.TestService'); _root['controllers']['TestService']['getActorDefinitions'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/actors/definition"})
        }
      ;
_nS('controllers.TestService'); _root['controllers']['TestService']['initiatePreliminary'] = 
        function(session_id0) {
          return _wA({method:"POST", url:"/" + "api/tests/" + encodeURIComponent((function(k,v) {return v})("session_id", session_id0)) + "/preliminary"})
        }
      ;
_nS('controllers.TestService'); _root['controllers']['TestService']['startHeadlessTestSessions'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/tests/startHeadless"})
        }
      ;
_nS('controllers.TestService'); _root['controllers']['TestService']['getTestCaseDefinition'] = 
        function(test_id0) {
          return _wA({method:"GET", url:"/" + "api/tests/" + encodeURIComponent((function(k,v) {return v})("test_id", test_id0)) + "/definition"})
        }
      ;
_nS('controllers.TestService'); _root['controllers']['TestService']['initiate'] = 
        function(test_id0) {
          return _wA({method:"POST", url:"/" + "api/tests/" + encodeURIComponent((function(k,v) {return v})("test_id", test_id0)) + "/initiate"})
        }
      ;
_nS('controllers.TestService'); _root['controllers']['TestService']['provideInput'] = 
        function(session_id0) {
          return _wA({method:"POST", url:"/" + "api/tests/" + encodeURIComponent((function(k,v) {return v})("session_id", session_id0)) + "/provide"})
        }
      ;
_nS('controllers.TestService'); _root['controllers']['TestService']['start'] = 
        function(session_id0) {
          return _wA({method:"POST", url:"/" + "api/tests/" + encodeURIComponent((function(k,v) {return v})("session_id", session_id0)) + "/start"})
        }
      ;
_nS('controllers.TestService'); _root['controllers']['TestService']['stop'] = 
        function(session_id0) {
          return _wA({method:"POST", url:"/" + "api/tests/" + encodeURIComponent((function(k,v) {return v})("session_id", session_id0)) + "/stop"})
        }
      ;
_nS('controllers.TestService'); _root['controllers']['TestService']['configure'] = 
        function(session_id0) {
          return _wA({method:"POST", url:"/" + "api/tests/" + encodeURIComponent((function(k,v) {return v})("session_id", session_id0)) + "/configure"})
        }
      ;
_nS('controllers.TestService'); _root['controllers']['TestService']['restart'] = 
        function(session_id0) {
          return _wA({method:"POST", url:"/" + "api/tests/" + encodeURIComponent((function(k,v) {return v})("session_id", session_id0)) + "/restart"})
        }
      ;
_nS('controllers.LandingPageService'); _root['controllers']['LandingPageService']['getCommunityDefaultLandingPage'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/pages/default"})
        }
      ;
_nS('controllers.LandingPageService'); _root['controllers']['LandingPageService']['getLandingPagesByCommunity'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/communities/pages/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.LandingPageService'); _root['controllers']['LandingPageService']['getLandingPageById'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/pages/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.LandingPageService'); _root['controllers']['LandingPageService']['deleteLandingPage'] = 
        function(id0) {
          return _wA({method:"DELETE", url:"/" + "api/pages/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.LandingPageService'); _root['controllers']['LandingPageService']['updateLandingPage'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/pages/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.LandingPageService'); _root['controllers']['LandingPageService']['createLandingPage'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/communities/pages/create"})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['getTestCasesForCommunity'] = 
        function(communityId0) {
          return _wA({method:"GET", url:"/" + "api/dashboard/tests/community" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("communityId", communityId0)])})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['getTestCasesForSystem'] = 
        function(systemId0) {
          return _wA({method:"GET", url:"/" + "api/dashboard/tests/system" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("systemId", systemId0)])})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['uploadCommunityExport'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/import/preview/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['exportConformanceCertificateReport'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/repository/export/certificate"})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['getTestSuiteResource'] = 
        function(test_id0,file1) {
          return _wA({method:"GET", url:"/" + "api/repository/resource/" + encodeURIComponent((function(k,v) {return v})("test_id", test_id0)) + "/" + (function(k,v) {return v})("file", file1)})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['exportTestCaseReport'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/repository/export/testcase"})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['exportTestStepReport'] = 
        function(session_id0,path1) {
          return _wA({method:"GET", url:"/" + "api/repository/export/teststep/" + encodeURIComponent((function(k,v) {return v})("session_id", session_id0)) + "/" + (function(k,v) {return v})("path", path1)})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['exportConformanceStatementReport'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/repository/export/conformance"})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['getTestCaseDefinition'] = 
        function(test_id0) {
          return _wA({method:"GET", url:"/" + "api/repository/tests/" + encodeURIComponent((function(k,v) {return v})("test_id", test_id0)) + "/definition"})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['cancelCommunityImport'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/import/cancel/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['confirmCommunityImportTestBedAdmin'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/import/confirm/admin/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['confirmDomainImportCommunityAdmin'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/domains/import/confirm/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['confirmDomainImportTestBedAdmin'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/domains/import/confirm/admin/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['getAllTestCases'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/dashboard/tests/all"})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['exportDomain'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/domains/export/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['applySandboxData'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/initdata"})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['getTestStepReport'] = 
        function(session_id0,file1) {
          return _wA({method:"GET", url:"/" + "api/repository/reports/teststep/" + encodeURIComponent((function(k,v) {return v})("session_id", session_id0)) + "/" + (function(k,v) {return v})("file", file1)})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['uploadDomainExport'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/domains/import/preview/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['exportCommunity'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/export/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['cancelDomainImport'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/domains/import/cancel/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['exportOwnConformanceCertificateReport'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/repository/exportOwn/certificate"})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['confirmCommunityImportCommunityAdmin'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/communities/import/confirm/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.RepositoryService'); _root['controllers']['RepositoryService']['exportDemoConformanceCertificateReport'] = 
        function(community_id0) {
          return _wA({method:"POST", url:"/" + "api/repository/export/certificatedemo" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("community_id", community_id0)])})
        }
      ;
_nS('controllers.Application'); _root['controllers']['Application']['javascriptRoutes'] = 
        function() {
          return _wA({method:"GET", url:"/" + "assets/javascripts/routes"})
        }
      ;
_nS('controllers.Application'); _root['controllers']['Application']['preFlight'] = 
        function(all0) {
          return _wA({method:"OPTIONS", url:"/" + (function(k,v) {return v})("all", all0)})
        }
      ;
_nS('controllers.Application'); _root['controllers']['Application']['index'] = 
        function() {
          return _wA({method:"GET", url:"/"})
        }
      ;
_nS('controllers.Application'); _root['controllers']['Application']['app'] = 
        function() {
          return _wA({method:"GET", url:"/" + "app"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['checkEmailOfSystemAdmin'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/check/sysadminemail"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['getUserFunctionalAccounts'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/sso/accounts"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['checkEmailOfCommunityAdmin'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/check/comadminemail"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['checkEmailOfOrganisationMember'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/check/memberemail"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['checkEmailOfOrganisationUser'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/check/orguseremail"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['linkFunctionalAccount'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/sso/linkFunctionalAccount"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['getUserUnlinkedFunctionalAccounts'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/sso/unlinkedAccounts"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['disconnectFunctionalAccount'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/user/disconnect"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['selectFunctionalAccount'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/sso/select"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['migrateFunctionalAccount'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/sso/migrate"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['replaceOnetimePassword'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/oauth/replacePassword"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['access_token'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/oauth/access_token"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['checkEmail'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/check/email"})
        }
      ;
_nS('controllers.AuthenticationService'); _root['controllers']['AuthenticationService']['logout'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/oauth/logout"})
        }
      ;
_nS('controllers.TriggerService'); _root['controllers']['TriggerService']['testTriggerEndpoint'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/trigger/test"})
        }
      ;
_nS('controllers.TriggerService'); _root['controllers']['TriggerService']['getTriggersByCommunity'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/communities/triggers/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.TriggerService'); _root['controllers']['TriggerService']['deleteTrigger'] = 
        function(id0) {
          return _wA({method:"DELETE", url:"/" + "api/triggers/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.TriggerService'); _root['controllers']['TriggerService']['getTriggerById'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/triggers/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.TriggerService'); _root['controllers']['TriggerService']['updateTrigger'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/triggers/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.TriggerService'); _root['controllers']['TriggerService']['createTrigger'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/communities/triggers/create"})
        }
      ;
_nS('controllers.TriggerService'); _root['controllers']['TriggerService']['clearStatus'] = 
        function(id0) {
          return _wA({method:"POST", url:"/" + "api/triggers/" + encodeURIComponent((function(k,v) {return v})("id", id0)) + "/clearStatus"})
        }
      ;
_nS('controllers.TriggerService'); _root['controllers']['TriggerService']['previewTriggerCall'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/trigger/previewTriggerCall"})
        }
      ;
_nS('controllers.AccountService'); _root['controllers']['AccountService']['updateVendorProfile'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/vendor/profile"})
        }
      ;
_nS('controllers.AccountService'); _root['controllers']['AccountService']['getUserProfile'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/user/profile"})
        }
      ;
_nS('controllers.AccountService'); _root['controllers']['AccountService']['getVendorProfile'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/vendor/profile"})
        }
      ;
_nS('controllers.AccountService'); _root['controllers']['AccountService']['getVendorUsers'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/vendor/users"})
        }
      ;
_nS('controllers.AccountService'); _root['controllers']['AccountService']['registerUser'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/user/register"})
        }
      ;
_nS('controllers.AccountService'); _root['controllers']['AccountService']['updateUserProfile'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/user/profile"})
        }
      ;
_nS('controllers.AccountService'); _root['controllers']['AccountService']['submitFeedback'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/user/feedback"})
        }
      ;
_nS('controllers.AccountService'); _root['controllers']['AccountService']['getConfiguration'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/app/configuration"})
        }
      ;
_nS('controllers.ParameterService'); _root['controllers']['ParameterService']['updateParameter'] = 
        function(parameter_id0) {
          return _wA({method:"POST", url:"/" + "api/parameters/" + encodeURIComponent((function(k,v) {return v})("parameter_id", parameter_id0))})
        }
      ;
_nS('controllers.ParameterService'); _root['controllers']['ParameterService']['deleteParameter'] = 
        function(parameter_id0) {
          return _wA({method:"DELETE", url:"/" + "api/parameters/" + encodeURIComponent((function(k,v) {return v})("parameter_id", parameter_id0))})
        }
      ;
_nS('controllers.ParameterService'); _root['controllers']['ParameterService']['orderParameters'] = 
        function(endpoint_id0) {
          return _wA({method:"POST", url:"/" + "api/parameters/order/" + encodeURIComponent((function(k,v) {return v})("endpoint_id", endpoint_id0))})
        }
      ;
_nS('controllers.SpecificationService'); _root['controllers']['SpecificationService']['deleteSpecification'] = 
        function(spec_id0) {
          return _wA({method:"DELETE", url:"/" + "api/specs/" + encodeURIComponent((function(k,v) {return v})("spec_id", spec_id0))})
        }
      ;
_nS('controllers.SpecificationService'); _root['controllers']['SpecificationService']['updateSpecification'] = 
        function(spec_id0) {
          return _wA({method:"POST", url:"/" + "api/specs/" + encodeURIComponent((function(k,v) {return v})("spec_id", spec_id0))})
        }
      ;
_nS('controllers.WebSocketService'); _root['controllers']['WebSocketService']['socket'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/ws"})
        }
      ;
_nS('controllers.TestSuiteService'); _root['controllers']['TestSuiteService']['updateTestSuiteMetadata'] = 
        function(test_suite_id0) {
          return _wA({method:"POST", url:"/" + "api/testsuites/" + encodeURIComponent((function(k,v) {return v})("test_suite_id", test_suite_id0))})
        }
      ;
_nS('controllers.TestSuiteService'); _root['controllers']['TestSuiteService']['updateTestCaseMetadata'] = 
        function(test_case_id0) {
          return _wA({method:"POST", url:"/" + "api/fulltestcases/" + encodeURIComponent((function(k,v) {return v})("test_case_id", test_case_id0))})
        }
      ;
_nS('controllers.TestSuiteService'); _root['controllers']['TestSuiteService']['getAllTestSuitesWithTestCases'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/dashboard/suites/all"})
        }
      ;
_nS('controllers.TestSuiteService'); _root['controllers']['TestSuiteService']['getTestSuiteWithTestCases'] = 
        function(test_suite_id0) {
          return _wA({method:"GET", url:"/" + "api/testsuites/" + encodeURIComponent((function(k,v) {return v})("test_suite_id", test_suite_id0))})
        }
      ;
_nS('controllers.TestSuiteService'); _root['controllers']['TestSuiteService']['getTestCase'] = 
        function(test_case_id0) {
          return _wA({method:"GET", url:"/" + "api/fulltestcases/" + encodeURIComponent((function(k,v) {return v})("test_case_id", test_case_id0))})
        }
      ;
_nS('controllers.TestSuiteService'); _root['controllers']['TestSuiteService']['downloadTestSuite'] = 
        function(suite_id0) {
          return _wA({method:"GET", url:"/" + "api/suite/" + encodeURIComponent((function(k,v) {return v})("suite_id", suite_id0))})
        }
      ;
_nS('controllers.TestSuiteService'); _root['controllers']['TestSuiteService']['undeployTestSuite'] = 
        function(suite_id0) {
          return _wA({method:"DELETE", url:"/" + "api/suite/" + encodeURIComponent((function(k,v) {return v})("suite_id", suite_id0)) + "/undeploy"})
        }
      ;
_nS('controllers.TestSuiteService'); _root['controllers']['TestSuiteService']['getTestSuitesWithTestCasesForCommunity'] = 
        function(communityId0) {
          return _wA({method:"GET", url:"/" + "api/dashboard/suites/community" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("communityId", communityId0)])})
        }
      ;
_nS('controllers.TestSuiteService'); _root['controllers']['TestSuiteService']['getTestSuitesWithTestCasesForSystem'] = 
        function(systemId0) {
          return _wA({method:"GET", url:"/" + "api/dashboard/suites/system" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("systemId", systemId0)])})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['checkSystemParameterValues'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/suts/sysparam/check/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['updateSystemProfile'] = 
        function(sut_id0) {
          return _wA({method:"POST", url:"/" + "api/suts/" + encodeURIComponent((function(k,v) {return v})("sut_id", sut_id0)) + "/profile"})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['defineConformanceStatement'] = 
        function(sut_id0) {
          return _wA({method:"POST", url:"/" + "api/suts/" + encodeURIComponent((function(k,v) {return v})("sut_id", sut_id0)) + "/conformance"})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['getSystemParameterValues'] = 
        function(id0) {
          return _wA({method:"GET", url:"/" + "api/suts/sysparam/" + encodeURIComponent((function(k,v) {return v})("id", id0))})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['getSystemsByCommunity'] = 
        function(communityId0) {
          return _wA({method:"GET", url:"/" + "api/vendor/systems/community" + _qS([(function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("communityId", communityId0)])})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['getConformanceStatements'] = 
        function(sut_id0) {
          return _wA({method:"GET", url:"/" + "api/suts/" + encodeURIComponent((function(k,v) {return v})("sut_id", sut_id0)) + "/conformance"})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['registerSystemWithOrganization'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/vendor/systems/register"})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['getSystemsByOrganization'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/vendor/systems"})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['deleteConformanceStatement'] = 
        function(sut_id0) {
          return _wA({method:"DELETE", url:"/" + "api/suts/" + encodeURIComponent((function(k,v) {return v})("sut_id", sut_id0)) + "/conformance"})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['getConfigurationsWithEndpointIds'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/endpoints/config"})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['saveEndpointConfiguration'] = 
        function(endpoint_id0) {
          return _wA({method:"POST", url:"/" + "api/endpoints/" + encodeURIComponent((function(k,v) {return v})("endpoint_id", endpoint_id0)) + "/config"})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['getEndpointConfigurations'] = 
        function(endpoint_id0) {
          return _wA({method:"GET", url:"/" + "api/endpoints/" + encodeURIComponent((function(k,v) {return v})("endpoint_id", endpoint_id0)) + "/config"})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['deleteEndpointConfiguration'] = 
        function(endpoint_id0) {
          return _wA({method:"DELETE", url:"/" + "api/endpoints/" + encodeURIComponent((function(k,v) {return v})("endpoint_id", endpoint_id0)) + "/config"})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['deleteSystem'] = 
        function(sut_id0) {
          return _wA({method:"DELETE", url:"/" + "api/suts/" + encodeURIComponent((function(k,v) {return v})("sut_id", sut_id0))})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['getSystems'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/dashboard/systems"})
        }
      ;
_nS('controllers.SystemService'); _root['controllers']['SystemService']['getSystemProfile'] = 
        function(sut_id0) {
          return _wA({method:"GET", url:"/" + "api/suts/" + encodeURIComponent((function(k,v) {return v})("sut_id", sut_id0))})
        }
      ;
_nS('controllers.SystemConfigurationService'); _root['controllers']['SystemConfigurationService']['updateSessionAliveTime'] = 
        function() {
          return _wA({method:"POST", url:"/" + "api/dashboard/config"})
        }
      ;
_nS('controllers.SystemConfigurationService'); _root['controllers']['SystemConfigurationService']['getSessionAliveTime'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/dashboard/config"})
        }
      ;
_nS('controllers.SystemConfigurationService'); _root['controllers']['SystemConfigurationService']['getCssForTheme'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/theme/css"})
        }
      ;
_nS('controllers.SystemConfigurationService'); _root['controllers']['SystemConfigurationService']['getLogo'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/theme/logo"})
        }
      ;
_nS('controllers.SystemConfigurationService'); _root['controllers']['SystemConfigurationService']['getFooterLogo'] = 
        function() {
          return _wA({method:"GET", url:"/" + "api/theme/footer"})
        }
      ;
_nS('controllers.SystemConfigurationService'); _root['controllers']['SystemConfigurationService']['getFaviconForTheme'] = 
        function() {
          return _wA({method:"GET", url:"/" + "favicon.ico"})
        }
      ;
})(jsRoutes)
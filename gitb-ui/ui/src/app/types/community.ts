import {Domain} from './domain';
import {EntityWithId} from './entity-with-id';
import {TypedLabelConfig} from './typed-label-config.type';

export interface Community extends EntityWithId {

  sname: string;
  fname: string;
  description?: string;
  selfRegType: number;
  selfRegRestriction: number;
  selfRegToken?: string;
  selfRegTokenHelpText?: string;
  selfRegNotification: boolean;
  interactionNotification: boolean;
  selfRegForceTemplateSelection?: boolean;
  selfRegForceRequiredProperties?: boolean;
  email?: string;
  domain?: Domain;
  domainId?: number;
  allowCertificateDownload: boolean;
  allowSystemManagement: boolean;
  allowStatementManagement: boolean;
  allowPostTestOrganisationUpdates: boolean;
  allowPostTestSystemUpdates: boolean;
  allowPostTestStatementUpdates: boolean;
  allowAutomationApi?: boolean;
  allowCommunityView: boolean;
  apiKey?: string;

  sameDescriptionAsDomain: boolean;
  activeDescription?: string;
  labels?: TypedLabelConfig[];

}

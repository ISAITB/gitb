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

import {Observable} from 'rxjs';
import {CommunityResourceSearchResult} from '../../types/community-resource-search-result';
import {HttpResponse} from '@angular/common/http';
import {FileData} from '../../types/file-data.type';
import {CommunityResourceUploadResult} from '../../types/community-resource-upload-result';

export interface ResourceActions {

  searchResources: (filter: string|undefined, page: number|undefined, limit: number|undefined) => Observable<CommunityResourceSearchResult>
  downloadResources: (filter: string|undefined) => Observable<ArrayBuffer>
  downloadResource: (resourceId: number) => Observable<HttpResponse<ArrayBuffer>>
  deleteResources: (resourceIds: number[]) => Observable<void>
  deleteResource: (resourceId: number) => Observable<void>
  createResource: (name: string, description: string|undefined, file: FileData) => Observable<void>
  updateResource: (resourceId: number, name: string, description: string|undefined, file?: FileData) => Observable<void>
  uploadBulk: (file: FileData, updateMatching?: boolean) => Observable<CommunityResourceUploadResult>
  systemScope: boolean

}

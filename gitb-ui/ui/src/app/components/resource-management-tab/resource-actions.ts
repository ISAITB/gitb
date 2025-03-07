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

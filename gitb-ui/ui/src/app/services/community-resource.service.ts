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

import { Injectable } from '@angular/core';
import {CommunityResourceSearchResult} from '../types/community-resource-search-result';
import {ROUTES} from '../common/global';
import {CommunityResource} from '../types/community-resource';
import {FileData} from '../types/file-data.type';
import {CommunityResourceUploadResult} from '../types/community-resource-upload-result';
import {FileParam} from '../types/file-param.type';
import {HttpResponse} from '@angular/common/http';
import {RestService} from './rest.service';

@Injectable({
  providedIn: 'root'
})
export class CommunityResourceService {

  constructor(private restService: RestService) { }

  getSystemResources() {
    return this.getResourcesInternal(ROUTES.controllers.CommunityResourceService.getSystemResources().url)
  }

  getCommunityResources(communityId: number) {
    return this.getResourcesInternal(ROUTES.controllers.CommunityResourceService.getCommunityResources(communityId).url)
  }

  private getResourcesInternal(url: string) {
    return this.restService.get<CommunityResource[]>({
      path: url,
      authenticate: true
    })
  }

  searchSystemResources(filter: string|undefined, page: number|undefined, limit: number|undefined) {
    return this.searchResourcesInternal(ROUTES.controllers.CommunityResourceService.searchSystemResources().url, filter, page, limit)
  }

  searchCommunityResources(communityId: number, filter: string|undefined, page: number|undefined, limit: number|undefined) {
    return this.searchResourcesInternal(ROUTES.controllers.CommunityResourceService.searchCommunityResources(communityId).url, filter, page, limit)
  }

  private searchResourcesInternal(url: string, filter: string|undefined, page: number|undefined, limit: number|undefined) {
    return this.restService.get<CommunityResourceSearchResult>({
      path: url,
      params: {
        filter: filter,
        page: page,
        limit: limit,
      },
      authenticate: true
    })
  }

  downloadSystemResources(filter: string|undefined) {
    return this.downloadResourcesInternal(ROUTES.controllers.CommunityResourceService.downloadSystemResources().url, filter)
  }

  downloadCommunityResources(communityId: number, filter: string|undefined) {
    return this.downloadResourcesInternal(ROUTES.controllers.CommunityResourceService.downloadCommunityResources(communityId).url, filter)
  }

  private downloadResourcesInternal(url: string, filter: string|undefined) {
    return this.restService.get<ArrayBuffer>({
      path: url,
      params: {
        filter: filter
      },
      authenticate: true,
      arrayBuffer: true
    })
  }

  createSystemResource(name: string, description: string|undefined, file: FileData) {
    return this.createResourceInternal(ROUTES.controllers.CommunityResourceService.createSystemResource().url, name, description, file)
  }

  createCommunityResource(name: string, description: string|undefined, file: FileData, communityId: number) {
    return this.createResourceInternal(ROUTES.controllers.CommunityResourceService.createCommunityResource(communityId).url, name, description, file)
  }

  private createResourceInternal(url: string, name: string, description: string|undefined, file: FileData) {
    return this.restService.put<void>({
      path: url,
      authenticate: true,
      data: {
        name: name,
        description: description
      },
      files: [{
        param: "file",
        data: file.file!
      }]
    })
  }

  uploadSystemResourcesInBulk(file: FileData, updateMatching?: boolean) {
    return this.uploadResourcesInBulkInternal(ROUTES.controllers.CommunityResourceService.uploadSystemResourcesInBulk().url, file, updateMatching)
  }

  uploadCommunityResourcesInBulk(communityId: number, file: FileData, updateMatching?: boolean) {
    return this.uploadResourcesInBulkInternal(ROUTES.controllers.CommunityResourceService.uploadCommunityResourcesInBulk(communityId).url, file, updateMatching)
  }

  private uploadResourcesInBulkInternal(url: string, file: FileData, updateMatching?: boolean) {
    return this.restService.post<CommunityResourceUploadResult>({
      path: url,
      authenticate: true,
      data: {
        // Update matching resources by default
        update: (updateMatching == undefined || updateMatching)
      },
      files: [{
        param: "file",
        data: file.file!
      }]
    })
  }

  updateSystemResource(resourceId: number, name: string, description: string|undefined, file?: FileData) {
    return this.updateResourceInternal(ROUTES.controllers.CommunityResourceService.updateSystemResource(resourceId).url, name, description, file)
  }

  updateCommunityResource(resourceId: number, name: string, description: string|undefined, file?: FileData) {
    return this.updateResourceInternal(ROUTES.controllers.CommunityResourceService.updateCommunityResource(resourceId).url, name, description, file)
  }

  private updateResourceInternal(url: string, name: string, description: string|undefined, file?: FileData) {
    const files: FileParam[] = []
    if (file?.file) {
      files.push({param: "file", data: file.file!})
    }
    return this.restService.post<void>({
      path: url,
      authenticate: true,
      data: {
        name: name,
        description: description
      },
      files: files
    })
  }

  deleteSystemResource(resourceId: number) {
    return this.deleteResourceInternal(ROUTES.controllers.CommunityResourceService.deleteSystemResource(resourceId).url)
  }

  deleteCommunityResource(resourceId: number) {
    return this.deleteResourceInternal(ROUTES.controllers.CommunityResourceService.deleteCommunityResource(resourceId).url)
  }

  private deleteResourceInternal(url: string) {
    return this.restService.delete<void>({
      path: url,
      authenticate: true
    })
  }

  deleteSystemResources(resourceIds: number[]) {
    return this.deleteResourcesInternal(ROUTES.controllers.CommunityResourceService.deleteSystemResources().url, resourceIds)
  }

  deleteCommunityResources(communityId: number, resourceIds: number[]) {
    return this.deleteResourcesInternal(ROUTES.controllers.CommunityResourceService.deleteCommunityResources(communityId).url, resourceIds)
  }

  private deleteResourcesInternal(url: string, resourceIds: number[]) {
    return this.restService.post<void>({
      path: url,
      data: {
        ids: resourceIds.join(',')
      },
      authenticate: true
    })
  }

  downloadSystemResourceById(resourceId: number) {
    return this.downloadResourceByIdInternal(ROUTES.controllers.CommunityResourceService.downloadSystemResourceById(resourceId).url)
  }

  downloadCommunityResourceById(resourceId: number) {
    return this.downloadResourceByIdInternal(ROUTES.controllers.CommunityResourceService.downloadCommunityResourceById(resourceId).url)
  }

  private downloadResourceByIdInternal(url: string) {
    return this.restService.get<HttpResponse<ArrayBuffer>>({
      path: url,
      authenticate: true,
      arrayBuffer: true,
      httpResponse: true
    })
  }

}

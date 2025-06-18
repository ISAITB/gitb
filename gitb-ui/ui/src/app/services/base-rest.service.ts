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

import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Utils } from '../common/utils';
import { HttpRequestConfig } from '../types/http-request-config.type'
import { AuthProviderService } from './auth-provider.service';
import { DataService } from './data.service';

@Injectable({
  providedIn: 'root'
})
export class BaseRestService {

  constructor(
    private http: HttpClient,
    private authProviderService: AuthProviderService,
    private dataService: DataService
  ) { }

  private call<T>(config: HttpRequestConfig, callFn: () => Observable<T>): Observable<T> {
    if (config.authenticate && !this.authProviderService.isAuthenticated()) {
      this.authProviderService.signalLogout({full: true})
      throw 'Signalling logout due to unauthorised call'
    }
    return callFn()
  }

  get<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(config, () => {
      return this.http.get<T>(
        this.dataService.completePath(config.path),
        this.prepareRequestOptions(config)
      )
    })
  }

  delete<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(config, () => {
      return this.http.delete<T>(
        this.dataService.completePath(config.path),
        this.prepareRequestOptions(config)
      )
    })
  }

  put<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(config, () => {
      return this.http.put<T>(
        this.dataService.completePath(config.path),
        this.prepareBody(config),
        this.prepareRequestOptions(config)
      )
    })
  }

  post<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(config, () => {
      return this.http.post<T>(
        this.dataService.completePath(config.path),
        this.prepareBody(config),
        this.prepareRequestOptions(config)
      )
    })
  }

  private prepareBody(config: HttpRequestConfig) {
    // Prepare body
    let body:any
    if (config.files == undefined || config.files.length == 0) {
      if (config.asJSON) {
        body = config.data
      } else {
        body = Utils.objectToFormRequest(config.data).toString()
      }
    } else {
      body = new FormData()
      for (let fileConfig of config.files) {
        body.append(fileConfig.param, fileConfig.data)
      }
      if (config.data != undefined) {
        for (let key in config.data) {
          if (config.data[key] != undefined) {
            body.append(key, config.data[key])
          }
        }
      }
    }
    return body
  }

  private prepareRequestOptions(config?: HttpRequestConfig): {headers: HttpHeaders} {
    const configToUse: any = {}
    // Headers
    if (this.authProviderService.isAuthenticated()) {
      configToUse.headers = Utils.createHttpHeaders(this.authProviderService.accessToken, config)
    } else {
      configToUse.headers = Utils.createHttpHeaders(undefined, config)
    }
    // Query parameters
    if (config) {
      if (config.params) {
        let params = new HttpParams()
        for (let key in config.params) {
          if (config.params[key] != undefined) {
            params = params.set(key, config.params[key])
          }
        }
        configToUse.params = params
      }
      if (config.text !== undefined && config.text) {
        configToUse.responseType = 'text'
      } else if (config.arrayBuffer !== undefined && config.arrayBuffer) {
        configToUse.responseType = 'arraybuffer'
      }
      if (config.httpResponse) {
        configToUse.observe = 'response'
      }
    }
    return configToUse
  }

}

import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Utils } from '../common/utils';
import { HttpRequestConfig } from '../types/http-request-config.type'
import { AuthProviderService } from './auth-provider.service';

@Injectable({
  providedIn: 'root'
})
export class BaseRestService {

  constructor(
    private http: HttpClient,
    private authProviderService: AuthProviderService
  ) { }

  private call<T>(config: HttpRequestConfig, callFn: () => Observable<T>): Observable<T> {
    if (config.authenticate && !this.authProviderService.isAuthenticated()) {
      this.authProviderService.signalLogout({full: true})
    }
    return callFn()
  }

  get<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(config, () => {
      return this.http.get<T>(
        Utils.completePath(config.path), 
        this.prepareRequestOptions(config)
      )      
    })
  }

  delete<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(config, () => {
      return this.http.delete<T>(
        Utils.completePath(config.path), 
        this.prepareRequestOptions(config)
      )      
    })
  }

  post<T>(config: HttpRequestConfig): Observable<T> {
    return this.call<T>(config, () => {
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
      return this.http.post<T>(
        Utils.completePath(config.path), 
        body,
        this.prepareRequestOptions(config)
      )
    })
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
    }
    return configToUse
  }

}

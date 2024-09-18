import { Injectable } from '@angular/core';
import { RestService } from './rest.service'
import { ROUTES } from '../common/global';
import { SystemConfiguration } from '../types/system-configuration';
import { ErrorDescription } from '../types/error-description';
import { Theme } from '../types/theme';
import { FileParam } from '../types/file-param.type';
import { HttpResponse } from '@angular/common/http';
import { EmailSettings } from '../types/email-settings';

@Injectable({
  providedIn: 'root'
})
export class SystemConfigurationService {

  constructor(private restService: RestService) { }

  getConfigurationValues() {
    return this.restService.get<SystemConfiguration[]>({
      path: ROUTES.controllers.SystemConfigurationService.getConfigurationValues().url,
      authenticate: false
    })
  }

  updateConfigurationValue(name: string, value?: string) {
    const data: any = {
      name: name
    }
    if (value !== undefined) {
      data.parameter = value
    }
    return this.restService.post<SystemConfiguration|undefined>({
      path: ROUTES.controllers.SystemConfigurationService.updateConfigurationValue().url,
      data: data,
      authenticate: true
    })
  }

  updateSessionAliveTime(value?: number) {
    const data: any = {}
    if (value !== undefined) {
      data.parameter = value
    }
    return this.restService.post<ErrorDescription|undefined>({
      path: ROUTES.controllers.SystemConfigurationService.updateSessionAliveTime().url,
      data: data,
      authenticate: true
    })
  }

  getThemes() {
    return this.restService.get<Theme[]>({
      path: ROUTES.controllers.SystemConfigurationService.getThemes().url,
      authenticate: true
    })
  }

  getTheme(themeId: number) {
    return this.restService.get<Theme>({
      path: ROUTES.controllers.SystemConfigurationService.getTheme(themeId).url,
      authenticate: true
    })
  }

  previewThemeResource(themeId: number|undefined, resourcePath: string) {
    return this.restService.get<HttpResponse<ArrayBuffer>>({
      path: ROUTES.controllers.SystemConfigurationService.previewThemeResource().url,
      authenticate: true,
      arrayBuffer: true,
      params: {
        id: themeId,
        name: resourcePath
      },
      httpResponse: true
    })    
  }

  deleteTheme(themeId: number) {
    return this.restService.delete<void>({
      path: ROUTES.controllers.SystemConfigurationService.deleteTheme(themeId).url,
      authenticate: true
    })
  }

  private prepareThemeData(theme: Theme): { data: any, files: FileParam[]|undefined } {
    const data = {
      key: theme.key,
      description: theme.description,
      active: theme.active,
      separatorTitleColor: theme.separatorTitleColor,
      modalTitleColor: theme.modalTitleColor,
      tableTitleColor: theme.tableTitleColor,
      cardTitleColor: theme.cardTitleColor,
      pageTitleColor: theme.pageTitleColor,
      headingColor: theme.headingColor,
      tabLinkColor: theme.tabLinkColor,
      footerTextColor: theme.footerTextColor,
      headerBackgroundColor: theme.headerBackgroundColor,
      headerBorderColor: theme.headerBorderColor,
      headerSeparatorColor: theme.headerSeparatorColor,
      footerBackgroundColor: theme.footerBackgroundColor,
      footerBorderColor: theme.footerBorderColor,
      footerLogoDisplay: theme.footerLogoDisplay,
      headerLogoPath: theme.headerLogoPath,
      footerLogoPath: theme.footerLogoPath,
      faviconPath: theme.faviconPath,
      primaryButtonColor: theme.primaryButtonColor,
      primaryButtonLabelColor: theme.primaryButtonLabelColor,
      primaryButtonHoverColor: theme.primaryButtonHoverColor,
      primaryButtonActiveColor: theme.primaryButtonActiveColor,
      secondaryButtonColor: theme.secondaryButtonColor,
      secondaryButtonLabelColor: theme.secondaryButtonLabelColor,
      secondaryButtonHoverColor: theme.secondaryButtonHoverColor,
      secondaryButtonActiveColor: theme.secondaryButtonActiveColor
    }
    let files: FileParam[]|undefined
    if (theme.headerLogoFile?.file || theme.footerLogoFile?.file || theme.faviconFile?.file) {
      files = []
      if (theme.headerLogoFile?.file) {
        files.push({ param: "headerLogoFile", data: theme.headerLogoFile.file })
      }
      if (theme.footerLogoFile?.file) {
        files.push({ param: "footerLogoFile", data: theme.footerLogoFile.file })
      }
      if (theme.faviconFile?.file) {
        files.push({ param: "faviconFile", data: theme.faviconFile.file })
      }
    }
    return { data: data, files: files }
  }

  activateTheme(themeId: number) {
    return this.restService.post<void>({
      path: ROUTES.controllers.SystemConfigurationService.activateTheme(themeId).url,
      authenticate: true
    })
  }

  updateTheme(theme: Theme) {
    const themeData = this.prepareThemeData(theme)
    return this.restService.post<ErrorDescription|void>({
      path: ROUTES.controllers.SystemConfigurationService.updateTheme(theme.id).url,
      authenticate: true,
      data: themeData.data,
      files: themeData.files
    })
  }

  createTheme(theme: Theme, referenceThemeId: number) {
    const themeData = this.prepareThemeData(theme)
    themeData.data.reference = referenceThemeId
    return this.restService.post<ErrorDescription|void>({
      path: ROUTES.controllers.SystemConfigurationService.createTheme().url,
      authenticate: true,
      data: themeData.data,
      files: themeData.files
    })    
  }

  testEmailSettings(settings: EmailSettings, to: string) {
    const data = {
      settings: JSON.stringify(settings),
      to: to
    }
    return this.restService.post<{ success: boolean, messages?: string[] }>({
      path: ROUTES.controllers.SystemConfigurationService.testEmailSettings().url,
      authenticate: true,
      data: data
    })
  }

}

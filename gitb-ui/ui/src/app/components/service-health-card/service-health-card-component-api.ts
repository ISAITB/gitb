import {Observable} from 'rxjs';

export interface ServiceHealthCardComponentApi {

  checkStatus: () => Observable<void>

}

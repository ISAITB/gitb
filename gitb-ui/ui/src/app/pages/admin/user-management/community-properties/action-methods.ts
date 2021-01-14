import { Observable } from "rxjs";
import { Parameter } from "src/app/types/parameter";

export interface ActionMethods {

    create: (parameter: Parameter, communityId: number) => Observable<any>
    update: (parameter: Parameter, communityId: number) => Observable<any>
    delete: (parameterId: number) => Observable<any>
    reload: () => void

}

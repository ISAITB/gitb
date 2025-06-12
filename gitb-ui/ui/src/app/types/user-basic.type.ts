import {EntityWithId} from './entity-with-id';

export interface UserBasic extends EntityWithId {

  id: number,
  email: string

}

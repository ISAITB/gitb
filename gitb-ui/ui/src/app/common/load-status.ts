import { Constants } from "./constants";

export class LoadStatus {

    status: number

    constructor() {
        this.status = Constants.STATUS.PENDING
    }

    pending() {
        this.status = Constants.STATUS.PENDING
    }

    finished() {
        this.status = Constants.STATUS.FINISHED
    }
}

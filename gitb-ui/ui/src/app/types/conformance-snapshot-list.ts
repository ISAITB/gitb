import { ConformanceSnapshot } from "./conformance-snapshot";

export interface ConformanceSnapshotList {

    latest?: string
    snapshots: ConformanceSnapshot[]

}

import { AnyContent } from "../components/diagram/any-content";

export interface UserInteractionInstruction extends AnyContent {

    id: string
    desc?: string
    with?: string

}

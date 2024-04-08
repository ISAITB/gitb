import { UserInteractionInputRequest } from "./user-interaction-input-request";
import { UserInteractionInstruction } from "./user-interaction-instruction";

export interface UserInteractionRequest {

    with?: string
    inputTitle?: string
    instructionOrRequest: UserInteractionInputRequest[]|UserInteractionInstruction[]

}

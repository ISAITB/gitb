/*
 * Copyright (C) 2026 European Union
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

package models

object MergeOperation {
  /** Merge the source organisation (and its systems) into a target organisation. The source is deleted. */
  val MERGE_ORGANISATIONS = "merge_organisations"
  /** Merge systems of the source organisation into other systems of the same organisation. */
  val MERGE_SYSTEMS = "merge_systems"
  /** Move (or merge) systems of the source organisation to a target organisation. The source is kept. */
  val MOVE_SYSTEMS = "move_systems"
}

object MergeSystemActionType {
  /** The system is left unchanged. */
  val KEEP = "keep"
  /** The system is moved unchanged to the target organisation. */
  val MOVE = "move"
  /** The system is merged into another system. */
  val MERGE = "merge"
  /** The system is deleted. */
  val DELETE = "delete"
}

/**
 * The action to take for a system of the source organisation.
 *
 * @param systemId The ID of the system (owned by the source organisation).
 * @param action One of MergeSystemActionType.
 * @param target The ID of the system to merge into (only for the merge action).
 */
case class MergeSystemAction(systemId: Long, action: String, target: Option[Long])

/**
 * A request to merge or move organisations and systems.
 *
 * @param operation One of MergeOperation.
 * @param target The ID of the target organisation (not applicable when merging systems within the source organisation).
 * @param systems The actions to take for the source organisation's systems.
 */
case class MergeRequest(operation: String, target: Option[Long], systems: List[MergeSystemAction])

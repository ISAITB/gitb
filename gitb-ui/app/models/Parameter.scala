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

import models.prerequisites.WithPrerequisite

/**
 * Created by serbay.
 */
case class Parameters(id: Long, name: String, testKey: String, desc: Option[String], use: String, kind: String, adminOnly: Boolean, notForTests:Boolean, hidden: Boolean, allowedValues: Option[String], displayOrder: Short, dependsOn: Option[String], dependsOnValue: Option[String], defaultValue: Option[String], endpoint: Long) {

	def withEndpoint(endpointId:Long, newDisplayOrder: Option[Short]): Parameters = {
		Parameters(id, name, testKey, desc, use, kind, adminOnly, notForTests, hidden, allowedValues, newDisplayOrder.getOrElse(displayOrder), dependsOn, dependsOnValue, defaultValue, endpointId)
	}

}

class ParametersWithValue(_parameter: Parameters, _value: Option[Configs]) extends WithPrerequisite {
	var parameter: Parameters = _parameter
	var value: Option[Configs] = _value

	override def prerequisiteKey(): Option[String] = parameter.dependsOn
	override def prerequisiteValue(): Option[String] = parameter.dependsOnValue
	override def currentKey(): String = parameter.testKey
	override def currentValue(): Option[String] = if (value.isDefined) Some(value.get.value) else None
	override def currentKind(): Option[String] = Some(parameter.kind)
}

/**
 * The kinds ("kind") supported for organisation, system and actor endpoint configuration
 * properties. SIMPLE, MULTILINE_TEXT, CODE and RICH_TEXT are all plain-text kinds that store
 * and transport their value identically (as a string) - they differ only in how the value is
 * captured/rendered in the UI. BINARY and SECRET are handled distinctly (as a file, resp. an
 * encrypted value).
 */
object PropertyKind {

	val SIMPLE = "SIMPLE"
	val BINARY = "BINARY"
	val SECRET = "SECRET"
	val MULTILINE_TEXT = "MULTILINE_TEXT"
	val CODE = "CODE"
	val RICH_TEXT = "RICH_TEXT"
	val RADIO_BUTTON = "RADIO_BUTTON"
	val CHECKBOX = "CHECKBOX"

	private val ALL = Set(SIMPLE, BINARY, SECRET, MULTILINE_TEXT, CODE, RICH_TEXT, RADIO_BUTTON, CHECKBOX)
	private val TEXT_KINDS = Set(SIMPLE, MULTILINE_TEXT, CODE, RICH_TEXT, RADIO_BUTTON, CHECKBOX)
	/**
	 * The kinds that behave as SIMPLE properties: a plain string value that may define preset values (a RADIO_BUTTON
	 * selects one of them, a CHECKBOX one or more, comma-separated, or is a true/false value if it has no preset
	 * values), a default value, a dependency, inclusion in exports, and use in filters, snapshots and test sessions.
	 */
	val SIMPLE_LIKE: Set[String] = Set(SIMPLE, RADIO_BUTTON, CHECKBOX)

	def isValid(kind: String): Boolean = ALL.contains(kind)

	/**
	 * True for SIMPLE and the kinds (RADIO_BUTTON, CHECKBOX) that behave as SIMPLE properties.
	 */
	def isSimpleLike(kind: String): Boolean = SIMPLE_LIKE.contains(kind)

	/**
	 * True for the kind that records one or more selected preset values as a comma-separated list.
	 */
	def isMultiValue(kind: String): Boolean = CHECKBOX.equals(kind)

	/**
	 * True for the kinds that hold a plain string value (as opposed to BINARY, held as a file, and
	 * SECRET, held encrypted).
	 */
	def isText(kind: String): Boolean = TEXT_KINDS.contains(kind)

	/**
	 * True if changing a property's kind from [[oldKind]] to [[newKind]] should keep the values
	 * already recorded for it. This is the case only when both kinds are text kinds - any change
	 * involving BINARY or SECRET still results in previous values being discarded.
	 */
	def valuesPreservedOnChange(oldKind: String, newKind: String): Boolean = {
		isText(oldKind) && isText(newKind)
	}

}


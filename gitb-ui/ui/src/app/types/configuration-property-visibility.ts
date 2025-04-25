export interface ConfigurationPropertyVisibility {

  hasProperties: boolean
  hasVisibleProperties: boolean
  hasMissingProperties: boolean
  hasVisibleMissingRequiredProperties: boolean
  hasVisibleMissingOptionalProperties: boolean
  hasNonVisibleMissingRequiredProperties: boolean
  hasNonVisibleMissingOptionalProperties: boolean
  hasVisibleMissingRequiredEditableProperties: boolean
  hasVisibleMissingRequiredNonEditableProperties: boolean

}

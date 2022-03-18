package models.automation

import com.gitb.core.AnyContent
import models.Enums.InputMappingMatchType.InputMappingMatchType

class InputMappingContent(var input: AnyContent, var matchType: InputMappingMatchType) {}

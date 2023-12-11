package managers.`export`

import com.gitb.xml.`export`.Settings

case class SystemSettingsExportInfo(
                                     latestSequenceId: Int,
                                     exportedSettings: Settings,
) {}
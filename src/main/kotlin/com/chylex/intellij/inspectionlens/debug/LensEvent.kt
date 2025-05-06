package com.chylex.intellij.inspectionlens.debug

import java.time.Instant

data class LensEvent(val time: Instant, val data: LensEventData)

package org.tianlangstudio.data.hamal.common

import org.slf4j.LoggerFactory

/**
  *
  * Created by zhuhq on 17-4-12.
  */
trait Logging {
  val logger = LoggerFactory.getLogger(getClass)
}

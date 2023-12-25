package infrastructure
package entities

import org.slf4j.{ Logger, LoggerFactory }

trait PersonEntityCommon:
    protected val logger: Logger = LoggerFactory.getLogger(getClass)

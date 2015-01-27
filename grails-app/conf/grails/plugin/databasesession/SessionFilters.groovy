package grails.plugin.databasesession

import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.slf4j.LoggerFactory

/**
 * @author Burt Beckwith
 */
class SessionFilters {

	def mongoSessionPersisterService;

	private static logger = LoggerFactory.getLogger(SessionFilters.class);

	def filters = {
		flash(controller:'*', action:'*') {

			afterView = { Exception e ->

				logger.debug("In filter 2")

				// If the flash scope is not set, no sense in storing it.
				// If there is no session, we cannot store any flash info.
				if (request.getAttribute(GrailsApplicationAttributes.FLASH_SCOPE) == null || request.getSession(false) == null) {
					return
				}

				log.debug("Found flash scope:\n ${flash.toString()}")
				logger.debug("Found flash scope 2:\n ${flash.toString()}")

				try {
					// set the value to the key as a flag to retrieve it from the request
					mongoSessionPersisterService.setAttribute(request.session.id, GrailsApplicationAttributes.FLASH_SCOPE, GrailsApplicationAttributes.FLASH_SCOPE)
				}
				catch (InvalidatedSessionException ise) {
					// ignored
				}
			}
		}
	}
}

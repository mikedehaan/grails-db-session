import grails.plugin.databasesession.MongoSessionCleanupService
import grails.plugin.databasesession.SessionProxyFilter
import grails.util.Metadata
import org.springframework.web.filter.DelegatingFilterProxy

class DatabaseSessionGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.4 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Database Session Plugin" // Headline display name of the plugin
    def author = "Mike De Haan"
    def authorEmail = ""
    def description = '''\
This is a fork and update of the database-session plugin by Burt Beckwith to utilize MongoDB without depending on hibernate.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/database-session"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    def getWebXmlFilterOrder() {
        // make sure the filter is first
        def FilterManager = getClass().getClassLoader().loadClass('grails.plugin.webxml.FilterManager')
        [sessionProxyFilter: FilterManager.GRAILS_WEB_REQUEST_POSITION - 10]
    }

    def doWithWebDescriptor = { xml ->
		if (!isEnabled(application.config)) {
            return
        }

        // add the filter after the last context-param
        def contextParam = xml.'context-param'

        contextParam[contextParam.size() - 1] + {
            'filter' {
                'filter-name'('sessionProxyFilter')
                'filter-class'(DelegatingFilterProxy.name)
            }
        }

        def filter = xml.'filter'
        filter[filter.size() - 1] + {
            'filter-mapping' {
                'filter-name'('sessionProxyFilter')
                'url-pattern'('/*')
                'dispatcher'('ERROR')
                'dispatcher'('FORWARD')
                'dispatcher'('REQUEST')
            }
        }
    }

    def doWithSpring = {
        if (!isEnabled(application.config)) {
            return
        }

        // do the check here instead of doWithWebDescriptor to get more obvious error display
        if (Metadata.current.getApplicationName() && !manager.hasGrailsPlugin('webxml')) {
            throw new IllegalStateException(
                    'The database-session plugin requires that the webxml plugin be installed')
        }

        sessionProxyFilter(SessionProxyFilter) {
            persister = ref('mongoSessionPersisterService')
        }
        databaseCleanupService(MongoSessionCleanupService) {
            grailsApplication = ref('grailsApplication')
        }
    }

    private boolean isEnabled(config) {
        def enabled = config.grails.plugin.databasesession.enabled
        if (enabled instanceof Boolean) {
            return enabled
        }
        return true; //!Environment.isDevelopmentMode()
    }
}

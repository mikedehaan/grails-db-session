package grails.plugin.databasesession

/**
 * @author Masatoshi Hayashi
 */
class MongoSessionCleanupService {

    def grailsApplication

    /**
     * Delete PersistentSessions where the last accessed time is older than a cutoff value.
     */
    void cleanup() {
        def config = grailsApplication.config.grails.plugin.databasesession
        float maxAge = (config.cleanup.maxAge ?: 30) as Float

        long age = System.currentTimeMillis() - maxAge * 1000 * 60

        MongoPersistentSession.removeAllByLastAccessedOlderThan(age)
        if (log.isDebugEnabled()) {
            log.debug "using max age $maxAge minute(s)"
        }
    }
}

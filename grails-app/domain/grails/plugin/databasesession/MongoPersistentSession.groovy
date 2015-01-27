package grails.plugin.databasesession

import com.mongodb.BasicDBObject

class MongoPersistentSession {

    static mapWith = "mongo"

    static mapping = {
        id generator: "assigned"
        version false
        lastAccessedTime index: true
        collection "userSession"
    }

    static byte[] getAttribute(String sessionId, String name) {
        if (name == null) return null

        def session = MongoPersistentSession.collection.findAndModify(
                bo([_id: sessionId]), bo([$set: [lastAccessedTime: System.currentTimeMillis()]]))
        checkInvalidated(session)

        name = escapeMongoKey(name)
        def value = session.attributes[name];

        /*
        def sName = name.split(/\./)
        def value = session.attributes[sName[0]]
        (sName.size() - 1).times {
            if (value instanceof Map) {
                value = value."${sName[it + 1]}"
            }
        }
        */
        return value
    }

    static void setAttribute(String sessionId, String name, byte[] value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException('name parameter cannot be null')
        }

        name = escapeMongoKey(name)

        def attributeName = "attributes.$name".toString()
        MongoPersistentSession.collection.update(
                bo([_id: sessionId]), bo([$set: [(attributeName): value]]))
    }

    private static String escapeMongoKey(String key) {
        return key.replaceAll("\\.", "\\\\" + "uff0e");
    }

    private static String unescapeMongoKey(String key) {
        return key.replaceAll("\\\\" + "uff0e", ".");
    }

    static void removeAttribute(String sessionId, String name) throws InvalidatedSessionException {
        if (name == null) return

        name = escapeMongoKey(name)

        def attributeName = "attributes.$name".toString()
        MongoPersistentSession.collection.update(
                bo([_id: sessionId]), bo([$unset: [(attributeName): 1]]))
    }

    static List<String> getAttributeNames(String sessionId) throws InvalidatedSessionException {
        def session = MongoPersistentSession.collection.findOne([_id: sessionId], [attributes: 1])
        def keySet = session.attributes.keySet() as Set<String>
        if (keySet) {

            keySet.collectEntries({ k, v ->
                [k, unescapeMongoKey(v)]
            });

            return new ArrayList<String>(keySet)
        } else {
            return []
        }
    }

    static void invalidate(String sessionId, Boolean delete) {
        if (delete) {
            MongoPersistentSession.collection.remove(bo([_id: sessionId]))
        } else {
            MongoPersistentSession.collection.update(
                    bo([_id: sessionId]), bo([$set: [invalidated: true]]))
        }
    }

    static long getLastAccessedTime(String sessionId) throws InvalidatedSessionException {
        MongoPersistentSession ps = MongoPersistentSession.get(sessionId)
        checkInvalidated ps
        ps.lastAccessedTime
    }

    static void setMaxInactiveInterval(String sessionId, int interval) throws InvalidatedSessionException {
        MongoPersistentSession.collection.update(
                bo([_id: sessionId]), bo([$set: [maxInactiveInterval: interval]]))
    }

    static int getMaxInactiveInterval(String sessionId) throws InvalidatedSessionException {
        MongoPersistentSession ps = MongoPersistentSession.get(sessionId)
        checkInvalidated ps
        ps.maxInactiveInterval
    }

    static void removeAllByLastAccessedOlderThan(long age) {
        MongoPersistentSession.collection.remove(["lastAccessedTime": [$lt: age]])
    }

    private static BasicDBObject bo(Map source) {
        new BasicDBObject(source)
    }

    private static void checkInvalidated(session) {
        if (!session || session.invalidated) {
            throw new InvalidatedSessionException()
        }
    }

    String id

    Long creationTime

    Long lastAccessedTime

    Boolean invalidated = false

    Integer maxInactiveInterval = 30

    Map<String, byte[]> attributes = [:]

    boolean isValid() {
        !invalidated && lastAccessedTime > System.currentTimeMillis() - maxInactiveInterval * 1000 * 60
    }
}

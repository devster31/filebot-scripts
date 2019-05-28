// test with filebot -list --q "Monogatari" --db AniDB --mapper xem.groovy
import groovy.json.JsonSlurper
import net.filebot.Cache
import net.filebot.CacheType

Closure<Object> request = { Map headers = [:], String base = "http://thexem.de", String path, Map params ->
    Cache  cache    = net.filebot.Cache.getCache('xem', CacheType.Daily)
    URL    baseURL  = new URL(base)
    String query    = params.collect { k, v -> "$k=$v" }.join('&')
    Object response = new URL(baseURL, "$path?$query").get(headers)
    response
    // TODO: daily caching
    // def content = cache.text(url, String.&toURL).get()
}

String origin = anime ? "anidb" : "tvdb"
Integer seas = anime ? 1 : episode?.season

Object  hasMap  = request("/map/havemap", ["origin": origin])
Map     jHasMap = new JsonSlurper().parseText(hasMap.text)
Boolean item    = jHasMap.data.any{ it == id.toString() }
if (item) {
    Object names = request("/map/names", [
        "origin": origin,
        "id": id,
        "defaultNames": 1,
    ])
    Map       jName   = new JsonSlurper().parseText(names.text)
    ArrayList reflect = jName.data.collect{
        if (it.value instanceof Map) {
            def name = it.value.entrySet().value
            [(it.key): name.flatten()]
        } else if (it.value instanceof String) {
            [(it.key): it.value]
        }
    }

    // String  newN   = reflect.findAll{ it instanceof String }.first()
    String  newN   = reflect.findAll{ it.all }?.all.first()
    Integer foundS = reflect.findAll{
		it.entrySet().value.any{ v -> v =~ /(?i)$episode.seriesName/ }
    }.first().entrySet().key.first().toInteger()
    // Integer foundS = item.findAll{ it instanceof Map }*.find{ k, v -> k.match(/$n/) }.find{ it != null }?.value
    Integer newS   = (foundS < 0) ? seas : foundS

    Map old = [
        ep: episode.episode ? episode.episode : episode.special,
        se: episode.special ? 0 : newS,
    ]
    // assuming TVDB destination, could be included in the query
    Object mapping = request("/map/single", [
        "origin": origin,
        "id": id,
        "season": old.se,
        "episode": old.ep,
    ])
    Map jMapping = new JsonSlurper().parseText(mapping.text)
    // also assuming TVDB destination
    if (jMapping.data.isEmpty()) {
        return episode
    }
    def result  = jMapping.data.entrySet().findAll{ it.key.matches(/tvdb.*/) }
    if (result.size() < 2) {
        return new net.filebot.web.Episode(newN, newS, result.first().value.episode, episode?.title, result.first().value.absolute, episode?.special, episode?.airdate, episode.id, episode.seriesInfo)
    } else {
        def multi = []
        for ( i in 0..result.size()-1 ) {
            // hopefully all multi-episodes are just multi-part because I couldn't find a way to merge titles
            multi << new net.filebot.web.Episode(newN, newS, result[i].value.episode, episode?.title, result[i].value.absolute, episode?.special, episode?.airdate, episode.id, episode.seriesInfo)
        }
        return new net.filebot.web.MultiEpisode(*multi)
    }
}
// hopefully return the episode untouched if not matched
return episode

/*
{
    seriesName=Better Off Ted,
    airdate=2009-03-18,
    special=null,
    title=Pilot,
    class=class net.filebot.web.Episode,
    absolute=1,
    episode=1,
    id=413862,
    numbers=[1, 1, null, 1],
    season=1,
    seriesInfo=TheTVDB::84021,
    seriesNames=[
        Better Off Ted,
        Better Off Ted (2009),
        Better Off Ted - Scientificamente pazzi,
        Dilinyósok,
        Давай ещё,
        Тэд, אי אפשר בלי טד,
        Ted a spol.
    ]
}
*/
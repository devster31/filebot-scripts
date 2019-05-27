// test with filebot -list --q "Monogatari" --db AniDB --mapper xem.groovy
import groovy.json.JsonSlurper

// Boolean anime = true
String origin = anime ? "anidb" : "tvdb"
Integer seas
if (anime) {
    seas = 1
} else {
    seas = s
}
def spec = call{special}
def ep = call{e}
def baseURL = new URL("http://thexem.de")
def reqHeaders = [:]
// def params = [
//                 "origin": origin,
//                 "seasonNumbers": 1,
//                 "defaultNames": 1
//             ]
// def query = params.collect { k, v -> "$k=$v" }.join('&')
// def getResponse = new URL(baseURL, "/map/allNames?$query").get(reqHeaders)
def params = [
                "origin": origin
            ]
def query = params.collect { k, v -> "$k=$v" }.join('&')
def getResponse = new URL(baseURL, "/map/havemap?$query").get(reqHeaders)
// def getResponse = new URL(baseURL, "/map/allNames?" +
//     URLEncoder.encode(query, "UTF-8")).get(reqHeaders)
String stringID = id.toString()
Map    json     = new JsonSlurper().parseText(getResponse.text)
// def    item     = json.data.find{ it.key == stringID }?.value
def    item     = json.data.any{ it == stringID }
if (item) {
    def paramsName = [
                "origin": origin,
                "id": id,
                "defaultNames": 1,
            ]
    def  queryName = paramsName.collect { k, v -> "$k=$v" }.join('&')
    def getResName = new URL(baseURL, "/map/names?$queryName").get(reqHeaders)
    Map   jsonName = new JsonSlurper().parseText(getResName.text)
    def    mat     = jsonName.data.collect{
        if (it.value instanceof Map) {
            def name = it.value.entrySet().value
            [(it.key): name.flatten()]
        } else if (it.value instanceof String) {
            [(it.key): it.value]
        }
    }

    // String  newN   = mat.findAll{ it instanceof String }.first()
    String  newN   = mat.findAll{ it.all }?.all.first()
    Integer foundS = mat.findAll{
		it.entrySet().value.any{ v -> v =~ /(?i)$n/ }
    }.first().entrySet().key.first().toInteger()
    // Integer foundS = item.findAll{ it instanceof Map }*.find{ k, v -> k.match(/$n/) }.find{ it != null }?.value
    Integer newS   = (foundS < 0) ? seas : foundS

    def paramsMap = [
                "origin": origin,
                "id": id,
                "season": newS,
                "episode": ep,
            ]
    def queryMap = paramsMap.collect { k, v -> "$k=$v" }.join('&')
    // assuming tvdb destination, could be included in the query
    def getResponseMap = new URL(baseURL, "/map/single?$queryMap").get(reqHeaders)
    Map jsonMap = new JsonSlurper().parseText(getResponseMap.text)
    def result  = jsonMap.data.entrySet().findAll{ it.key.matches(/tvdb.*/) }
    if (result.size() < 2) {
        return new net.filebot.web.Episode(newN, newS, result.first().value.episode, t, result.first().value.absolute, spec, d, id, series)
    } else {
        def multi = []
        for ( i in 0..result.size()-1 ) {
            // hopefully all multi-episodes are just multi-part because I couldn't find a way to merge titles
            multi << new net.filebot.web.Episode(newN, newS, result[i].value.episode, t, result[i].value.absolute, spec, d, id, series)
        }
        return new net.filebot.web.MultiEpisode(*multi)
    }
}
// hopefully return the episode untouched if not matched
return new net.filebot.web.Episode(n, seas, ep, t, absolute, spec, d, id, series)
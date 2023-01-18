import groovy.json.JsonSlurper
import groovy.json.JsonOutput

Boolean isLatin = {
    java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
                        .replaceAll(/\p{InCombiningDiacriticalMarks}+/, '') ==~ /^\p{InBasicLatin}+$/
}

def translJap = {
    /* rate limited to 100 per day I believe, please be careful */
    Object url = new URL('https://api.kuroshiro.org/convert')
    Map requestHeaders = [:]
    Map postBody = [:]
    postBody.str = it
    postBody.to = 'romaji'
    postBody.mode = 'spaced'
    postBody.romajiSystem = 'hepburn'
    def postResponse = url.post(JsonOutput.toJson(postBody).getBytes('UTF-8'), 'application/json', requestHeaders)
    Object json = new JsonSlurper().parseText(postResponse.text)
    return json.result
}

def transl = {
    (languages.first().iso_639_2B == 'jpn') ? translJap(it) : it.transliterate('Any-Latin; NFD; NFC; Title')
}

// def isEng = any{ audio.language ==~ /en/ }{ true }
// def isJpn = any{ languages.first().iso_639_2B == "jpn" || net.filebot.Language.findLanguage(audio.language.first()).iso_639_2B == "jpn" }{false}

Boolean isEng = any{ audio.language.any{ it ==~ /en/ } }{ audio.language ==~ /en/ }{true}
Boolean isJpn = any{ languages.first().ISO2 ==~ /ja/ }{ audio.language.first() ==~ /ja/ }{ false }

// WARNING: any db.{AniDB,TheTVDB} binding requires FileBot 4.8.6 or above
String mainTitle = any{ db.TMDb.n }{ db.TheTVDB.n }{ norm(n).colon(" - ").replaceTrailingBrackets() }
String primTitle = norm(primaryTitle).colon(" - ").replaceTrailingBrackets()

String.metaClass.surround { l = "(", r = ")" ->
    l + delegate + r
}

/* alternative to the above, with defaults, usable with any Type
  String surround(s, l = "(", r = ")") {
    l + s + r
  }
*/

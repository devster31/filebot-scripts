import java.util.regex.Pattern

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

class Common {

    Map replaceMap = [
        (~/[`´‘’ʻ""“”]/): "'",
        '|': ' - ',
        ':': '\u2236',
        // ':': "\u2236", // "∶" Ratio symbol
        // ':': "\uFF1A", // "：" Fullwidth Colon
        // ':': "\uFE55", // "﹕" Small Colon
        // '/': "\u002F", // "/" Solidus
        // '/': "\u29F8", // "⧸" Big Solidus
        // '/': "\u2215", // "∕" Division Slash
        // '/': "\u2044", // "⁄" Fraction Slash
        // '/': "\u2571", // "╱" Box Drawings Light Diagonal Upper Right to Lower Left
        // '?': "\uFF1F", // "？" Fullwidth Question Mark
        '?': '\uFE56', // '﹖' Small Question Mark
        '*': '\u204E', // '⁎' low asterisk
        (~/[*\p{Zs}]+/): ' ',
        (~/\b[IiVvXx]+\b/): { String it -> it.upper() },
        (~/\b[0-9](?i:th|nd|rd)\b/): { String it -> it.lower() }
    ]

    Closure<String> clsReplace = { String origin ->
        String tmpStd = origin
        replaceMap.each { k1, v ->
            [k1].flatten().each { k2 ->
                pattern = k2.class == Pattern ? k2 : Pattern.quote(k2)
                if (v.class == Closure) {
                    tmpStd = tmpStd.replaceAll(pattern, (Closure) v)
                } else {
                    tmpStd = tmpStd.replaceAll(pattern, v)
                }
            }
        }
        if (movie) {
            return tmpStd.replaceTrailingBrackets()
        } else if (anime || episode) {
            return tmpStd
        }
        return tmpStd
    }

    Closure<Boolean> isLatin = {
        java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
                        .replaceAll(/\p{InCombiningDiacriticalMarks}+/, '') ==~ /^\p{InBasicLatin}+$/
    }

    Closure<String> translJap = { original ->
        /* rate limited to 100 per day I believe, please be careful */
        Object url = new URL('https://api.kuroshiro.org/convert')
        Map requestHeaders = [:]
        Map postBody = [:]
        postBody.str = original
        postBody.to = 'romaji'
        postBody.mode = 'spaced'
        postBody.romajiSystem = 'hepburn'
        Object postResponse = url.post(
            JsonOutput.toJson(postBody).getBytes('UTF-8'),
            'application/json',
            requestHeaders
        )
        Object json = new JsonSlurper().parseText(postResponse.text)
        return json.result
    }

    Closure<String> transl = {
        if (languages.first().iso_639_2B == 'jpn') {
            translJap(it)
        } else {
            it.transliterate('Any-Latin; NFD; NFC; Title')
        }
    }


    // def isEng = any{ audio.language ==~ /en/ }{ true }
    // def isJpn = any{ languages.first().iso_639_2B == "jpn" || net.filebot.Language.findLanguage(audio.language.first()).iso_639_2B == "jpn" }{false}

    // Boolean isEng = any{ audio.language.any{ it ==~ /en/ } }{ audio.language ==~ /en/ }{ true }
    // Boolean isJpn = any{ languages.first().ISO2 ==~ /ja/ }{ audio.language.first() ==~ /ja/ }{ false }

    // // WARNING: any db.{AniDB,TheTVDB} binding requires FileBot 4.8.6 or above
    // String mainTitle = any{ db.TMDb.n }{ db.TheTVDB.n }{ norm(n).colon(" - ").replaceTrailingBrackets() }
    // String primTitle = norm(primaryTitle).colon(" - ").replaceTrailingBrackets()

}

String.metaClass.stdReplace { Map replacer ->
    String tmpStd = delegate
    replacer.each { k1, v ->
        [k1].flatten().each { k2 ->
            pattern = k2.class == Pattern ? k2 : Pattern.quote(k2)
            if (v.class == Closure) {
                tmpStd = tmpStd.replaceAll(pattern, (Closure) v)
            } else {
                tmpStd = tmpStd.replaceAll(pattern, v)
            }
        }
    }
    if (movie) {
        return tmpStd.replaceTrailingBrackets()
    } else if (anime || episode) {
        return tmpStd
    }
    return tmpStd
}

String.metaClass.surround { l = "(", r = ")" ->
    l + delegate + r
}

/* alternative to the above, with defaults, usable with any Type
  String surround(s, l = "(", r = ")") {
    l + s + r
  }
*/

return new Common()

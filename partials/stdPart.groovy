import java.util.regex.Pattern

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

    Object clsReplace = { String origin ->
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

return new SubCommon()

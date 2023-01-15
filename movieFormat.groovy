{
  import groovy.json.JsonSlurper
  import groovy.json.JsonOutput

  def normMovie = {
    it.replaceTrailingBrackets()
      // .upperInitial().lowerTrail()
      .replaceAll(/[`´‘’ʻ""“”]/, "'")
      .replaceAll(/[:|]/, " - ")
      // .replaceAll(/[:]/, "\u2236") // "∶" Ratio symbol
      // .replaceAll(/[:]/, "\uFF1A") // "：" Fullwidth Colon
      // .replaceAll(/[:]/, "\uFE55") // "﹕" Small Colon
      // .replaceAll("/", "\u29F8") // "⧸" Big Solidus
      // .replaceAll("/", "\u2215") // "∕" Division Slash
      // .replaceAll("/", "\u2044") // "⁄" Fraction Slash
      // .replaceAll(/[?]/, "\uFF1F") // "？" Fullwidth Question Mark
      .replaceAll(/[?]/, "\uFE56") // "﹖" Small Question Mark
      .replaceAll(/[\*]/, "\u204E") // "⁎" low asterisk
      .replaceAll(/[*\p{Zs}]+/, " ")
      .replaceAll(/\b[IiVvXx]+\b/, { it.upper() })
      .replaceAll(/\b[0-9](?i:th|nd|rd)\b/, { it.lower() })
  }

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

  allOf
    {
      if ((media.OverallBitRate.toInteger() / 1000 < 3000 && vf.minus("p").toInteger() >= 720)
        || vf.minus("p").toInteger() < 720) {
        return "LQ_Movies"
      } else {
        return "Movies"
      }
    }
    // Movies directory
    { def film_directors = info.directors.sort().join(", ")
      n.colon("\u2236 ") + " ($y) [$film_directors]" }
    // File name
    { allOf
      { isLatin(primaryTitle) ? primaryTitle.colon("\u2236 ") : transl(primaryTitle).colon("\u2236 ") }
      {" ($y)"}
      // tags + a few more variants
      { def last = n.tokenize(" ").last()
        /* def _tags = (tags != null) ? tags : null */
        def _tags = any{tags}{null}
        if (_tags) {
          _tags.removeIf { it ==~ /(?i:imax)/ }
        }

        specials = allOf
                    { _tags }
                    { fn.after(/(?i:$last)/).findAll(/(?i)(alternate|first)[ ._-]cut|limited|hybrid/)
                      *.upperInitial()*.lowerTrail()*.replaceAll(/[._-]/, " ") }
                    { fn.after(/(?i:$last)/).findAll(/(?i)imax.?(edition|version)?/)
                      *.upperInitial()*.lowerTrail()*.replaceAll(/[._-]/, " ")
                      *.replaceAll(/(?i:imax)/, "IMAX") }
                    { if (!!(fn.after(/(?i:$last)/) =~ /\WDC\W/)) "Directors Cut" }
                    { fn.after(/(?i:$last)/).match(/remaster/).upperInitial().lowerTrail() }
                    .flatten().sort()
        if (specials.size() > 0) {
          specials.removeIf{ a ->
            _tags.any{ b ->
              a != b && (b.startsWith(a) || b.endsWith(a)) } }
          specials.unique().join(", ").replaceAll(/^/, " - ") } }
      {" PT $pi"}
      {" ["}
      { allOf
        { // Video
          // net.filebot.media.VideoFormat.DEFAULT_GROUPS.guessFormat(dim[0], dim[1])
          allOf
            { vf }
            { vc }
            { include 'partials/hdrPart.groovy' }
          .join(" ")
        }
        { include 'partials/audioPart.groovy' }
      /* logo-free release source finder + source */
      { def fileURL = new URL("file:///scripts/websources.txt")
        def file = new File(fileURL.toURI())
        def websources = file.exists() ? lines(file).join("|") : null
        def isWeb = (source ==~ /WEB.*/)
        // def isWeb = source.matches(/WEB.*/) don't know which one is preferrable
        String lfr
        if (isWeb) {
          lfr = any{ fn.match(/($websources)\.(?i)WEB/) }
                   { if (fn.matches(/(?<=\d{3}[p].)WEB|WEB(?=.[hx]\d{3})/)) 'WEB-DL' }
                   { null }
        }
        allOf
          { def yrange = (y-1)..(y+1)
            fn.find(/([0-9]{4}).([A-Z]{3}).1080p/) { match, year, country ->
            	if (match &&
                  yrange.contains(year.toInteger()) &&
                  Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA3).contains(country))
              country
            }
          }
          { fn.match(/(?i)(UHD).$source/).upper() }
          { lfr }
          {
            def replacements = [
              'Blu-Ray': 'BluRay',
              'Blu-ray': 'BluRay',
              'BD': 'BluRay'
            ]
            source.replace(replacements)
          }
        .join(".") }
      .join(" - ") }
    {"]"}
    { def ed = fn.findAll(/(?i)repack|proper|rerip/)*.upper().join(".")
      // def ed = allOf{fn.match(/repack|proper/)}{f.dir.path.match(/repack|proper/)}*.upper().join(".")
      if (ed) { ".$ed" } }
    /* { any{"-$group"}{"-" + fn.match(/(?:(?<=[-])\w+$)|(?:^\w+(?=[-]))/)} } */
    {"-$group"}
    {subt}
    .join("") }
  .join("/")
}

{ import net.filebot.Language
  import groovy.json.JsonSlurper
  import groovy.json.JsonOutput

  def norm = { it.replaceTrailingBrackets()
                 // .upperInitial().lowerTrail()
                 .replaceAll(/[`´‘’ʻ""“”]/, "'")
                 .replaceAll(/[:|]/, " - ")
                 // .replaceAll(/[:]/, "\uFF1A")
                 // .replaceAll(/[:]/, "\u2236") // ratio
                 .replaceAll(/[?]/, "\uFE56")
                 .replaceAll(/[*\s]+/, " ")
                 .replaceAll(/\b[IiVvXx]+\b/, { it.upper() })
                 .replaceAll(/\b[0-9](?i:th|nd|rd)\b/, { it.lower() }) }

  def isLatin = { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
                                      .replaceAll(/\p{InCombiningDiacriticalMarks}+/, "") ==~ /^\p{InBasicLatin}+$/ }

  def translJap = {
    // rate limited to 100 per day I believe, please be careful
    def url = new URL('https://api.kuroshiro.org/convert')
    def requestHeaders = [:]
    def postBody = [:]
      postBody.str = it
      postBody.to = "romaji"
      postBody.mode = "spaced"
      postBody.romajiSystem = "hepburn"
    def postResponse = url.post(JsonOutput.toJson(postBody).getBytes('UTF-8'), 'application/json', requestHeaders)
    def json = new JsonSlurper().parseText(postResponse.text)
    return json.result
  }

  def transl = {
    (languages.first().iso_639_2B == 'jpn') ? translJap(it) : it.transliterate("Any-Latin; NFD; NFC; Title") }

allOf
  // { if (vf.minus("p").toInteger() < 1080 || ((media.OverallBitRate.toInteger() / 1000 < 3000) && vf.minus("p").toInteger() >= 720)) { } }
  { if ((media.OverallBitRate.toInteger() / 1000 < 3000 && vf.minus("p").toInteger() >= 720) || vf.minus("p").toInteger() < 720) {
      return "LQ_Movies"
    } else {
      return "Movies"
    } }
  // Movies directory
  { def film_directors = info.directors.sort().join(", ")
    n.colon(" - ") + " ($y) [$film_directors]" }
  // File name
  { allOf
    { isLatin(primaryTitle) ? primaryTitle.colon(" - ") : transl(primaryTitle).colon(" - ") }
    {" ($y)"}
    // tags + a few more variants
    { def last = n.tokenize(" ").last()
      def t = tags
      t.removeIf { it ==~ /(?i:imax)/ }
      specials = { allOf
                    { t }
                    { fn.after(/(?i:$last)/).findAll(/(?i:alternate[ ._-]cut|limited)/)
                      *.upperInitial()*.lowerTrail()*.replaceAll(/[._-]/, " ") }
                    { fn.after(/(?i:$last)/).findAll(/(?i:imax).(?i:edition|version)?/)
                      *.upperInitial()*.lowerTrail()*.replaceAll(/[._-]/, " ")
                      *.replaceAll(/(?i:imax)/, "IMAX") }
                    .flatten().sort() }
      specials().size() > 0 ? specials().join(", ").replaceAll(/^/, " - ") : "" }
    {" PT $pi"}
    {" ["}
    { allOf
      // Video stream
      { allOf{vf}{vc}.join(" ") }
      { /* def audioClean = { if (it != null) it.replaceAll(/[\p{Pd}\p{Space}]/, ' ').replaceAll(/\p{Space}{2,}/, ' ') }
           def mCFP = [ "AC3" : "AC3",
                      "AC3+" : "E-AC3",
                      "TrueHD" : "TrueHD",
                      "TrueHD TrueHD+Atmos / TrueHD" : "TrueHD ATMOS",
                      "DTS" : "DTS",
                      "DTS HD HRA / Core" : "DTS-HD HRA",
                      "DTS HD MA / Core" : "DTS-HD MA",
                      "DTS HD X / MA / Core" : "DTS-X",
                      "FLAC" : "FLAC",
                      "PCM" : "PCM",
                      "AC3+ E AC 3+Atmos / E AC 3": "E-AC3+Atmos",
                      "AAC LC LC" : "AAC-LC",
                      "AAC LC SBR HE AAC LC": "HE-AAC",
                      "MLP FBA": "TrueHD"] */

        // map Codec + Format Profile
        def mCFP = [
          "FLAC" : "FLAC",
          "PCM" : "PCM",
          "MP3": "MP3",
          "AC-3": "AC-3",
          "E-AC-3 JOC": "E-AC-3",
          "DTS ES XXCH": "DTS-ES Discrete",
          "DTS XLL": "DTS-HD MA",
          "MLP FBA": "TrueHD",
          "MLP FBA 16-ch": "TrueHD"
        ]
        audio.collect { au ->
          def ac1 = any{ au['CodecID/Hint'] }{au['Format/String']}{ au['Format'] } // extends _ac_ which strips spaces > "CodecID/Hint", "Format"
          def ac2 = any{ au['CodecID/String'] }{ au['Codec/String'] }{ au['Codec'] }
          def atmos = (aco =~ /(?i:atmos)/) ? 'Atmos' : null // _aco_ uses "Codec_Profile", "Format_Profile", "Format_Commercial"
          def combined = allOf{ac1}{ac2}.join(' ')
          def fallback = any{ac1}{ac2}{aco}
          def stream = allOf
            /* _channels_ as it uses "ChannelPositions/String2", "Channel(s)_Original", "Channel(s)"
               compared to _af_ which uses "Channel(s)_Original", "Channel(s)" */
            { allOf{channels}{au['NumberOfDynamicObjects'] + "obj"}.join('+') }
            { allOf{ mCFP.get(combined, aco) }{atmos}.join('+') } /* bit risky keeping aco as default */
            { Language.findLanguage(au['Language']).ISO3.upperInitial() }
            /* _cf_ not being used > "Codec/Extensions", "Format" */
          return stream
        }.sort{a, b -> a.first() <=> b.first() }*.join(" ").join(", ") }
      /* source */
      { // logo-free release source finder
        def file = new File('/scripts/websources.txt')
        def websources = file.exists() ? readLines(file).join("|") : null
        def isWeb = (source ==~ /WEB.*/)
        // def isWeb = source.matches(/WEB.*/) don't know which one is preferrable
        def lfr = { if (isWeb) fn.match(/($websources)\.(?i)WEB/) }
        return allOf{fn.match(/(?i)(UHD).$source/).upper()}{lfr}{source}.join(".") }
      .join(" - ") }
    {"]"}
    { def ed = fn.findAll(/(?i:repack|proper)/)*.upper().join()
      if (ed) { return "." + ed } }
    /* { any{"-$group"}{"-" + fn.match(/(?:(?<=[-])\w+$)|(?:^\w+(?=[-]))/)} } */
    {"-$group"}
    {subt}
    .join("") }
  .join("/") }

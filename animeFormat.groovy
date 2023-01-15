{
  def norm = { it.replaceAll(/[`´‘’ʻ""“”]/, "'")
                 .replaceAll(/[|]/, " - ")
                 .replaceAll(/[?]/, "\uFE56")
                 .replaceAll(/[*\p{Zs}]+/, " ")
                 .replaceAll(/\b[IiVvXx]+\b/, { it.upper() })
                 .replaceAll(/\b[0-9](?i:th|nd|rd)\b/, { it.lower() }) }

  def transl = { it.transliterate("Any-Latin; NFD; NFC; Title") }
  def isLatin = { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
                                      .replaceAll(/\p{InCombiningDiacriticalMarks}+/, "") ==~ /^\p{InBasicLatin}+$/ }

  // def isEng = any{ audio.language ==~ /en/ }{ true }
  // def isJpn = any{ languages.first().iso_639_2B == "jpn" || net.filebot.Language.findLanguage(audio.language.first()).iso_639_2B == "jpn" }{false}
  Boolean isEng = any{ audio.language.first() ==~ /en/ }{ true }
  Boolean isJpn = any{ languages.first().ISO2 ==~ /ja/ }{ audio.language.first() ==~ /ja/ }{ false }

  // WARNING: any db.{AniDB,TheTVDB} binding requires FileBot 4.8.6 or above
  String mainTitle = any{ db.TheTVDB.n }{ norm(n).colon(" - ").replaceTrailingBrackets() }
  String primTitle = norm(primaryTitle).colon(" - ").replaceTrailingBrackets()

  String.metaClass.surround { l = "(", r = ")" ->
    l + delegate + r
  }

/* alternative to the above, with defaults, usable with any Type
  String surround(s, l = "(", r = ")") {
    l + s + r
  }
*/

allOf
  { "Anime" }
  { allOf
      { mainTitle }
      { db.TheTVDB.y.toString().surround() }
    .join(" ") }
  {
    // TODO: possibly replace with db.TheTVDB.special
    if (episode.special) { // else { if (sc > 0) "Season $s" }
      "Specials"
    } else {
      allOf
        { ["Season", db.TheTVDB.s].join(" ") }
        { if (mainTitle.getSimilarity(primTitle) < 0.95) primTitle.surround("[", "]") }
        { db.TheTVDB.sy.bounds().join("-").surround() }
      .join(" ")
    }
  }
  { allOf
  	{ allOf
        { def grp = net.filebot.media.MediaDetection.releaseInfo.getReleaseGroup(fn.replaceAll(/\[.*\]$/, ""))
          (grp) ? "[$grp]" : "[$group]" }
        { mainTitle }
      .join(" ") }
    { // EPISODE NUMBERING
      // String _absolute = "EP" + db.TheTVDB.absolute.pad(2)
      if (episode.special) {
        "S$special"
      } else {
        any
          { allOf
              /*
              { if (db.TheTVDB.sc > 1) db.TheTVDB.s00e00 }
              { db.TheTVDB.sc > 1 ? _absolute.surround("(", ")") : _absolute }
              */
              { db.TheTVDB.sxe }
              { db.TheTVDB.absolute.pad(2).surround() }
            .join(" ") }
          { absolute.pad(2) }
      }
    }
    { allOf
      // { isLatin(t) ? t.colon(" - ") : transl(t).colon(" - ") }
      { // EPISODE NAME
        def trLang = any{ if (isJpn) "x-jat" }{ if (isEng) "eng" }{ audio.language.first() }{"eng"}
        def epName = any{ db.TheTVDB.t }{t}
        // ╱ is the replacement for slash
        switch (trLang) {
          case { it == "x-jat" }:
          allOf
            { norm(localize."$trLang".t).colon(", ").slash("\u2571") }
            { "[" + norm(epName).colon(", ").slash("\u2571") + "]" }
          .join(" ")
          break
        case { it == "eng" }:
          norm(epName).colon(", ").slash("\u2571")
          break
        default:
          norm(localize."$trLang".t).colon(", ").slash("\u2571")
        }
      }
      { tags.join(", ").replaceAll(/^/, " - ") }
      { "PT $pi" }
      { allOf
        { allOf
          { // Video
            // net.filebot.media.VideoFormat.DEFAULT_GROUPS.guessFormat(dim[0], dim[1])
            allOf
              { vf }
              { vc }
              {
                def _HDRMap = [
                  "HDR10": "HDR10",
                  "SMPTE ST 2086": "HDR10",
                  "SMPTE ST 2094 App 4": "HDR10+",
                  "Dolby Vision / SMPTE ST 2086": "Dolby Vision",
                  "Dolby Vision / HDR10": "Dolby Vision",
                  "SL-HDR1": "SL-HDR1", // , Version 1.0, Parameter-based, constant
                                        // , Version 1.0, Parameter-based, non-constant
                  "SL-HDR2": "SL-HDR2", // , Version 0.0, Parameter-based
                ]
                def vid = video.first()
                if (bitdepth > 8) {
                  String _HDR
                  switch (vid) {
                    case { it.findAll { p -> p.key =~ /^HDR_/ }.size() > 0 }:
                      _HDR = any
                        { vid["HDR_Format_Commercial"] }
                        { vid["HDR_Format"] }
                        { hdr }
                        { null }
                        // { vid["HDR_Format/String"] }
                        // { vid["HDR_Format_Compatibility"] }
                        // following for both HDR10+ (misses compatibility) and Dolby Vision
                        // { vid["HDR_Format_Version"] }
                        // following only for Dolby Vision
                        // { vid["HDR_Format_Profile"] }
                        // { vid["HDR_Format_Level"] }
                        // { vid["HDR_Format_Settings"] }

                      // _HDRMap.get(_HDR, _HDR)
                      _HDRMap.find { k, v ->
                        k =~ _HDR
                      }?.value
                      break
                    case { it["transfer_characteristics"].findMatch(/HLG/) }:
                      "HLG"
                      break
                    case { it["transfer_characteristics"] == "PQ" && it["colour_primaries"] == "BT.2020" }:
                      "HDR"
                      break
                    default:
                      "$bitdepth-bit"
                    break
                  }
                }
              }
            .join(" ")
          }
          { include 'partials/audioPart.groovy' }
            /* .sort{ a, b -> a.first() <=> b.first() }.reverse() */
            /* logo-free release source finder + source */
            { def fileURL = new URL("file:///scripts/websources.txt")
              def file = new File(fileURL.toURI())
              def websources = file.exists() ? lines(file).join("|") : null
              def isWeb = (source ==~ /WEB.*/)
              // def isWeb = source.matches(/WEB.*/) don't know which one is preferrable
              String lfr
              if (isWeb) {
                lfr = any{fn.match(/($websources)\.(?i)WEB/)}{null}
              }
              return allOf{ lfr }{ source }.join(".") }
          .join(" - ").surround("[", "]") }
        { "[$crc32]" }
        { def ed = fn.findAll(/(?i)repack|proper/)*.upper().join(".")
          // def ed = allOf{fn.match(/repack|proper/)}{f.dir.path.match(/repack|proper/)}*.upper().join(".")
          if (ed) { ".$ed" } }
        /* { def grp = fn.match(/(?<=[-])\w+$/)
          any{"-$group"}{"-$grp"} } */
        {subt}
        .join("") }
      .join(" ") }
    .join(" - ") }
  .join("/") }

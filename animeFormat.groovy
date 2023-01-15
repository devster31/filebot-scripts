{
  def normTV = {
    it.replaceAll(/[`´‘’ʻ""“”]/, "'")
      .replaceAll(/[|]/, " - ")
      .replaceAll(/[?]/, "\uFE56") // "﹖" Small Question Mark
      .replaceAll(/[\*]/, "\u204E") // "⁎" low asterisk
      .replaceAll(/[*\p{Zs}]+/, " ")
      .replaceAll(/\b[IiVvXx]+\b/, { it.upper() })
      .replaceAll(/\b[0-9](?i:th|nd|rd)\b/, { it.lower() })
  }

  Boolean isEng = any{ audio.language.any{ it ==~ /en/ } }{ audio.language ==~ /en/ }{true}
  Boolean isJpn = any{ languages.first().ISO2 ==~ /ja/ }{ audio.language.first() ==~ /ja/ }{ false }

  // WARNING: any db.{AniDB,TheTVDB} binding requires FileBot 4.8.6 or above
  String mainTitle = any{ db.TheTVDB.n }{ norm(n).colon(" - ").replaceTrailingBrackets() }
  String primTitle = norm(primaryTitle).colon(" - ").replaceTrailingBrackets()

  String.metaClass.surround { l = "(", r = ")" ->
    l + delegate + r
  }

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
      { // EPISODE NAME
        def trLang = any{ if (isJpn) "x-jat" }{ if (isEng) "eng" }{ audio.language.first() }{"eng"}
        def epName = any{ db.TheTVDB.t }{t}
        // ╱ is the replacement for slash
        switch (trLang) {
          case { it == "x-jat" }:
          allOf
            { normTV(localize."$trLang".t).colon(", ").slash("\u2571") }
            { "[" + normTV(epName).colon(", ").slash("\u2571") + "]" }
          .join(" ")
          break
        case { it == "eng" }:
          normTV(epName).colon(", ").slash("\u2571")
          break
        default:
          normTV(localize."$trLang".t).colon(", ").slash("\u2571")
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
              { include 'partials/hdrPart.groovy' }
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
  .join("/")
}

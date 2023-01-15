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

  allOf
    {"TV Shows"}
    { allOf
        { (!isEng && (audio.language != null)) ?
          normTV(localize[audio.language[0]].n).colon(" - ").replaceTrailingBrackets() :
          normTV(n).colon(" - ").replaceTrailingBrackets() }
        { def firstYear = episodelist.find{ it.regular }.airdate.year
          "($firstYear)" }
      .join(" ") }
    { episode.special ? "Specials" : allOf{"Season"}{s}.join(" ") }
    /* allOf{"Season"}{s}{sy}.join(" ") --- {sc >= 10 ? s.pad(2) : s} */
    { allOf
      {
        if (!isEng && (audio.language != null)) {
          normTV(localize[audio.language[0]].n).colon("\u2236 ").replaceTrailingBrackets()
        } else {
          normTV(n).colon("\u2236 ").replaceTrailingBrackets()
        }
      { episode.special ? "S00E" + special.pad(2) : s00e00 }
      { allOf
        // { t.replacePart(replacement = ", Part $1") }
        {
          if (!isEng && (audio.language != null)) {
            normTV(localize[audio.language[0]].t).colon("\u2236 ").slash("\u2571")
          } else {
            normTV(t).colon("\u2236 ").slash("\u2571") // ╱ is the replacement for slash
          }
        }
        {"PT $pi"}
        { allOf
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
            { def fileURL = new URL('file:///scripts/websources.txt')
              def file = new File(fileURL.toURI())
              def websources = file.exists() ? lines(file).join("|") : null
              def isWeb = (source ==~ /WEB.*/)
              // def isWeb = source.matches(/WEB.*/) don't know which one is preferrable
              String lfr
              if (isWeb) {
                lfr = any{ fn.match(/($websources)\.(?i)WEB/)}
		         { if (fn.matches(/(?<=\d{3}[p].)WEB|WEB(?=.[hx]\d{3})/)) 'WEB-DL' }
                         { null }
              }
              def replacements = [
                'dvdrip': 'DVDRip',
              ]
              def src = vs =~ /BluRay|HDTV/ ? vs : source.replace(replacements)
              return allOf{lfr}{src}.join(".") }
          .join(" - ") }
        {"]"}
        { def ed = fn.findAll(/(?i)repack|proper/)*.upper().join(".")
          // def ed = allOf{fn.match(/repack|proper/)}{f.dir.path.match(/repack|proper/)}*.upper().join(".")
          if (ed) { ".$ed" } }
        { def grp = net.filebot.media.MediaDetection.releaseInfo.getReleaseGroup(fn.replaceAll(/\[.*\]$/, ""))
          (grp) ? "-$grp" : "-$group" }
        /* { def grp = fn.match(/(?<=[-])\w+$/)
          any{"-$group"}{"-$grp"} } */
        {subt}
        .join("") }
      .join(" ") }
    .join(" - ") }
  .join("/")
}

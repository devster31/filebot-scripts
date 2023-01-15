{
  def norm = { it.replaceAll(/[`´‘’ʻ""“”]/, "'")
                 .replaceAll(/[|]/, " - ")
                 .replaceAll(/[?]/, "\uFE56") // "﹖" Small Question Mark
                 .replaceAll(/[\*]/, "\u204E") // "⁎" low asterisk
                 .replaceAll(/[*\p{Zs}]+/, " ")
                 .replaceAll(/\b[IiVvXx]+\b/, { it.upper() })
                 .replaceAll(/\b[0-9](?i:th|nd|rd)\b/, { it.lower() }) }

def isEng = any{ audio.language.any{ it ==~ /en/ } }{ audio.language ==~ /en/ }{true}

allOf
  {"TV Shows"}
  { allOf
      { (!isEng && (audio.language != null)) ?
        norm(localize[audio.language[0]].n).colon(" - ").replaceTrailingBrackets() :
        norm(n).colon(" - ").replaceTrailingBrackets() }
      { def firstYear = episodelist.find{ it.regular }.airdate.year
        "($firstYear)" }
    .join(" ") }
  { episode.special ? "Specials" : allOf{"Season"}{s}.join(" ") }
  /* allOf{"Season"}{s}{sy}.join(" ") --- {sc >= 10 ? s.pad(2) : s} */
  { allOf
    { (!isEng && (audio.language != null)) ? norm(localize[audio.language[0]].n).colon("\u2236 ").replaceTrailingBrackets() : norm(n).colon("\u2236 ").replaceTrailingBrackets() }
    { episode.special ? "S00E" + special.pad(2) : s00e00 }
    { allOf
      // { t.replacePart(replacement = ", Part $1") }
      { (!isEng && (audio.language != null)) ? norm(localize[audio.language[0]].t).colon("\u2236 ").slash("\u2571") : norm(t).colon("\u2236 ").slash("\u2571") } // ╱ is the replacement for slash
      {"PT $pi"}
      { allOf
        {" ["}
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
  .join("/") }

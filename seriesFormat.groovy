{ import java.math.RoundingMode
  import net.filebot.Language
  def norm = { it.replaceAll(/[`´‘’ʻ""“”]/, "'")
                 .replaceAll(/[|]/, " - ")
                 .replaceAll(/[?]/, "\uFE56")
                 .replaceAll(/[*\p{Zs}]+/, " ")
                 .replaceAll(/\b[IiVvXx]+\b/, { it.upper() })
                 .replaceAll(/\b[0-9](?i:th|nd|rd)\b/, { it.lower() }) }

def isEng = any{audio.language ==~ /en/}{true}

allOf
  {"TV Shows"}
  { allOf
      // { norm(n).colon(" - ").replaceTrailingBrackets() }
      { (!isEng && (audio.language != null)) ? norm(localize[audio.language[0]].n).colon(" - ").replaceTrailingBrackets() : norm(n).colon(" - ").replaceTrailingBrackets() }
      { "($y)" }
    .join(" ") }
  { episode.special ? 'Specials' : allOf{'Season'}{s}.join(' ') } // allOf{'Season'}{s}{sy}.join(' ') --- {sc >= 10 ? s.pad(2) : s}
  { allOf
    { (!isEng && (audio.language != null)) ? norm(localize[audio.language[0]].n).colon(", ").replaceTrailingBrackets() : norm(n).colon(", ").replaceTrailingBrackets() }
    { episode.special ? 'S00E' + special.pad(2) : s00e00 }
    { allOf
      // { t.replacePart(replacement = ", Part $1") }
      { (!isEng && (audio.language != null)) ? norm(localize[audio.language[0]].t).colon(", ").slash("\u2571") : norm(t).colon(", ").slash("\u2571") } // ╱ is the replacement for slash
      {"PT $pi"}
      { allOf
        { allOf
          {"["}
          { allOf
            // Video stream
            { allOf{vf}{vc}.join(" ") }
            { def audioClean = { it.replaceAll(/[\p{Pd}\p{Space}]/, ' ').replaceAll(/\p{Space}{2,}/, ' ') }
              // map Codec + Format Profile
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
                           "AAC LC SBR HE AAC LC": "HE-AAC" ]
              audio.collect { au ->
              def channels = any{ au['ChannelPositions/String2'] }{ au['Channel(s)_Original'] }{ au['Channel(s)/String'] }{ au['Channel(s)'] }
              def ch = { if ( channels =~ /object/ ) {
                 any
                   { au['Channel(s)/String'] }
                   { au['Channel(s)'] }
                 .replaceAll(/object(s)?/, 'obj')
                 .replaceAll(/channel(s)?/, 'ch')
                 .replaceAll(/\//, '+')
                 .replaceAll(/\p{Space}/, '')
              } else {
                channels.replaceAll(/Object\sBased\s\/|0.(?=\d.\d)/, '')
                        .tokenize('\\/').take(3)*.toDouble()
                        .inject(0, { a, b -> a + b }).findAll { it > 0 }
                        .max().toBigDecimal().setScale(1, RoundingMode.HALF_UP).toString()
              } }
              def codec = audioClean(any{ au['CodecID/String'] }{ au['Codec/String'] }{ au['Codec'] })
              def format = any{ au['CodecID/Hint'] }{ au['Format'] }
              def format_profile = { if ( au['Format_Profile'] != null ) audioClean(au['Format_Profile']) else '' }
              def combined = allOf{codec}{format_profile}.join(' ')
              def stream = allOf
                             { ch }
                             { mCFP.get(combined, format) }
                             { Language.findLanguage(au['Language']).ISO3.upperInitial() }
              return stream }*.join(" ").join(", ") }
            // { any{source}{ if (fn.match(/web/)) { return "WEB-DL" }} }
            { // logo-free release source finder
              def file = new File('/scripts/websources.txt')
              def websources = file.exists() ? readLines(file).join("|") : null
              def isWeb = (source ==~ /WEB.*/)
              // def isWeb = source.matches(/WEB.*/) don't know which one is preferrable
              def lfr = { if (isWeb) fn.match(/($websources)\.(?i)WEB/) }
              return allOf{lfr}{source}.join(".") }
            .join(" - ") }
          {"]"}
          .join("") }
        { def ed = fn.match(/repack|proper/).upper()
          // def ed = allOf{fn.match(/repack|proper/)}{f.dir.path.match(/repack|proper/)}*.upper().join('.')
          if (ed) { return ".$ed" } }
        { def grp = net.filebot.media.MediaDetection.releaseInfo.getReleaseGroup(fn.replaceAll(/\[.*\]$/, ''))
          (grp) ? "-$grp" : "-$group" }
        /* { def grp = fn.match(/(?<=[-])\w+$/)
          any{"-$group"}{"-$grp"} } */
        {subt}
        .join("") }
      .join(" ") }
    .join(" - ") }
  .join("/") }

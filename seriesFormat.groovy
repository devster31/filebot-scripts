{ import java.math.RoundingMode
  import net.filebot.Language
  def norm = { it.replaceAll(/[`´‘’ʻ""“”]/, "'")
                 .replaceAll(/[|]/, " - ")
                 .replaceAll(/[?]/, "\uFE56")
                 .replaceAll(/[*\p{Zs}]+/, " ")
                 .replaceAll(/\b[IiVvXx]+\b/, { it.upper() })
                 .replaceAll(/\b[0-9](?i:th|nd|rd)\b/, { it.lower() }) }

allOf
  {"TV Shows"}
  { allOf
      // { (norm(n) == norm(primaryTitle)) ? norm(n) : norm(n) + ' [' + norm(primaryTitle) + ']' }
      { norm(n).colon(" - ").replaceTrailingBrackets() }
      { "($y)" }
    .join(" ") }
  { episode.special ? 'Specials' : 'Season ' + s }
  { allOf
    { norm(n).colon(", ").replaceTrailingBrackets() }
    { episode.special ? 'S00E' + special.pad(2) : s00e00 }
    { allOf
      // { t.replacePart(replacement = ", Part $1") }
      { norm(t).colon(", ") }
      {"PT $pi"}
      { allOf
        { allOf
          {"["}
          { allOf
            // Video stream
            { allOf{vf}{vc}.join(" ") }
            { def audioClean = { it.replaceAll(/[\p{Punct}\p{Space}]/, ' ').replaceAll(/\p{Space}{2,}/, ' ') }
              // map Codec + Format Profile
              def mCFP = [ "AC3" : "AC3",
                           "AC3+" : "E-AC3",
                           "AAC LC LC" : "AAC-LC",
                           "AAC LC SBR HE AAC LC": "HE-AAC" ]
              audio.collect { au ->
              def channels = any{ au['ChannelPositions/String2'] }{ au['Channel(s)_Original'] }{ au['Channel(s)'] } 
              def ch = channels.replaceAll(/Object\sBased\s\/|0.(?=\d.\d)/, '')
                               .tokenize('\\/').take(3)*.toDouble()
                               .inject(0, { a, b -> a + b }).findAll { it > 0 }
                               .max().toBigDecimal().setScale(1, RoundingMode.HALF_UP).toString()
              def codec = audioClean(any{ au['CodecID/String'] }{ au['Codec/String'] }{ au['Codec'] })
              def format = any{ au['CodecID/Hint'] }{ au['Format'] }
              def format_profile = ( au['Format_Profile'] != null) ? audioClean(au['Format_Profile']) : ''
              def combined = allOf{codec}{format_profile}.join(' ')
              def stream = allOf
                             { ch }
                             { mCFP.get(combined, format) }
                             { Language.findLanguage(au['Language']).ISO3.upperInitial() }
              return stream }*.join(" ").join(", ") }
            {source}
            .join(" - ") }
          {"]"}
          .join("") }
        { def ed = fn.findAll(/(?i)repack|proper/)*.upper().join()
          if (ed) { return ".$ed" } }
        { def grp = net.filebot.media.MediaDetection.releaseInfo.getReleaseGroup(fn)
          (grp && grp == group) ? "-$group" : "-$grp" }
        {subt}
        .join("") }
      .join(" ") }
    .join(" - ") }
  .join("/") }

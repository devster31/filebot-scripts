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
  { episode.special ? 'Specials' : 'Season ' + s.pad(2) }
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
            {[vf,vc].join(" ")}
            { audio.collect { au ->
              def channels = any{ au['ChannelPositions/String2'] }{ au['Channel(s)_Original'] }{ au['Channel(s)'] } 
              def ch = channels.replaceAll(/Object\sBased\s\/|0.(?=\d.\d)/, '')
                               .tokenize('\\/')*.toDouble()
                               .inject(0, { a, b -> a + b }).findAll { it > 0 }
                               .max().toBigDecimal().setScale(1, RoundingMode.HALF_UP).toString()
              def codec = any{ au['CodecID/Hint'] }{ au['Format'] }.replaceAll(/['`´‘’ʻ\p{Punct}\p{Space}]/, '')
              return allOf{ch}{codec}{Language.findLanguage(au['Language']).ISO3.upperInitial()}
            }*.join(" ").join(", ") }
            {source}
            .join(" - ") }
          {"]"}
          .join("") }
        { def ed = fn.findAll(/(?i)repack|proper/)*.upper().join()
          if (ed) { return ".$ed" } }
        {"-$group"}
        {subt}
        .join("") }
      .join(" ") }
    .join(" - ") }
  .join("/") }

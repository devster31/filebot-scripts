{ import java.math.RoundingMode
  import net.filebot.Language
  def norm = { it.replaceAll(/[`´‘’ʻ""“”]/, "'")
                 .replaceAll(/[|]/, " - ")
                 .replaceAll(/[?]/, "")
                 .replaceAll(/[*\p{Zs}]+/, " ") }

allOf
  {"Anime"}
  { primaryTitle ? norm(primaryTitle).colon(" - ") : norm(n).colon(" - ") }
  { allOf
    // { primaryTitle ? norm(primaryTitle).colon(" ").replaceTrailingBrackets() : norm(n).colon(" ").replaceTrailingBrackets() }
    { norm(n).colon(", ").replaceTrailingBrackets() }
    { episode.special ? "S$special" : absolute.pad(2) }
    { allOf
      // { isLatin(t) ? t.colon(" - ") : transl(t).colon(" - ") }
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

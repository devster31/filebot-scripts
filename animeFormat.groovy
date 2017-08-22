{ import java.math.RoundingMode
  import net.filebot.Language
  norm = { it.replaceAll(/[`´‘’ʻ""“”]/, "'")
             .replaceAll(/[|]/, " - ")
             .replaceAll(/[*\p{Zs}]+/, " ") }
  def transl = { it.transliterate("Any-Latin; NFD; NFC; Title") }
  def isLatin = { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
                      .replaceAll(/\p{InCombiningDiacriticalMarks}+/, "") ==~ /^\p{InBasicLatin}+$/ }
allOf
  {"Anime"}
  { primaryTitle ? norm(primaryTitle).colon(" - ") : norm(n).colon(" - ") }
  { allOf
    { primaryTitle ? norm(primaryTitle).colon(" ").replaceTrailingBrackets() : norm(n).colon(" ").replaceTrailingBrackets() }
    // { isLatin(n) ? n.colon(" - ") : transl(n).colon(" - ") }
    { episode.special ? "S$special" : absolute.pad(2) }
    { allOf
      // { t.replacePart(replacement = ", Part $1") }
      // { isLatin(t) ? t.colon(" - ") : transl(t).colon(" - ") }
      { norm(t).replaceAll(/[?]/, "").colon(", ") }
      {"PT $pi"}
      { allOf
        { allOf
          {"["}
          { allOf
            {[vf,vc].join(" ")}
            { audio.collect { au ->
              def channels = any{ au['ChannelPositions/String2'] }{ au['Channel(s)_Original'] }{ au['Channel(s)'] } 
              def ch = channels.tokenize('\\/')*.toDouble()
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

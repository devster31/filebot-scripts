{ import java.math.RoundingMode
  import net.filebot.Language
  def norm = { it.replaceTrailingBrackets()
                 .replaceAll(/[`´‘’ʻ""“”]/, "'")
                 .replaceAll(/[:|]/, " - ")
                 .replaceAll(/[?]/, "!")
                 .replaceAll(/[*\s]+/, " ")
                 .replaceAll(/\b[IiVvXx]+\b/, { it.upper() })
                 .replaceAll(/\b[0-9](?i:th|nd|rd)\b/, { it.lower() }) }
  def transl = { it.transliterate("Any-Latin; NFD; NFC; Title") }
  def isLatin = { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
                                  .replaceAll(/\p{InCombiningDiacriticalMarks}+/, "") ==~ /^\p{InBasicLatin}+$/ }
// { def norm = { it.upperInitial().lowerTrail() } }
allOf
  // { if (vf.minus("p").toInteger() < 1080 || ((media.OverallBitRate.toInteger() / 1000 < 3000) && vf.minus("p").toInteger() >= 720)) { } }
  { if ((media.OverallBitRate.toInteger() / 1000 < 3000) && vf.minus("p").toInteger() >= 720) {
      return "LQ_Movies"
    } else {
      return "Movies"
    } }
  // Movies directory
  // {n.colon(" - ") + " ($y, $director)"}
  { def film_directors = info.directors.sort().join(", ")
    n.colon(" - ") + " ($y; $film_directors)" }
  // File name
  { allOf
    { isLatin(primaryTitle) ? primaryTitle.colon(" - ") : transl(primaryTitle).colon(" - ") }
    {" ($y)"}
    // tags + a few more variants
    { specials = { allOf
                     {tags}
                     { def last = n.tokenize(" ").last()
                       fn.after(/(?i:$last)/).findAll(/(?i:alternate[ ._-]cut|limited)/)*.upperInitial()*.lowerTrail()*.replaceAll(/[._-]/, " ") }
                     .flatten().sort() }
      specials().size() > 0 ? specials().join(", ").replaceAll(/^/, " - ") : "" }
    {" PT $pi"}
    {" ["}
    { allOf
      // Video stream
      {[vf,vc].join(" ")}
      { audio.collect { au ->
        def channels = any{ au['ChannelPositions/String2'] }{ au['Channel(s)_Original'] }{ au['Channel(s)'] }
        def ch = channels.tokenize('\\/')*.toDouble()
                         .inject(0, { a, b -> a + b }).findAll { it > 0 }
                         .max().toBigDecimal().setScale(1, RoundingMode.HALF_UP).toString()
        def codec = any{ au['CodecID/Hint'] }{ au['Format'] }.replaceAll(/['`´‘’ʻ\p{Punct}\p{Space}]/, '')
        return allOf{ch}{codec}{Language.findLanguage(au['Language']).ISO3.upperInitial()} }*.join(" ").join(", ") }
      {source}
      .join(" - ") }
    {"]"}
    { def ed = fn.findAll(/(?i:repack|proper)/)*.upper().join()
      if (ed) { return "." + ed } }
    {"-" + group}
    {subt}
    .join("") }
  .join("/") }

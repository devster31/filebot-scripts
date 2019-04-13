{ import java.math.RoundingMode
  import net.filebot.Language
  def norm = { it.replaceTrailingBrackets()
                 // .upperInitial().lowerTrail()
                 .replaceAll(/[`´‘’ʻ""“”]/, "'")
                 .replaceAll(/[:|]/, " - ")
                 // .replaceAll(/[:]/, "\uFF1A")
                 // .replaceAll(/[:]/, "\u2236") // ratio
                 .replaceAll(/[?]/, "\uFE56")
                 .replaceAll(/[*\s]+/, " ")
                 .replaceAll(/\b[IiVvXx]+\b/, { it.upper() })
                 .replaceAll(/\b[0-9](?i:th|nd|rd)\b/, { it.lower() }) }

  def transl = { it.transliterate("Any-Latin; NFD; NFC; Title") }
  def isLatin = { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
                                  .replaceAll(/\p{InCombiningDiacriticalMarks}+/, "") ==~ /^\p{InBasicLatin}+$/ }

allOf
  // { if (vf.minus("p").toInteger() < 1080 || ((media.OverallBitRate.toInteger() / 1000 < 3000) && vf.minus("p").toInteger() >= 720)) { } }
  { if ((media.OverallBitRate.toInteger() / 1000 < 3000) && vf.minus("p").toInteger() >= 720) {
      return "LQ_Movies"
    } else {
      return "Movies"
    } }
  // Movies directory
  { def film_directors = info.directors.sort().join(", ")
    n.colon(" - ") + " ($y) [$film_directors]" }
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
      { allOf{vf}{vc}.join(" ") }
      { audio.collect { au ->
        def channels = any{ au['ChannelPositions/String2'] }{ au['Channel(s)_Original'] }{ au['Channel(s)'] }
        def ch = channels.replaceAll(/Object\sBased\s\/|0.(?=\d.\d)/, '')
                         .tokenize('\\/').take(3)*.toDouble()
                         .inject(0, { a, b -> a + b }).findAll { it > 0 }
                         .max().toBigDecimal().setScale(1, RoundingMode.HALF_UP).toString()
        def codec = any{ au['CodecID/String'] }{ au['Codec/String'] }{ au['Codec'] }.replaceAll(/['`´‘’ʻ]/, '')
        def format = any{ au['CodecID/Hint'] }{ au['Format'] }.replaceAll(/['`´‘’ʻ\p{Punct}\p{Space}]/, '')
        def format_profile = any{au['Format_Profile']}{'a'}.findAll(/ES(?= Matrix| Discrete)|MA|HRA|Atmos/)
        // def profile_m = any{au['Format_Profile']}{''} =~ /(?<fp>ES|Pro|MA Core|LC)/
        // def profile = profile_m ? profile_m.group('fp') : ''
        def stream = allOf
                       {ch}
                       { allOf{codec}{format_profile[0]}.join('+') }
                       { Language.findLanguage(au['Language']).ISO3.upperInitial() }
        return stream
      }.sort{a, b -> a.first() <=> b.first() }.reverse()*.join(" ").join(", ")
       .replaceAll("AC3\\+", "EAC3").replaceAll("DTS\\+ES", "DTS-ES")
       .replaceAll('MPEG-1 Audio layer 3', 'MP3') }
      /* source */
      { // logo-free release source finder
        def websources = readLines("/mnt/antares/scripts/websources.txt").join("|")
        def isWeb = (source ==~ /WEB.*/)
        // def isWeb = source.matches(/WEB.*/) don't know which one is preferrable
        def lfr = { if (isWeb) fn.match(/($websources)\.(?i)WEB/) }
        return allOf{fn.match(/(?i)(UHD).$source/).upper()}{lfr}{source}.join(".") }
      .join(" - ") }
    {"]"}
    { def ed = fn.findAll(/(?i:repack|proper)/)*.upper().join()
      if (ed) { return "." + ed } }
    {"-" + group}
    {subt}
    .join("") }
  .join("/") }

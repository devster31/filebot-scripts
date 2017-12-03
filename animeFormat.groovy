{ import java.math.RoundingMode
  import net.filebot.Language
  def norm = { it.replaceAll(/[`´‘’ʻ""“”]/, "'")
                 .replaceAll(/[|]/, " - ")
                 .replaceAll(/[?]/, "\uFE56")
                 .replaceAll(/[*\p{Zs}]+/, " ") }

  def transl = { it.transliterate("Any-Latin; NFD; NFC; Title") }
  def isLatin = { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
                                  .replaceAll(/\p{InCombiningDiacriticalMarks}+/, "") ==~ /^\p{InBasicLatin}+$/ }

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
        { "_[" + crc32 + "]" }
        {subt}
        .join("") }
      .join(" ") }
    .join(" - ") }
  .join("/") }

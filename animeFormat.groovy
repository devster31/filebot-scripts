{
  def norm = { it.replaceAll(/[`´‘’ʻ""“”]/, "'")
                 .replaceAll(/[|]/, " - ")
                 .replaceAll(/[?]/, "\uFE56")
                 .replaceAll(/[*\p{Zs}]+/, " ")
                 .replaceAll(/\b[IiVvXx]+\b/, { it.upper() })
                 .replaceAll(/\b[0-9](?i:th|nd|rd)\b/, { it.lower() }) }

  def transl = { it.transliterate("Any-Latin; NFD; NFC; Title") }
  def isLatin = { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
                                      .replaceAll(/\p{InCombiningDiacriticalMarks}+/, "") ==~ /^\p{InBasicLatin}+$/ }

  // def isEng = any{ audio.language ==~ /en/ }{ true }
  // def isJpn = any{ languages.first().iso_639_2B == "jpn" || net.filebot.Language.findLanguage(audio.language.first()).iso_639_2B == "jpn" }{false}
  Boolean isEng = any{ audio.language.first() ==~ /en/ }{ true }
  Boolean isJpn = any{ languages.first().ISO2 ==~ /ja/ }{ audio.language.first() ==~ /ja/ }{ false }

  // WARNING: any db.{AniDB,TheTVDB} binding requires FileBot 4.8.6 or above
  String mainTitle = any{ db.TheTVDB.n }{ norm(n).colon(" - ").replaceTrailingBrackets() }
  String primTitle = norm(primaryTitle).colon(" - ").replaceTrailingBrackets()

  String.metaClass.surround { l = "(", r = ")" ->
    l + delegate + r
  }

/* alternative to the above, with defaults, usable with any Type
  String surround(s, l = "(", r = ")") {
    l + s + r
  }
*/

allOf
  { "Anime" }
  { allOf
      { mainTitle }
      { db.TheTVDB.y.toString().surround() }
    .join(" ") }
  {
    // TODO: possibly replace with db.TheTVDB.special
    if (episode.special) { // else { if (sc > 0) "Season $s" }
      "Specials"
    } else {
      allOf
        { ["Season", db.TheTVDB.s].join(" ") }
        { if (mainTitle.getSimilarity(primTitle) < 0.95) primTitle.surround("[", "]") }
        { db.TheTVDB.sy.bounds().join("-").surround() }
      .join(" ")
    }
  }
  { allOf
  	{ allOf
        { def grp = net.filebot.media.MediaDetection.releaseInfo.getReleaseGroup(fn.replaceAll(/\[.*\]$/, ""))
          (grp) ? "[$grp]" : "[$group]" }
        { mainTitle }
      .join(" ") }
    { // EPISODE NUMBERING
      // String _absolute = "EP" + db.TheTVDB.absolute.pad(2)
      if (episode.special) {
        "S$special"
      } else {
        any
          { allOf
              /*
              { if (db.TheTVDB.sc > 1) db.TheTVDB.s00e00 }
              { db.TheTVDB.sc > 1 ? _absolute.surround("(", ")") : _absolute }
              */
              { db.TheTVDB.sxe }
              { db.TheTVDB.absolute.pad(2).surround() }
            .join(" ") }
          { absolute.pad(2) }
      }
    }
    { allOf
      // { isLatin(t) ? t.colon(" - ") : transl(t).colon(" - ") }
      { // EPISODE NAME
        def trLang = any{ if (isJpn) "x-jat" }{ if (isEng) "eng" }{ audio.language.first() }{"eng"}
        def epName = any{ db.TheTVDB.t }{t}
        // ╱ is the replacement for slash
        switch (trLang) {
          case { it == "x-jat" }:
          allOf
            { norm(localize."$trLang".t).colon(", ").slash("\u2571") }
            { "[" + norm(epName).colon(", ").slash("\u2571") + "]" }
          .join(" ")
          break
        case { it == "eng" }:
          norm(epName).colon(", ").slash("\u2571")
          break
        default:
          norm(localize."$trLang".t).colon(", ").slash("\u2571")
        }
      }
      { tags.join(", ").replaceAll(/^/, " - ") }
      { "PT $pi" }
      { allOf
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
          {
            /* def audioClean = { if (it != null) it.replaceAll(/[\p{Pd}\p{Space}]/, " ").replaceAll(/\p{Space}{2,}/, " ") }
            def mCFP = [
              "AC3" : "AC3",
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
              "AAC LC SBR HE AAC LC": "HE-AAC"
            ] */

            // audio map, some of these are probably not needed anymore
            def mCFP = [
              "FLAC": "FLAC",
              "PCM": "PCM",
              "MPEG Audio Layer 3": "MP3",
              "AAC LC": "AAC LC",
              "AAC LC SBR": "HE-AAC", // HE-AACv1
              "AAC LC SBR PS": "HE-AACv2",
              "AC-3 Dep": "E-AC-3+Dep",
              "AC-3 Blu-ray Disc Dep": "E-AC-3+Dep",
              "E-AC-3 Blu-ray Disc Dep": "E-AC-3+Dep",
              "E-AC-3 Dep": "E-AC-3+Dep",
              "E-AC-3 JOC": "E-AC-3 JOC",
              "DTS XBR": "DTS-HD HRA", // needs review
              "DTS ES": "DTS-ES Matrix",
              "DTS ES XBR": "DTS-HD HRA",
              "DTS ES XXCH XBR": "DTS-HD HRA", // needs review
              "DTS ES XXCH": "DTS-ES Discrete",
              "DTS ES XXCH XLL": "DTS-HD MA", // needs review
              "DTS XLL": "DTS-HD MA",
              /* "DTS XLL X": "DTS\u02D0X", // IPA triangular colon */
              "DTS XLL X": "DTS-X",
              "MLP FBA": "TrueHD",
              "MLP FBA 16-ch": "TrueHD",
              "DTS 96/24": "DTS 96-24", // needs review
            ]

            audio.collect { au ->
              /* Format seems to be consistently defined and identical to Format/String
                 Format_Profile and Format_AdditionalFeatures instead
                 seem to be usually mutually exclusive
                 Format_Commercial (and _If_Any variant) seem to be defined
                 mainly for Dolby/DTS formats */
              String _ac = any
                            { allOf
                                { any{ au["Format/String"] }{ au["Format"] } }
                                { au["Format_Profile"] }
                                { au["Format_AdditionalFeatures"] }
                              .collect{ it.tokenize() }.flatten().unique().join(" ") }
                            { au["Format_Commercial"] }
              /* original _aco_ binding uses "Codec_Profile", "Format_Profile", "Format_Commercial" */
              String _aco = any{ au["Codec_Profile"] }{ au["Format_Profile"] }{ au["Format_Commercial"] }
              /* def atmos = (_aco =~ /(?i:atmos)/) ? "Atmos" : null */
              def _fAtmos = any{ audio.FormatCommercial =~ /(?i)atmos/ }{ false }
              def _oAtmos = any{ audio.NumberOfDynamicObjects }{ false }
              String isAtmos = (_fAtmos || _oAtmos) ? "Atmos" : null
              /* _channels_ uses "ChannelPositions/String2", "Channel(s)_Original", "Channel(s)"
                 compared to _af_ which uses "Channel(s)_Original", "Channel(s)"
                 local _channels uses the same variables as {channels} but calculates
                 the result for each audio stream */
              String    _channels = any
                                      { au["ChannelPositions/String2"] }
                                      { au["Channel(s)_Original"] }
                                      { au["Channel(s)"] }
              String    _ch
              /* _channels can contain no numbers */
              Object    splitCh = _channels =~ /^(?i)object.based$/ ? "Object Based" :
                                  _channels.tokenize("\\/\\.")
                                  /* the below may be needed for 3/2/0.2.1/3/2/0.1 files */
                                  // _channels.tokenize("\\/").take(3)*.tokenize("\\.")
                                  //          .flatten()*.toInteger()

              String    chSimple = any{ au["Channel(s)"] }{ au["Channel(s)/String"].replaceAll("channels", "") }

              switch (splitCh) {
                case { it instanceof String }:
                  _ch = allOf{ splitCh }{ chSimple + "ch" }.join(" ")
                  break

                case { it.size > 4 }:
                  def wide = splitCh.takeRight(1)
                  Double main = splitCh.take(4)*.toDouble().inject(0, { a, b -> a + b })
                  Double sub = Double.parseDouble("0." + wide.last())
                  _ch = (main + sub).toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP).toString()
                  break

                case { it.size > 1 }:
                  /* original logic is _mostly_ unchanged if format is like 3/2/0.1 */
                  Double sub = Double.parseDouble(splitCh.takeRight(2).join("."))
                  _ch = splitCh.take(2)*.toDouble().plus(sub).inject(0, { a, b -> a + b })
                              .toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP).toString()
                  break

                default:
                  _ch = splitCh.first().toDouble()
              }

              /* UNUSED - possible fix for mistakes in ChannelPositions/String2 */
              String channelParse
              if ( chSimple.toInteger() != _ch.tokenize(".")*.toInteger().sum() ) {
                List   channelsPos = au["ChannelPositions"].tokenize(",")
                String mainFix = channelsPos.take(3).inject(0) { acc, p ->
                  Integer parsedCh = p.tokenize(":").takeRight(1).first().trim().tokenize(" ").size()
                  acc + parsedCh
                }
                String subFix = channelsPos.takeRight(1).first().trim().tokenize(" ").size()
                channelParse = "${mainFix}.${subFix}"
              }
              /* UNUSED */

              def stream = allOf
                { allOf{ _ch }{ au["NumberOfDynamicObjects"] + "obj" }.join("+") }
                { allOf{ mCFP.get(_ac, _ac) }{isAtmos/* atmos */}.join("+") }
                /* { allOf{ mCFP.get(combined, _aco) }{atmos}.join("+") } /* bit risky keeping _aco as default */
                { def _lang = any{ au["Language"] }{ video.first()["Language"] }
                  net.filebot.Language.findLanguage(_lang).ISO3.upperInitial() }
                /* _cf_ not being used > "Codec/Extensions", "Format" */
              def ret = [:]
              /* this is done to retain stream order */
              ret.id = any{ au["StreamKindId"] }{ au["StreamKindPos"] }{ au["ID"] }
              ret.data = stream
              return ret
            }.toSorted{ it.id }.collect{ it.data }*.join(" ").join(", ") }
            /* .sort{ a, b -> a.first() <=> b.first() }.reverse() */
            /* logo-free release source finder + source */
            { def fileURL = new URL("file:///scripts/websources.txt")
              def file = new File(fileURL.toURI())
              def websources = file.exists() ? lines(file).join("|") : null
              def isWeb = (source ==~ /WEB.*/)
              // def isWeb = source.matches(/WEB.*/) don't know which one is preferrable
              String lfr
              if (isWeb) {
                lfr = any{fn.match(/($websources)\.(?i)WEB/)}{null}
              }
              return allOf{ lfr }{ source }.join(".") }
          .join(" - ").surround("[", "]") }
        { "[$crc32]" }
        { def ed = fn.findAll(/(?i)repack|proper/)*.upper().join(".")
          // def ed = allOf{fn.match(/repack|proper/)}{f.dir.path.match(/repack|proper/)}*.upper().join(".")
          if (ed) { ".$ed" } }
        /* { def grp = fn.match(/(?<=[-])\w+$/)
          any{"-$group"}{"-$grp"} } */
        {subt}
        .join("") }
      .join(" ") }
    .join(" - ") }
  .join("/") }

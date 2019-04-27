{ import net.filebot.Language
  import java.math.RoundingMode

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
  { episode.special ? "Specials" : allOf{"Season"}{s}.join(" ") } // allOf{"Season"}{s}{sy}.join(" ") --- {sc >= 10 ? s.pad(2) : s}
  { allOf
    { (!isEng && (audio.language != null)) ? norm(localize[audio.language[0]].n).colon(", ").replaceTrailingBrackets() : norm(n).colon(", ").replaceTrailingBrackets() }
    { episode.special ? "S00E" + special.pad(2) : s00e00 }
    { allOf
      // { t.replacePart(replacement = ", Part $1") }
      { (!isEng && (audio.language != null)) ? norm(localize[audio.language[0]].t).colon(", ").slash("\u2571") : norm(t).colon(", ").slash("\u2571") } // ╱ is the replacement for slash
      {"PT $pi"}
      { allOf
        {" ["}
        { allOf
          // Video stream
          { allOf{vf}{vc}.join(" ") }
          { /* def audioClean = { if (it != null) it.replaceAll(/[\p{Pd}\p{Space}]/, " ").replaceAll(/\p{Space}{2,}/, " ") }
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
              "FLAC" : "FLAC",
              "PCM" : "PCM",
              "MPEG Audio Layer 3": "MP3",
              "AAC LC": "AAC-LC",
              "AAC LC SBR": "HE-AAC", // HE-AACv1
              "AAC LC SBR PS": "HE-AACv2",
              "E-AC-3 JOC": "E-AC-3",
              "DTS ES": "DTS-ES Matrix",
              "DTS ES XXCH": "DTS-ES Discrete",
              "DTS XLL": "DTS-HD MA",
              /* "DTS XLL X": "DTS\u02D0X", // IPA triangular colon */
              "DTS XLL X": "DTS-X",
              "DTS XBR": "DTS-HR",
              "MLP FBA": "TrueHD",
              "MLP FBA 16-ch": "TrueHD"
            ]
            audio.collect { au ->
              /* Format seems to be consistently defined and identical to Format/String
                Format_Profile and Format_AdditionalFeatures instead
                seem to be usually mutually exclusive
                Format_Commercial (and _If_Any variant) seem to be defined
                mainly for Dolby/DTS formats */
              def _ac = any{allOf{ au["Format"] }{ au["Format_Profile"] }{ au["Format_AdditionalFeatures"] }}{ au["Format_Commercial"] }.join(" ")
              def _aco = any{ au["Codec_Profile"] }{ au["Format_Profile"] }{ au["Format_Commercial"] } // _aco_ uses "Codec_Profile", "Format_Profile", "Format_Commercial"
              /* def atmos = (_aco =~ /(?i:atmos)/) ? "Atmos" : null */
              def isAtmos = {
                def _fAtmos = any{audio.FormatCommercial =~ /(?i)atmos/}{false}
                def _oAtmos = any{audio.NumberOfDynamicObjects}{false}
                if (_fAtmos || _oAtmos) { return "Atmos" }
              }
              /* _channels_ uses "ChannelPositions/String2", "Channel(s)_Original", "Channel(s)"
                  compared to _af_ which uses "Channel(s)_Original", "Channel(s)"
                local _channels uses the same variables as {channels} but calculates
                the result for each audio stream */
              def _channels = any{ au["ChannelPositions/String2"] }{ au["Channel(s)_Original"] }{ au["Channel(s)"] }
              /* _channels can contain no numbers */
              def ch = _channels =~ /^(?i)object.based$/ ? 'Object Based' :
                      _channels.tokenize("\\/").take(3)*.toDouble()
                                .inject(0, { a, b -> a + b }).findAll { it > 0 }.max()
                                .toBigDecimal().setScale(1, RoundingMode.HALF_UP).toString()
              def stream = allOf
                { allOf{ ch }{ au["NumberOfDynamicObjects"] + "obj" }.join("+") }
                { allOf{ mCFP.get(_ac, _ac) }{isAtmos/* atmos */}.join("+") }
                /* { allOf{ mCFP.get(combined, _aco) }{atmos}.join("+") } /* bit risky keeping _aco as default */
                { Language.findLanguage(au["Language"]).ISO3.upperInitial() }
                /* _cf_ not being used > "Codec/Extensions", "Format" */
              def ret = [:]
              /* this is done to retain stream order */
              ret.id = any{ au["StreamKindId"] }{ au["StreamKindPos"] }{ au["ID"] }
              ret.data = stream
              return ret
            }.toSorted{ it.id }.collect{ it.data }*.join(" ").join(", ") }
            /* .sort{ a, b -> a.first() <=> b.first() }.reverse() */
            /* logo-free release source finder + source */
            { def fileURL = new URL('file:///scripts/websources.txt')
              def file = new File(fileURL.toURI())
              def websources = file.exists() ? readLines(file).join("|") : null
              def isWeb = (source ==~ /WEB.*/)
              // def isWeb = source.matches(/WEB.*/) don't know which one is preferrable
              def lfr = { if (isWeb) fn.match(/($websources)\.(?i)WEB/) }
              return allOf{lfr}{source}.join(".") }
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

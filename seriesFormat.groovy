{ import net.filebot.Language
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
        { allOf
          {"["}
          { allOf
            // Video stream
            { allOf{vf}{vc}.join(" ") }
            { /* def audioClean = { if (it != null) it.replaceAll(/[\p{Pd}\p{Space}]/, " ").replaceAll(/\p{Space}{2,}/, " ") }
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
                           "AAC LC SBR HE AAC LC": "HE-AAC" ] */

              // map Codec + Format Profile
              def mCFP = [
                "FLAC" : "FLAC",
                "PCM" : "PCM",
                "MP3": "MP3",
                "AC-3": "AC-3",
                "AAC LC": "AAC LC",
                "E-AC-3 JOC": "E-AC-3",
                "DTS ES XXCH": "DTS-ES Discrete",
                "DTS XLL": "DTS-HD MA",
                "MLP FBA": "TrueHD",
                "MLP FBA 16-ch": "TrueHD"
              ]
              audio.collect { au ->
                def ac1 = any{ au["CodecID/Hint"] }{au["Format/String"]}{ au["Format"] } // extends _ac_ which strips spaces > "CodecID/Hint", "Format"
                def ac2 = any{ au["CodecID/String"] }{ au["Codec/String"] }{ au["Codec"] }
                def acon = any{ au["Codec_Profile"] }{ au["Format_Profile"] }{ au["Format_Commercial"] } // _aco_ uses  "Codec_Profile", "Format_Profile", "Format_Commercial"
                def atmos = (acon =~ /(?i:atmos)/) ? "Atmos" : null // _aco_ uses "Codec_Profile", "Format_Profile", "Format_Commercial"
                def combined = allOf{ac1}{ac2}.join(" ")
                def fallback = any{ac1}{ac2}{acon}
                def stream = allOf
                  /* _channels_ as it uses "ChannelPositions/String2", "Channel(s)_Original", "Channel(s)"
                    compared to _af_ which uses "Channel(s)_Original", "Channel(s)" */
                  { allOf{"${channels}"}{au["NumberOfDynamicObjects"] + "obj"}.join("+") }
                  { allOf{ mCFP.get(combined, acon) }{atmos}.join("+") } /* bit risky keeping aco as default */
                  { Language.findLanguage(au["Language"]).ISO3.upperInitial() }
                  /* _cf_ not being used > "Codec/Extensions", "Format" */
                return stream
              }.sort{a, b -> a.first() <=> b.first() }*.join(" ").join(", ") }
            // { any{source}{ if (fn.match(/web/)) { return "WEB-DL" }} }
            { // logo-free release source finder
              def file = new File("/scripts/websources.txt")
              def websources = file.exists() ? readLines(file).join("|") : null
              def isWeb = (source ==~ /WEB.*/)
              // def isWeb = source.matches(/WEB.*/) don't know which one is preferrable
              def lfr = { if (isWeb) fn.match(/($websources)\.(?i)WEB/) }
              return allOf{lfr}{source}.join(".") }
            .join(" - ") }
          {"]"}
          .join("") }
        { def ed = fn.match(/repack|proper/).upper()
          // def ed = allOf{fn.match(/repack|proper/)}{f.dir.path.match(/repack|proper/)}*.upper().join(".")
          if (ed) { return ".$ed" } }
        { def grp = net.filebot.media.MediaDetection.releaseInfo.getReleaseGroup(fn.replaceAll(/\[.*\]$/, ""))
          (grp) ? "-$grp" : "-$group" }
        /* { def grp = fn.match(/(?<=[-])\w+$/)
          any{"-$group"}{"-$grp"} } */
        {subt}
        .join("") }
      .join(" ") }
    .join(" - ") }
  .join("/") }

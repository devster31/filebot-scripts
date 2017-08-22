{ norm = { it.replaceTrailingBrackets()
             .replaceAll(/[`´‘’ʻ""“”]/, "'")
             .replaceAll(/[|]/, " - ")
             .replaceAll(/[?]/, "")
             .replaceAll(/[*\p{Zs}]+/, " ")
             .replaceAll(/\b[IiVvXx]+\b/, { it.upper() })
             .replaceAll(/\b[0-9](?i:th|nd|rd)\b/, { it.lower() }) }
allOf
  {"TV Shows"}
  { allOf
//      { (norm(n) == norm(primaryTitle)) ? norm(n) : norm(n) + ' [' + norm(primaryTitle) + ']' }
//      { if (primaryTitle) {
//          if (n == primaryTitle) {
//            return n
//          } else {
//            return n + " [$primaryTitle]"
//          }
//        } else {
//          return n
//        } }
      { norm(n).colon(" - ") }
      { "($y)" }
    .join(" ") }
  { episode.special ? 'Specials' : 'Season ' + s.pad(2) }
  { allOf
    { norm(n).colon(", ") }
    { episode.special ? 'S00E' + special.pad(2) : s00e00 }
    { allOf
      // { t.replacePart(replacement = ", Part $1") }
      { t.replaceAll(/[?]/, "").colon(", ") }
      {"PT $pi"}
      { allOf
        { allOf
          {"["}
          { allOf
            {[vf,vc].join(" ")}
            {[channels,ac].join(" ")}
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

{ // Movies directory }
{n.colon(" - ")}{" ($y, $director)"}/

{ // File Name }
{ transl = { it.transliterate("Any-Latin; NFD; NFC; Title") }
  isLatin = { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD)
                                  .replaceAll(/\p{InCombiningDiacriticalMarks}+/, "") ==~ /^\p{InBasicLatin}+$/ }
  isLatin(primaryTitle) ? primaryTitle : transl(primaryTitle)
}{" ($y)"}

{ specials = { allOf 
                 {tags}
                 { fn.findAll(/(?i:alternate[ ._-]cut|limited|proper|repack)/)*.upperInitial()*.lowerTrail()*.replaceAll(/[._-]/, " ") }
                 .flatten().sort() }
    specials().size() > 0 ? specials().join(", ").replaceAll(/^/, " - ") : "" }
{" PT $pi"}

{ allOf {"["}
  { allOf {[vf,vc].join(" ")}
          { allOf {[channels,ac].join(" ")}
                  { def a = audioLanguages
                    a.size() > 1 ? a.ISO3.join(", ").upperInitial() : a.name.first() }
                  .join(" ") }
          {source}.join(" - ") }
  {"]"}
  {"-" + group}
  {subt}
  .join("")
}

{n.colon(' - ')} ({y}, {director})/{primaryTitle.colon(' - ').transliterate('Any-Latn')} ({y}){' - ' + fn.matchAll(/extended|uncensored|uncut|directors[ ._-]cut|remastered|unrated|special[ ._-]edition/)*.upperInitial()*.lowerTrail().sort().join(', ').replaceAll(/[.]/, ' ') + ' '}{' PT'+pi} [{vf} {vc} - {channels} {ac} {languages[0].name} - {source}]-{group}{subt}


{
// allOf
//    {tags.join(",")}
//    {fn.match(/alternate[ ._-]cut/).upperInitial().lowerTrail().replaceAll(/[.]/, " ")}
//    {fn.match(/limited/).upperInitial().lowerTrail()}
//    .flatten().sort().join(", ").replaceAll(/^/, " - ")
// audio.size() > 2 ? 'Multi' : audio.size() > 1 ? 'Dual' : audioLanguages[0]
// audioLanguages.size() > 1 ? audioLanguages*.name.join(", ") : ""
// audioLanguages.size() > 1 ? audioLanguages.join(", ").upperInitial() : ""
//  { any // import net.filebot.Language; Language.findLanguage(audio.language[0]).iso_639_2B
//        {audio.LanguageString[0]}  // audioLanguages[0].name.take(3)
//        {audioLanguages[0].name}   // audioLanguages[0].iso_639_3.upperInitial()
//        {languages[0].name}        // can also use .first() for all of these
//  { // Tags }
//  [{imdbid} {info.RunTime}m]
//  [{vc} {resolution}]
//  { // Subtitle Streams, code style does not match the above, needs rewrite }
//  { " [$text.format[0] " + any {subt}{if (text.language[0]) text.language} \
//                               {'[nil]'} + ']' }
}

/* logo-free release source finder + source */
Object fileURL = new URL('file:///scripts/websources.txt')
Object file = new File(fileURL.toURI())
def websources = file.exists() ? lines(file).join("|") : null
Boolean isWeb = (source ==~ /WEB.*/)
// def isWeb = source.matches(/WEB.*/) don't know which one is preferrable
String lfr
if (isWeb) {
    lfr = any
        { source.match(/($websources)\.(?i)WEB/) }
        { fn.match(/($websources)\.(?i)WEB/) }
        { if (fn.matches(/(?<=\d{3}[p].)WEB|WEB(?=.[hx]\d{3})/)) 'WEB-DL' }
        { null }
}
def replacements = [
    'dvdrip': 'DVDRip',
    'bluray': 'Blu-ray',
    'Blu-Ray': 'Blu-ray',
    'BluRay': 'Blu-ray',
    'BD': 'Blu-ray',
]

allOf
    {
        def yrange = (y-1)..(y+1)
            fn.find(/([0-9]{4}).([A-Z]{3}).1080p/) { match, year, country ->
            	if (match &&
                  yrange.contains(year.toInteger()) &&
                  Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA3).contains(country))
              country
            }
    }
    { fn.match(/(?i)(UHD).$source/).upper() }
    { lfr }
    {
        !(vs ==~ /(?i)BluRay|DVDRip|WEB-DL/) ? vs : source.replace(replacements)
    }
.join(".")

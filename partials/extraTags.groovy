final String space = ' '
final String commaSpace = ', '
final String reIMAX = /(?i:imax)/

String last = n.tokenize(space).last()
List inTags = any { tags } { null }
if (inTags) {
    inTags.removeAll { a ->
        a ==~ reIMAX
    }
}
String pattern = /(?i)(?:Special.?|Extended.?|Ultimate.?)?(?:(?:Director.?s|/ +
                 /Collector.?s|Theatrical|Ultimate|Final|Extended|Rogue|Special|/ +
                 /Diamond|Despecialized|R.?Rated|Super.?Duper|Alternate|First|/ +
                 /IMAX|(?:1st|2nd|3rd|[4-9]th).?Anniversary).(?:Cut|Edition|Version))/ +
                 /|DC|(?:Extended|Theatrical|Remaster(?:ed)?|Recut|Uncut|/ +
                 /Uncensored|Unrated|IMAX|Alternate.?Ending|Limited|Hybrid)/

specials = allOf
        { inTags }
        { fn.after(/(?i:$last)/).findAll(/$pattern/) }
        .flatten()
        .sort()
        *.upperInitial()
        *.lowerTrail()
        *.replaceAll(/[._-]/, space)
        *.replaceAll('Dc', "Director's Cut")
        *.replaceAll(reIMAX, 'IMAX')

if (specials.size() > 0) {
    specials.removeIf { a ->
        inTags.any { b ->
                a != b && (b.startsWith(a) || b.endsWith(a))
        }
    }
    specials.unique()
}

" - ${specials.join(commaSpace)}"

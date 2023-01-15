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
Map codecMap = [
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
                    { any { au["Format/String"] } { au["Format"] } }
                    { au["Format_Profile"] }
                    { au["Format_AdditionalFeatures"] }
                    .collectMany{ it.tokenize() }.unique().join(" ") }
                { au["Format_Commercial"] }
    /* original _aco_ binding uses "Codec_Profile", "Format_Profile", "Format_Commercial" */
    String _aco = any { au["Codec_Profile"] } { au["Format_Profile"] } { au["Format_Commercial"] }
    /* def atmos = (_aco =~ /(?i:atmos)/) ? "Atmos" : null */
    Boolean fAtmos = any { au.FormatCommercial =~ /(?i)atmos/ } { false }
    Boolean oAtmos = any { au.NumberOfDynamicObjects } { false }
    String isAtmos = (fAtmos || oAtmos) ? "Atmos" : null
    /* _channels_ uses "ChannelPositions/String2", "Channel(s)_Original", "Channel(s)"
        compared to _af_ which uses "Channel(s)_Original", "Channel(s)"
        local _channels uses the same variables as {channels} but calculates
        the result for each audio stream */
    String _channels = any
                { au["ChannelPositions/String2"] }
                { au["Channel(s)_Original"] }
                { au["Channel(s)"] }
    String _ch
    /* _channels can contain no numbers */
    Object splitCh = _channels =~ /^(?i)object.based$/ ? "Object Based" :
                    _channels.tokenize("\\/\\.")
                    /* the below may be needed for 3/2/0.2.1/3/2/0.1 files */
                    // _channels.tokenize("\\/").take(3)*.tokenize("\\.")
                    //          .flatten()*.toInteger()


    String chSimple = any { au["Channel(s)"] } { au["Channel(s)/String"].replaceAll("channels", "") }

    switch (splitCh) {
        case { it instanceof String }:
            _ch = allOf { splitCh } { chSimple + "ch" }.join(" ")
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
            _ch = splitCh.take(2)*.toDouble().plus(sub).inject(0) { a, b -> a + b }
                        .toBigDecimal().setScale(1, java.math.RoundingMode.HALF_UP).toString()
            break

        default:
            _ch = splitCh.first().toDouble()
    }

    String _channelPositions = any{au["ChannelPositions"]}{null}
    String channelParse
    if ( _channelPositions != null && chSimple.toInteger() != _ch.tokenize(".")*.toInteger().sum() ) {
        List   channelsPos = _channelPositions.tokenize(",")
        String mainFix = channelsPos.take(3).inject(0) { acc, p ->
            Integer parsedCh = p.tokenize(":").takeRight(1).first().trim().tokenize(" ").size()
            acc + parsedCh
        }
        String subFix = channelsPos.takeRight(1).first().trim().tokenize(" ").size()
        channelParse = "${mainFix}.${subFix}"
    }

    String _chFix
    if (channelParse != null && Float.parseFloat(_ch) <=> Float.parseFloat(channelParse))  {
        _chFix = channelParse
    } else {
        _chFix = _ch.replaceAll(/(?i)Object Based/, '')
    }

    String _lang = any { au["Language"] } { video.first()["Language"] }
    List stream = allOf
        { allOf { _chFix } { au["NumberOfDynamicObjects"].concat("obj") }.join("+") }
        { allOf { codecMap.get(_ac, _ac) } {isAtmos/* atmos */}.join("+") }
        /* { allOf{ codecMap.get(combined, _aco) }{atmos}.join("+") } /* bit risky keeping _aco as default */
        { net.filebot.Language.findLanguage(_lang).ISO3.upperInitial() }
    /* _cf_ not being used > "Codec/Extensions", "Format" */
    Map ret = [:]
    /* this is done to retain stream order */
    ret.id = any{ au["StreamKindId"] }{ au["StreamKindPos"] }{ au["ID"] }
    ret.data = stream
    return ret
}.toSorted{ it.id }.collect{ it.data }*.join(" ").join(", ")
/* .sort{ a, b -> a.first() <=> b.first() }.reverse() */

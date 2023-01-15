Map HDRMap = [
    'HDR10': 'HDR10',
    'SMPTE ST 2086': 'HDR10',
    'SMPTE ST 2094 App 3': 'Advanced HDR',
    'SMPTE ST 2094 App 4': 'HDR10+',
    'Dolby Vision / SMPTE ST 2086': 'Dolby Vision',
    'Dolby Vision / HDR10': 'Dolby Vision',
    'ETSI TS 103 433': 'SL-HDR1',
    'SL-HDR1': 'SL-HDR1', // , Version 1.0, Parameter-based, constant
                        // , Version 1.0, Parameter-based, non-constant
    'SL-HDR2': 'SL-HDR2', // , Version 0.0, Parameter-based
    'SL-HDR3': 'SL-HDR3',
    'Technicolor Advanced HDR': 'Technicolor Advanced HDR',
]

Map vid = video.first()

if (bitdepth > 8) {
    switch (vid) {
        case { vid =~ /\bHDR_Format_Commercial/ }:
            vid['HDR_Format_Commercial']
            break

        case { vid =~ /\bHDR_/ }:
            String fHDR = any
                { vid['HDR_Format'] }
                { vid['HDR_Format/String'] }
                // { vid['HDR_Format/String'] }
                // { vid['HDR_Format_Compatibility'] }
                // following for both HDR10+ (misses compatibility) and Dolby Vision
                // { vid['HDR_Format_Version'] }
                // following only for Dolby Vision
                // { vid['HDR_Format_Profile'] }
                // { vid['HDR_Format_Level'] }
                // { vid['HDR_Format_Settings'] }

            hdr_out = HDRMap.get(fHDR, fHDR)
            if (hdr_out.findMatch(/vision/)) {
                dv_info = allOf
                    { 'P' }
                    { vid['HDR_Format_Profile'].match(/[dh][ve][hvca][e13v]\.\d(\d)/) }
                    {
                        '.' + vid['HDR_Format_Compatibility'].match(/HDR10|SDR/)
                            .replace('HDR10', '1').replace('SDR', '2')
                    }
                .join()
                hdr_out = "$hdr_out $dv_info"
            }
            hdr_out
            break
        case { it['transfer_characteristics'].findMatch(/HLG/) && it['colour_primaries'] == 'BT.2020' }:
            'HLG10' // HLG
            break
        case { it['transfer_characteristics'] == 'PQ' && it['colour_primaries'] == 'BT.2020' }:
            'HDR10' // PQ10 or HDR
            break
        default:
            "$bitdepth-bit"
            break
    }
}

def grp = net.filebot.media.MediaDetection.releaseInfo.getReleaseGroup(fn.replaceAll(/\[.*\]$/, ""))

String.metaClass.surround { l = "[", r = "]" ->
    l + delegate + r
}

String grpOut = grp ? "$grp" : "$group"

if (anime) {
    "$grpOut".surround()
} else {
    "$grpOut".surround('-', '')
}

/* { any{"-$group"}{"-" + fn.match(/(?:(?<=[-])\w+$)|(?:^\w+(?=[-]))/)} } */
/* { def grp = fn.match(/(?<=[-])\w+$/)
    any{"-$group"}{"-$grp"} } */

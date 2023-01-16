def grp = net.filebot.media.MediaDetection.releaseInfo.getReleaseGroup(fn.replaceAll(/\[.*\]$/, ""))
(grp) ? "-$grp" : "-$group"
/* { any{"-$group"}{"-" + fn.match(/(?:(?<=[-])\w+$)|(?:^\w+(?=[-]))/)} } */
/* { def grp = fn.match(/(?<=[-])\w+$/)
    any{"-$group"}{"-$grp"} } */
def ed = fn.findAll(/(?i)repack|proper/)*.upper().join(".")
// def ed = allOf{fn.match(/repack|proper/)}{f.dir.path.match(/repack|proper/)}*.upper().join(".")
if (ed) { ".$ed" }
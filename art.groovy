#!/usr/bin/env filebot -script

// log input parameters
log.fine("Run script [$_args.script] at [$now]")
_def.each{ n, v -> log.finest('Parameter: ' + [n, v].join(' = ')) }
args.withIndex().each{ f, i -> if (f.exists()) { log.finest "Argument[$i]: $f" } else { log.warning "Argument[$i]: File does not exist: $f" } }

// initialize variables
failOnError = _args.conflict.equalsIgnoreCase('fail')
testRun = _args.action.equalsIgnoreCase('test')

// --output folder must be a valid folder
outputFolder = tryLogCatch{ any{ _args.output }{ '.' }.toFile().getCanonicalFile() }

// extra options, myepisodes updates and email notifications
extractFolder      = tryQuietly{ extractFolder as File }
skipExtract        = tryQuietly{ skipExtract.toBoolean() }
deleteAfterExtract = tryQuietly{ deleteAfterExtract.toBoolean() }
excludeList        = tryQuietly{ def f = excludeList as File; f.isAbsolute() ? f : outputFolder.resolve(f.path) }
gmail              = tryQuietly{ gmail.split(':', 2) as List }
mail               = tryQuietly{ mail.split(':', 5) as List }
pushover           = tryQuietly{ pushover.split(':', 2) as List }
storeReport        = tryQuietly{ storeReport.toBoolean() }
reportError        = tryQuietly{ reportError.toBoolean() }

// user-defined filters
ignore      = any{ ignore }{ null }
minFileSize = any{ minFileSize.toLong() }{ 50 * 1000L * 1000L }
minLengthMS = any{ minLengthMS.toLong() }{ 10 * 60 * 1000L }

artFormat = any{ artFormat }{ 'Unsorted/{file.structurePathTail}' }

// include artwork/nfo, pushover/pushbullet and ant utilities as required
if (pushover || pushbullet ) { include('lib/web') }
if (gmail || mail) { include('lib/ant') }

// error reporting functions
def sendEmailReport(title, message, messagetype) {
	if (gmail) {
		sendGmail(
			subject: title, message: message, messagemimetype: messagetype,
			to: any{ mailto } { gmail[0].contains('@') ? gmail[0] : gmail[0] + '@gmail.com' },		// mail to self by default
			user: gmail[0].contains('@') ? gmail[0] : gmail[0] + '@gmail.com', password: gmail[1]
		)
	}
	if (mail) {
		sendmail(
			subject: title, message: message, messagemimetype: messagetype,
			mailhost: mail[0], mailport: mail[1], from: mail[2], to: mailto,
			user: mail[3], password: mail[4]
		)
	}
}

def fail(message) {
	if (reportError) {
		sendEmailReport('[FileBot] Failure', message as String, 'text/plain')
	}
	die(message)
}

// check input parameters
def ut = _def.findAll{ k, v -> k.startsWith('ut_') }.collectEntries{ k, v ->
	if (v ==~ /[%$]\p{Alnum}|\p{Punct}+/) {
		log.warning "Bad $k value: $v"
		v = null
	}
	return [k.substring(3), v ? v : null]
}

// sanity checks
if (outputFolder == null || !outputFolder.isDirectory()) {
	fail "Illegal usage: output folder must exist and must be a directory: $outputFolder"
}

if (args.size() == 0) {
	fail "Illegal usage: no input"
} else if (args.any{ f -> f in outputFolder.listPath() }) {
	fail "Illegal usage: output folder [$outputFolder] must be separate from input arguments $args"
} else if (args.any{ f -> f in File.listRoots() }) {
	fail "Illegal usage: input $args must not include a filesystem root"
}

// collect input fileset as specified by the given --def parameters
roots = args

// helper function to work with the structure relative path rather than the whole absolute path
def relativeInputPath(f) {
	def r = roots.find{ r -> f.path.startsWith(r.path) && r.isDirectory() && f.isFile() }
	if (r != null) {
		return f.path.substring(r.path.length() + 1)
	}
	return f.name
}

// define and load exclude list (e.g. to make sure files are only processed once)
excludePathSet = new FileSet()

if (excludeList) {
	if (excludeList.exists()) {
		try {
			excludePathSet.load(excludeList)
		} catch(Exception e) {
			fail "Failed to load excludeList: $e"
		}
		log.fine "Use excludes: $excludeList (${excludePathSet.size()})"
	} else {
		log.fine "Use excludes: $excludeList"
		if ((!excludeList.parentFile.isDirectory() && !excludeList.parentFile.mkdirs()) || (!excludeList.isFile() && !excludeList.createNewFile())) {
			fail "Failed to create excludeList: $excludeList"
		}
	}
}

extractedArchives = []
temporaryFiles = []

def extract(f) {
	def folder = new File(extractFolder ?: f.dir, f.nameWithoutExtension)
	def files = extract(file: f, output: folder.resolve(f.dir.name), conflict: 'auto', filter: { it.isArchive() || it.isVideo() || it.isSubtitle() || (music && it.isAudio()) }, forceExtractAll: true) ?: []

	extractedArchives += f
	temporaryFiles += folder
	temporaryFiles += files

	return files
}

def acceptFile(f) {
	if (f.isHidden()) {
		log.finest "Ignore hidden: $f"
		return false
	}

	if (f.isDirectory() && f.name ==~ /[.@].+|bin|initrd|opt|sbin|var|dev|lib|proc|sys|var.defaults|etc|lost.found|root|tmp|etc.defaults|mnt|run|usr|System.Volume.Information/) {
		log.finest "Ignore system path: $f"
		return false
	}

	if (f.name =~ /(?<=\b|_)(?i:Sample|Trailer|Extras|Extra.Episodes|Bonus.Features|Music.Video|Scrapbook|Behind.the.Scenes|Extended.Scenes|Deleted.Scenes|Mini.Series|s\d{2}c\d{2}|S\d+EXTRA|\d+xEXTRA|NCED|NCOP|(OP|ED)\d+|Formula.1.\d{4})(?=\b|_)/) {
		log.finest "Ignore extra: $f"
		return false
	}

	// ignore if the user-defined ignore pattern matches
	if (f.path.findMatch(ignore)) {
		log.finest "Ignore pattern: $f"
		return false
	}

	// ignore archives that are on the exclude path list
	if (excludePathSet.contains(f)) {
		return false
	}

	// accept folders right away and skip file sanity checks
	if (f.isDirectory()) {
		return true
	}

	// accept archives if the extract feature is enabled
	if (f.isArchive() || f.hasExtension('001')) {
		return !skipExtract
	}

	// ignore iso images that do not contain a video disk structure
	if (f.hasExtension('iso') && !f.isDisk()) {
		log.fine "Ignore disk image: $f"
		return false
	}

	// ignore small video files
	if (minFileSize > 0 && f.isVideo() && f.length() < minFileSize) {
		log.fine "Skip small video file: $f"
		return false
	}

	// ignore short videos
	if (minLengthMS > 0 && f.isVideo() && any{ getMediaInfo(f, '{minutes}').toLong() * 60 * 1000L < minLengthMS }{ false /* default if MediaInfo fails */ }) {
		log.fine "Skip short video: $f"
		return false
	}

	// process only media files (accept audio files only if music mode is enabled)
	return f.isVideo()
}

// specify how to resolve input folders, e.g. grab files from all folders except disk folders and already processed folders (i.e. folders with movie/tvshow nfo files)
def resolveInput(f) {
	// resolve folder recursively, except disk folders
	if (f.isDirectory()) {
		if (f.isDisk()) {
			return f
		}
		return f.listFiles{ acceptFile(it) }.collect{ resolveInput(it) }
	}

	if (f.isArchive() || f.hasExtension('001')) {
		return extract(f).findAll{ acceptFile(it) }.collect{ resolveInput(it) }
	}

	return f
}

// flatten nested file structure
def input = roots.findAll{ acceptFile(it) }.flatten{ resolveInput(it) }.toSorted()

// update exclude list with all input that will be processed during this run
if (excludeList && !testRun) {
	excludePathSet.append(excludeList, extractedArchives, input)
}

// print exclude and input sets for logging
input.each{ log.fine "Input: $it" }

// early abort if there is nothing to do
if (input.size() == 0) {
	log.warning "No files selected for processing"
	return
}

// group episodes/movies and rename according to Plex standards
def groups = input.groupBy{ f ->
	// print xattr metadata
	if (f.metadata) {
		log.finest "xattr: [$f.name] => [$f.metadata]"
	}
}

if (destinationFiles.size() == 0) {
	fail "Finished without processing any files"
}

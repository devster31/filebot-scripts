#!/usr/bin/env filebot -script

/*
 * Print media info of all video files to TSV file  
 */
def model = [
	'Name': 'fn',
	'Container': 'cf',
	'Resolution': 'resolution',
	'Video Codec': 'vc',
	'Video Format': 'vf',
	'Audio Codec': 'ac',
	'Audio Channels': 'channels',
	'Audio Languages': 'audioLanguages',
	'Subtitle Languages': 'textLanguages',
	'Duration': 'hours',
	'File Size (GB)': 'gigabytes',
	'Extended Attributes': 'json'
]

def separator = '\t'
def header = model.keySet().join(separator)
def format = model.values().collect{ "{$it}" }.join(separator)

args.getFiles{ it.isVideo() }.each{
	def mi = getMediaInfo(it, format)
	// print to console
	log.info header
	log.info mi
}

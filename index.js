#!/usr/bin/env node
const fs = require('fs').promises
const path = require('path')

const _ = require('lodash')
const mu = require('mustache')
const debug = require('debug')('fb:generate')

const vars = require('./templates/vars.json')
/*
	Consider using https://github.com/koblas/sops-decoder-node
	for now it needs `sops -d templates/secrets.json > templates/clear_secrets.json`
*/
const secrets = require('./templates/clear_secrets.json')

const dataView = _.merge(vars, secrets)
debug('dataView is:\n%O', dataView)

const tplReg = /.*\.mustache/
const tplDir = 'templates'
const parDir = path.join(tplDir, 'partials')
const genDir = 'generated'

/**
 * Creates a directory or no-op
 * @param {String} dir path to create or ensure
 * @param {Object|Number} opts options object or mode number
 * @returns {Promise} no arguments or error
 */
async function ensureDir(dir, opts) {
	if (!opts || typeof opts === 'number') {
		opts = { mode: opts }
	}

	let { mode } = opts

	if (mode === undefined) {
		mode = 0o777 & (~process.umask())
	}

	const p = path.resolve(dir)
	try {
		await fs.mkdir(p, mode)
	} catch (error) {
		if (error.code !== 'EEXIST') {
			throw error
		}
	}
}

/**
 * Reads directory and filters results for specific pattern
 * @param {String} scanDir path to scan
 * @param {RegExp} pattern pattern to filter for
 * @return {Promise} containing a Dirent array
 */
async function getFiles(scanDir, pattern) {
	const regex = new RegExp(pattern)
	const files = await fs.readdir(scanDir, { withFileTypes: true })
	const filteredFiles = files.filter(dirent => dirent.isFile() && regex.test(dirent.name))
	const fileContents = Promise.all(
		filteredFiles.map(async dirent => {
			const fBase = dirent.name
			const fName = path.parse(fBase).name
			const absPath = path.resolve(scanDir, fBase)
			debug('loading %s from %s', path.relative(scanDir, absPath), scanDir)
			return {
				name: fName,
				contents: await fs.readFile(absPath, 'utf8')
			}
		})
	)
	return fileContents
}

/**
 * Renders the template asyncronously
 * @param {String} tpl template string
 * @param {Object} data JSON data for template
 * @param {Object} parts partials
 * @return {Promise} containing rendered string
 */
async function render(tpl, data, parts) {
	return new Promise((resolve, reject) => {
		try {
			const rendered = mu.render(tpl, data, parts)
			resolve(rendered)
		} catch (error) {
			reject(error)
		}
	})
}

/**
 * Main function
 * @return {VoidFunction} no data returned
 */
async function main() {
	try {
		const templates = getFiles(tplDir, tplReg)
		const partials = getFiles(parDir, tplReg)
		const loaded = _.zipObject(['templates', 'partials'], await Promise.all([templates, partials]))
		const partObj = loaded.partials.reduce((acc, cur/* , idx, src */) => ({ ...acc, [cur.name]: cur.contents }), {})
		const rendered = await Promise.all(
			loaded.templates.map(async tpl => {
				return _.assign(tpl, { contents: await render(tpl.contents, dataView, partObj) })
			})
		)
		await ensureDir(genDir, { mode: 0o775 })
		await Promise.all(
			rendered.map(async tpl => {
				const outFile = path.resolve(genDir, tpl.name) + '.groovy'
				await fs.writeFile(outFile, tpl.contents, { mode: 0o664 })
				debug('written out %s', path.relative(process.cwd(), outFile))
			})
		)
	} catch (error) {
		debug(error)
	}
}

main().catch(error => {
	debug(error)
	setTimeout(() => {
		process.exit(-1)
	}, 100)
})

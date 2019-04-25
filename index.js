#!/usr/bin/env node
const fs = require('fs')
const path = require('path')

const mu = require('mustache')
const debug = require('debug')
const error = debug('fb:error')
const log = debug('fb:log')
// set this namespace to log via console.log/info
log.log = console.info.bind(console)

const vars = require('./template/vars.json')
const secrets = require('./template/secrets.json')
const dataView = Object.assign({}, vars, secrets)
log(`dataView is:\n${dataView}`)

let tpls = {}
let partials = {}

function multiLoad(scanDir, pattern, obj) {
    var re = new RegExp(pattern)
    fs.readdir(scanDir, { withFileTypes: true }, (err, dirents) => {
        if (err) {
            error(err)
        }

        dirents
            .filter(dirent => dirent.isFile())
            .filter(dirent => re.test(dirent.name) )
            .forEach(file => {
                let absPath = path.resolve(scanDir, file.name)
                log(`loading ${file.name} from ${scanDir}`)
                read(absPath, obj)
            })
    })
}

function read(f, o) {
    let parsed = path.parse(f)
    fs.readFile(f, 'utf8', (err, content) => {
        if (err) {
            error(err)
        }
        let out = {}
        out[parsed.name] = content
        Object.assign(o, out)
    })
}

function ensureDir(dir, opts, callback, made) {
    if (typeof opts === 'function') {
        callback = opts
        opts = {}
    } else if (!opts || typeof opts !== 'object') {
        opts = { mode: opts }
    }

    let mode = opts.mode

    if (mode === undefined) {
        mode = 0o777 & (~process.umask())
    }
    if (!made) made = null

    callback = callback || function () {}

    p = path.resolve(dir)
    fs.mkdir(p, mode, err => {
        if (!err) {
            made = made || p
            return callback(null, made)
        }
        switch (err.code) {
            case 'ENOENT':
                if (path.dirname(p) === p) return callback(err)
                ensureDir(path.dirname(p), opts, (err, made) => {
                    if (err) callback(err, made)
                    else ensureDir(p, opts, callback, made)
                })
                break

            // In the case of any other error, just see if there's a dir
            // there already.  If so, then hooray!  If not, then something
            // is borked.
            default:
                fs.stat(p, (er2, stat) => {
                    // if the stat fails, then that's super weird.
                    // let the original error be the failure reason.
                    if (er2 || !stat.isDirectory()) callback(err, made)
                    else callback(null, made)
                })
                break
        }
    })
}

function renderAndWrite(outDir, obj) {
    Object.keys(obj).forEach( key => {
        let outPath = path.resolve(outDir, key + '.groovy')
        let outData = mu.render(obj[key], dataView, partials)
        log(`templating of ${path.parse(outPath).base} successful`)
        fs.writeFile(outPath, outData, { mode: 0o664 }, (err) => {
            if (err) {
                error(err)
            }
            log(`writing of template ${key + '.groovy'} successful`)
        })
    })
}

log('loading templates')
multiLoad('./template', ".*\.mustache", tpls)
log('loading partials')
multiLoad('./template/partials', ".*\.mustache", partials)
log(`ensuring output directory`)
ensureDir('./generated', { mode: 0o775 } )
renderAndWrite('./generated', tpls)

import argparse
import asyncio
import datetime
import logging
import os
import shutil
import subprocess

"""
async def fb(cmd):
    import select
    proc = subprocess.Popen(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE)

    poller = select.poll()
    poller.register(proc.stdout, select.POLLIN)
    poller.register(proc.stderr, select.POLLIN)

    while True:
        for result in poller.poll(1):
            if result[0] == proc.stdout.name:
                print(proc.stdout.readline())

            if result[1] == proc.stderr.name:
                print(proc.stderr.readline())
"""

"""
import asyncio

async def runc(args):
    proc = await asyncio.create_subprocess_exec(*args)

    while proc.returncode is None:
        out = await proc.stdout.readline()
        err = await proc.stderr.readline()
        l1 = out.decode().rstrip()
        l2 = err.decode().rstrip()
        # Handle line (somehow)
        print(out)
        print(err)
        print(l1)
        print(l2)

args = (['ping','-c','10','google.com'])
asyncio.run(runc(args))
"""
# https://stackoverflow.com/questions/51133407/capture-stdout-and-stderr-of-process-that-runs-an-infinite-loop

async def fb(cmd):
    # shell vs exec
    proc = await asyncio.create_subprocess_exec(
        *cmd,
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE)
    # read asyncronously proc.stdout
    async for line in proc.stdout:
        log.info(f'[stdout] {line.decode().rstrip()}')
    # read asyncronously proc.stderr
    async for line in proc.stderr:
        log.info(f'[stderr] {line.decode().rstrip()}')
    # Wait for the subprocess exit.
    return await proc.wait()

def main():
    scripts = os.getenv("SCRIPTS", default="/scripts")
    ppr_log = os.getenv("PPR_LOG", default="/config/postprocess.log")
    out_dir = os.getenv("OUT_DIR", default=os.getcwd())
    media_out = os.path.join(out_dir, "Media")
    fb_exec = shutil.which("filebot") or os.getenv("FILEBOT", default="/usr/bin/filebot")

    log = logging.getLogger('qbittorrent.postprocess')
    log.addHandler(logging.FileHandler("{}.log".format(ppr_log)))
    log.basicConfig(
        format="{asctime:15} [{name}] - {levelname} - {message}",
        datefmt="%Y-%m-%d %H:%M:%S",
        style='{')

    parser = argparse.ArgumentParser()
    parser.add_argument("-G", "--qb-tags", help="torrent tags separated by comma")
    parser.add_argument("-Z", "--qb-size", type=int, help="torrent size in bytes")
    parser.add_argument("-T", "--qb-tracker", help="current tracker")
    req = parser.add_argument_group('required arguments')
    req.add_argument("-N", "--qb-name", help="torrent name", required=True)
    req.add_argument("-L", "--qb-category", help="torrent category", required=True)
    req.add_argument("-F", "--qb-content", help="content path, same as root for multifile torrent", required=True)
    req.add_argument("-R", "--qb-root", help="root path, first torrent subdirectory path", required=True)
    req.add_argument("-D", "--qb-save", help="torrent tags separated by comma", required=True)
    req.add_argument("-C", "--qb-num", type=int, help="number of files", required=True)
    req.add_argument("-I", "--qb-hash", help="torrent info hash", required=True)
    args = parser.parse_args()

    log.info('-' * 50)
    log.info('--- RUN {} ---'.format(datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")))
    log.info('Value of SCRIPTS:\t\t{}'.format(scripts))
    log.info('Value of OUT_DIR:\t\t{}'.format(out_dir))
    log.info('Value of FILEBOT:\t\t{}'.format(fb_exec))
    log.info('Value of --qb-name:\t\t{}'.format(args.qb_name))
    log.info('Value of --qb-category\t\t{}'.format(args.qb_category))
    log.info('Value of --qb-tags\t\t{}'.format(args.qb_tags))
    log.info('Value of --qb-root\t\t{}'.format(args.qb_root))
    log.info('Value of --qb-save\t\t{}'.format(args.qb_save))
    log.info('Value of --qb-num\t\t{}'.format(args.qb_num))
    log.info('Value of --qb-size\t\t{}'.format(args.qb_size))
    log.info('Value of --qb-hash\t\t{}'.format(args.qb_hash))

    cmd = [filebot, '-script', 'fn:amc', '--action', 'keeplink',
            '--output', media_out, '--conflict', 'skip', '-non-strict',
            '--filter' "'!readLines(\"{}\").contains(n)'".format(os.path.join(scripts, 'excludes.txt')),
            '--log-file', 'amc.log', '--def', 'excludelist=".excludes"',
            'ut_dir={}'.format(args.qb_root), 'ut_kind={}'.format(qb_multi),
            'ut_title={}'.format(args.qb_name), 'ut_label={}'.format(qb_category),
            '@{}'.format(os.path.join(scripts, 'notify.txt')),
            '@{}'.format(os.path.join(scripts, 'movieFormat.groovy')),
            '@{}'.format(os.path.join(scripts, 'seriesFormat.groovy')),
            '@{}'.format(os.path.join(scripts, 'animeFormat.groovy'))]

    qb_multi = args.qb_num > 1 : 'multi' ? 'single'
    ret = asyncio.run(fb(cmd))
    log.info(f'{cmd!r} exited with {ret}')

if __name__ == '__main__':
    main()
#!/usr/bin/env python3
import os
from os import path
import logging

"""
.
|-- downloads
|   |-- complete
|   |   |-- movies
|   |   |   |-- Apollo.13.1995.1080p.BluRay.DTS.x264-NTb.mkv -> ../../../cephfs/Media/Movies/Apollo\ 13\ (1995)\ [Ron\ Howard]/Apollo\ 13\ (1995)\ [1080p\ x264\ -\ 5.1\ DTS\ Eng,\ 2.0\ AC3\ Eng,\ 2.0\ AC3\ Eng\ -\ BluRay]-NTb.mkv
|   |   `-- tv_shows
|   |       |-- A.Series.of.Unfortunate.Events.S03.720p.NF.WEB-DL.DDP5.1.x264-MZABI
|   |       |   |-- A.Series.Of.Unfortunate.Events.S03E01.The.Slippery.Slope.Part.One.720p.AMZN.WEB-DL.DDP5.1.H.264-MZABI.mkv -> ../../../../cephfs/Media/TV\ Shows/A\ Series\ of\ Unfortunate\ Events\ (2017)/Season\ 3/A\ Series\ of\ Unfortunate\ Events\ -\ S03E01\ -\ The\ Slippery\ Slope,\ Part\ One\ [720p\ x264\ -\ 5.1\ E-AC3\ Eng\ -\ AMZN.WEB-DL]-MZABI.mkv
"""

host_ln_path = "/srv/pv/torrent/downloads"
cont_ln_path = "/downloads"
link_dest = "/cephfs"

log = logging.getLogger('symlinks')
log.basicConfig(
    format="{asctime:15} [{name}] - {levelname} - {message}",
    datefmt="%Y-%m-%d %H:%M:%S",
    style='{',
    level=logging.INFO)

for entry in scanTree(sys.argv[1] if len(sys.argv) > 1 else '.'):
    print(entry.path)

def scanTree(p):
    for el in os.scandir(p):
        if el.is_dir(follow_symlinks=False):
            yield from scanTree(el.path)
        else:
            yield el

def getSymlinks(p='.'):
    return [e for e in scanTree(p) if e.is_symlink()]

def main():
    symlinks = getSymlinks(host_ln_path)
    for el in getSymlinks(mnt_abs_root):
        p = el.path
        link = os.readlink(p)
        real_path = path.realpath(link)
        s[s.find(link_dest):]
        if path.startswith(link_dest) and path.exists(real_path):
            link_path = os.path.dirname(p)
            """
            os.path.relpath('/cephfs/Media/Movies/The Princess Bride (1987) [Rob Reiner]/The Princess Bride (1987) - Remastered [1080p x264 - 5.1 DTS Eng - BluRay]-AMIABLE.mkv', '/srv/pv/torrent/downloads/complete/movies')

            this src:
            '../../../cephfs/Media/Movies/The Princess Bride (1987) [Rob Reiner]/The Princess Bride (1987) - Remastered [1080p x264 - 5.1 DTS Eng - BluRay]-AMIABLE.mkv'
            needs to become
            '../../../../../../cephfs/Media/Movies/The Princess Bride (1987) [Rob Reiner]/The Princess Bride (1987) - Remastered [1080p x264 - 5.1 DTS Eng - BluRay]-AMIABLE.mkv'
            and point to:
            /srv/pv/torrent/downloads/complete/movies/The.Princess.Bride.1987.REMASTERED.1080p.BluRay.X264-AMIABLE.mkv
            """
            new_link = os.path.relpath(p, start=mnt_abs_root)
            os.symlink(real_path, dst, target_is_directory=False)

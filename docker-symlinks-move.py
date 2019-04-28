#!/usr/bin/env python3
import os
import logging
import argparse
import pathlib as pl

from os import path

log = logging.getLogger("symlinks")
log.basicConfig(
    format="{asctime:15} [{name}] - {levelname} - {message}",
    datefmt="%Y-%m-%d %H:%M:%S",
    style="{",
    level=logging.INFO,
)

# def scanTree(p):
#     """Generator to recursively scan a directory."""
#     for el in os.scandir(p):
#         if el.is_dir(follow_symlinks=False):
#             yield from scanTree(el.path)
#         else:
#             yield el

# def getSymlinks(p='.'):
#     return [e for e in scanTree(p) if e.is_symlink()]


def get_symlinks(p):
    """
    Returns list of paths which are links in specified directory,
    relative to the argument p
    """
    file_list = [
        os.path.join(path, f) for path, dirs, files in os.walk(p) for f in files
    ]
    return list(filter(lambda x: os.path.islink(x), file_list))


def read_links(ls):
    """Returns list of objects with source and target"""
    return list(map(lambda el: {"source": el, "target": os.readlink(el)}, ls))


def main():
    """
    .
    |-- downloads
    |   |-- complete
    |   |   |-- movies
    |   |   |   |-- Apollo.13.1995.1080p.BluRay.DTS.x264-NTb.mkv -> ../../../cephfs/Media/Movies/Apollo 13 (1995) [Ron Howard]/Apollo 13 (1995) [1080p x264 - 5.1 DTS Eng, 2.0 AC3 Eng, 2.0 AC3 Eng - BluRay]-NTb.mkv
    |   |   `-- tv_shows
    |   |       |-- A.Series.of.Unfortunate.Events.S03.720p.NF.WEB-DL.DDP5.1.x264-MZABI
    |   |       |   |-- A.Series.Of.Unfortunate.Events.S03E01.The.Slippery.Slope.Part.One.720p.AMZN.WEB-DL.DDP5.1.H.264-MZABI.mkv -> ../../../../cephfs/Media/TV Shows/A Series of Unfortunate Events (2017)/Season 3/A Series of Unfortunate Events - S03E01 - The Slippery Slope, Part One [720p x264 - 5.1 E-AC3 Eng - AMZN.WEB-DL]-MZABI.mkv

    steps:
    ✓ read files
    ['./complete/tv_shows/My.Show.S01.720p/My.Show.S01E01.Episode.Title.mkv']

    ✓ read links
    [{'source': './complete/tv_shows/My.Show.S01.720p/My.Show.S01E01.Episode.Title.mkv', 'target': '../../../../cephfs/Media/TV Shows/My Show/Season 1/My Show - S01E01 - Episode Title.mkv'}]

    ✓ map mountpoints
    convert to relative
    """
    parser = argparse.ArgumentParser(
        description="Recreates links to a different directory"
    )
    parser.add_argument(
        "--debug",
        help="Print lots of debugging statements",
        action="store_const",
        dest="loglevel",
        const=logging.DEBUG,
        default=logging.WARNING,
    )
    parser.add_argument(
        "-v",
        "--verbose",
        help="Be verbose",
        action="store_const",
        dest="loglevel",
        const=logging.INFO,
    )
    parser.add_argument(
        "-n",
        "--dry-run",
        help="Log what would be performed without performing it",
        action="store_true",
        dest="noop"
    )
    parser.add_argument("-f", "--from", help="The directory to save the files to")
    parser.add_argument("-t", "--to", help="The directory to save the files to")

    mount_map = {
        "from": {"host": "/cephfs", "container": "/cephfs"},
        "to": {"host": "/srv/pv/torrent/downloads", "container": "/downloads"},
    }

    host_link_list = get_symlinks(host_ln_path)
    host_link_dict = read_links(host_link_list)

    for el in get_symlinks(mnt_abs_root):
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


if __name__ == "__main__":
    main()

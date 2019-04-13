#!/bin/bash -xu

# Input Parameters
TR_APP_VERSION="$TR_APP_VERSION"
TR_TIME_LOCALTIME="$TR_TIME_LOCALTIME"
TR_TORRENT_HASH="$TR_TORRENT_HASH"
TR_TORRENT_ID="$TR_TORRENT_ID"

ARG_PATH="$TR_TORRENT_DIR/$TR_TORRENT_NAME"
ARG_NAME="$TR_TORRENT_NAME"
ARG_LABEL="N/A"

# Configuration
MOUNT="/mnt"
CONFIG_OUTPUT="/$MOUNT/Media"
FILEBOT="/usr/local/bin/filebot"

if [[ "$TR_TORRENT_DIR" =~ ^$MOUNT/usbhdd.* ]]
then
    exit 0
fi

if [[ "$TR_TORRENT_DIR" =~ ^$MOUNT/bellatrix/downloads/books.* ]]
then
    exit 0
fi

LINKS=($(find "$ARG_PATH" -type l))
if [[ ${#LINKS[@]} -gt 0 ]]
then
    exit 0
fi

transmission-remote -t ${TR_TORRENT_ID} -S

export JAVA_OPTS="-Xmx256M"
if [[ "$TR_TORRENT_DIR" =~ ^$MOUNT/bellatrix/downloads/(tv_shows|anime).* ]]
then
# exec="chmod 664 {quote file} ; docker exec rpi3_medusa curl -s -w '\n' 'http://localhost:8081/api/v1/***REMOVED***?cmd=show.refresh&tvdbid={info.id}'" \
# tmpfile="$(mktemp -p /dev/shm/)"
# exec="echo {quote file},{quote f.dir.dir},{info.database},{info.id},{quote info.name} > $tmpfile"
    sudo -H -u devster -g devster -- $FILEBOT -script fn:amc --action keeplink --output "$CONFIG_OUTPUT" --conflict skip \
        --filter '!readLines("$MOUNT/antares/scripts/tv_excludes.txt").contains(n)' \
        -non-strict --log-file amc.log --def excludeList=".excludes" \
        --def ut_dir="$ARG_PATH" ut_kind="multi" ut_title="$ARG_NAME" ut_label="$ARG_LABEL" \
        --def @$MOUNT/scripts/notify.txt \
        --def movieFormat=@$MOUNT/scripts/movieFormat.groovy \
        --def seriesFormat=@$MOUNT/scripts/seriesFormat.groovy \
        --def animeFormat=@$MOUNT/scripts/animeFormat.groovy
else
    sudo -H -u devster -g devster -- $FILEBOT -script fn:amc --action keeplink --output "$CONFIG_OUTPUT" --conflict skip \
        --filter '!readLines("$MOUNT/scripts/movie_excludes.txt").contains(n)' \
        --log-file amc.log --def subtitles=en artwork=y excludeList=".excludes" \
        ut_dir="$ARG_PATH" ut_kind="multi" ut_title="$ARG_NAME" ut_label="$ARG_LABEL" \
        exec="chmod 664 {quote file} ; setfacl -m user:transmission:rw {quote file}" \
        --def @$MOUNT/scripts/notify.txt \
        --def movieFormat=@$MOUNT/scripts/movieFormat.groovy \
        --def seriesFormat=@$MOUNT/scripts/seriesFormat.groovy \
        --def animeFormat=@$MOUNT/scripts/animeFormat.groovy
fi


transmission-remote -t ${TR_TORRENT_ID} -s

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
#CONFIG_OUTPUT="/mnt/usbhdd/Movies"
#CONFIG_OUTPUT="/mnt/bellatrix/Media"
CONFIG_OUTPUT="/mnt/antares/Media"

if [[ "$TR_TORRENT_DIR" =~ ^/mnt/usbhdd.* ]]
then
    exit 0
fi

LINKS=($(find $ARG_PATH -type l))
if [[ ${#LINKS[@]} -gt 0 ]]
then
    exit 0
fi

transmission-remote -t ${TR_TORRENT_ID} -S

sudo -H -u devster -g devster -- /usr/bin/filebot -script fn:amc --action keeplink --output "$CONFIG_OUTPUT" --conflict auto \
    --log-file amc.log --def subtitles=en artwork=y excludeList=".excludes" \
    ut_dir="$ARG_PATH" ut_kind="multi" ut_title="$ARG_NAME" ut_label="$ARG_LABEL" \
    exec="chmod 664 {quote file}" \
    --def @/mnt/antares/scripts/pushover.txt \
    --def movieFormat=@/mnt/antares/scripts/movieFormat.groovy \
    --def seriesFormat=@/mnt/antares/scripts/seriesFormat.groovy

transmission-remote -t ${TR_TORRENT_ID} -s

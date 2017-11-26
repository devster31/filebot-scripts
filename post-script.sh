#!/bin/bash -u

MEDUSA_API_KEY=***REMOVED***
file=$1 # {f}
location=$2 # {f.dir.dir}
tvdbid=${3:-x} # {info.id}

chmod 664 "${file}"
echo "${location/mnt/downloads}"

[ ${tvdbid} == "x" ] && exit 0

http --check-status --ignore-stdin --body \
    :8081/api/v1/${MEDUSA_API_KEY}/ \
    cmd=="show.refresh" \
    indexerid=="${tvdbid}"

#check_show="$(http --check-status --ignore-stdin --body :8081/api/v1/"${MEDUSA_API_KEY}" cmd==shows)"
#echo "${check_show}"
#parse_check="$(echo "${check_show}" | jq ".data | map(select(.tvdbid == "${tvdbid}")) | length > 0")"
#echo "${parse_check}"

#if [[ "${parse_check}" = true ]]
#then
#    http --check-status --ignore-stdin --body \
#        :8081/api/v1/${MEDUSA_API_KEY}/ \
#        cmd=="show.refresh" \
#        indexerid=="${tvdbid}"
#else
#    docker exec rpi3_medusa \
#    http --check-status --ignore-stdin --body \
#        :8081/api/v1/${MEDUSA_API_KEY}/ \
#        cmd==show.addexisting \
#        indexerid==${tvdbid} \
#        location=="${location}" \
#        tvdbid==${tvdbid}
#fi

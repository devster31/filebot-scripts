#!/bin/bash -u

file=$1 # {f}
location=$2 # {f.dir.dir}
database=${3} # {info.DataBase}
id=${4:-x} # {info.id}
name="${5}" # {info.Name}

if [[ ! -f "$1" ]]; then
    echo "No ouput file"
    exit 0
fi

chmod 664 "${file}"
export JAVA_OPTS="-Xmx128M"
filebot -script fn:suball --def maxAgeDaysLimit=false maxAgeDays=3000d "${file}"
echo "${location/mnt/downloads}"

[ ${id} == "x" ] && exit 0

if [[ "${database}" = "AniDB" ]]
then
  id=$(http --body :8081/medusa/api/v1/"${MEDUSA_API_KEY}"/ cmd=='shows' sort==name | jq --arg n "${name}" '.data | .[$n].indexerid')
fi

http --check-status --ignore-stdin --body --pretty=format \
    :8081/medusa/api/v1/${MEDUSA_API_KEY}/ \
    cmd=="show.refresh" \
    indexerid=="${id}"

printf "\n"

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

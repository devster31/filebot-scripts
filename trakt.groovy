#!/usr/bin/env filebot -script

/**
* trakt API v2 URL.
*/
def API_HOST = "api.trakt.tv";
def API_URL = "https://" + API_HOST + "/";
def API_VERSION = "2";

def SITE_URL = "https://trakt.tv";
def OAUTH2_AUTHORIZATION_URL = SITE_URL + "/oauth/authorize";
def OAUTH2_TOKEN_URL = SITE_URL + "/oauth/token";

def HEADER_AUTHORIZATION = "Authorization";
def HEADER_CONTENT_TYPE = "Content-Type";
def CONTENT_TYPE_JSON = "application/json";
def HEADER_TRAKT_API_VERSION = "trakt-api-version";
def HEADER_TRAKT_API_KEY = "trakt-api-key";

// series name => series key (e.g. Doctor Who (2005) => doctorwho)
def collationKey = { s -> s == null ? '' : s.removeAll(/^(?i)(The|A)\b/).removeAll(/\(?\d{4}\)?$/).removeAll(/\W/).lower() }

args.getFiles().findAll{ it.isVideo() && parseEpisodeNumber(it) && detectSeriesName(it) }.groupBy{ detectSeriesName(it) }.each{ series, files ->
	def show = myshows.find{ collationKey(it.name) == collationKey(series) }
	if (show == null && mesadd) {
		show = mes.getShows().find{ collationKey(it.name) == collationKey(series) }
		if (show == null) {
			println "[failure] '$series' not found"
			return
		}
		mes.addShow(show.id)
		println "[added] $show.name"
	}

	files.each{
		if (show != null) {
			def sxe = parseEpisodeNumber(it)
			mes.update(show.id, sxe.season, sxe.episode, mesupdate, mesvalue)
			println "[$mesupdate] $show.name $sxe [$it.name]"
		} else {
			println "[failure] '$series' has not been added [$it.name]"
		}
	}
}

/****************************************************************************
 * Trakt
 ****************************************************************************/

class MyEpisodesScraper {
	this.apikey = apikey
	def username
	def password

	def cache = Cache.getCache('myepisodes', CacheType.Weekly)
	def session = [:]

	def login = {
		def response = Jsoup.connect('http://www.myepisodes.com/login.php').data('username', username, 'password', password, 'action', 'Login', 'u', '').method(Method.POST).execute()
		session << response.cookies()
		return response.parse()
	}

	def get = { url ->
		if (session.isEmpty()) {
			login()
		}

		def response = Jsoup.connect(url).cookies(session).method(Method.GET).execute()
		session << response.cookies()
		def html = response.parse()

		if (html.select('#frmLogin')) {
			session.clear()
			throw new Exception('Login failed')
		}

		return html
	}

	def getShows = {
		def shows = cache.get('MyEpisodes.Shows')
		if (shows == null) {
			shows = ['other', 'A'..'Z'].flatten().findResults{ section ->
				get("http://myepisodes.com/shows.php?list=${section}").select('a').findResults{ a ->
					try {
						return [id:a.absUrl('href').match(/showid=(\d+)/).toInteger(), name:a.text().trim()]
					} catch(e) {
						return null
					}
				}
			}.flatten().sort{ it.name }
			cache.put('MyEpisodes.Shows', shows)
		}
		return shows
	}

	def getShowList = {
		get("http://www.myepisodes.com/shows.php?type=manage").select('option').findResults{ option ->
			try {
				return [id:option.attr('value').toInteger(), name:option.text().trim()]
			} catch(e) {
				return null
			}
		}
	}

	def addShow = { showid ->
		get("http://www.myepisodes.com/views.php?type=manageshow&mode=add&showid=${showid}")
	}

	def update = { showid, season, episode, tick = 'acquired', value = '1' ->
		get("http://www.myepisodes.com/myshows.php?action=Update&showid=${showid}&season=${season}&episode=${episode}&${tick}=${value}")
	}
}

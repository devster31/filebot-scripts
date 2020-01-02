cask 'filebot' do
  version '4.9.0'
  sha256 :no_check
  conflicts_with cask: "filebot"

  url "https://get.filebot.net/filebot/BETA/FileBot_#{version}.app.tar.xz"
  name 'FileBot Beta'
  homepage 'https://www.filebot.net/'

  app 'FileBot.app'
  binary "#{appdir}/FileBot.app/Contents/MacOS/filebot.sh", target: 'filebot'

  zap trash: '~/Library/Preferences/net.filebot.ui.plist'
end

{ def actress = readLines("Movies/names.txt").join("|")
  def network = csv("Movies/networks.csv")
  def site = csv("Movies/sites.csv")
  allOf{
    network.findResult{ k, v -> if (fn =~ /(?i:$k)/) return v }
    site.findResult{ k, v -> if (fn =~ /(?i:$k)/) return v }
    // fn.replaceFirst(/(?i:$siteRegex)/){ site.get( it ) }
    //   .replaceFirst(/(?i:$actress)/){ it.replaceAll(/[_.-]/," ").upperInitial() }
    //   .replaceAll(/(\d{2})\D(\d{2})\D(\d{4})/){ a, d, m, y -> "$y-$m-$d" } }
    ( fn =~ /(?i:$actress)/ ).findAll().join(", ").replaceAll(/[_.-]/, " ").upperInitial()
    // [0].replaceAll(/[_.-]/, " ").upperInitial()
    ( fn =~ /([0-3]?\d)\D([0-1]?\d)\D(\d{4})/ ).findAll()
  }
  .join(" - ")
}

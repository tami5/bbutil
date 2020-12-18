(ns tami.url
  "File system related utils"
  (:require [clojure.string :as str]
            [babashka.process :as p]))

(defn open
  "
  Given a url, it opens it browser
  "
  [url]
  (let [os (-> (p/$ uname -s) :out slurp str/trim)
        cmd (if (= os "Linux") "xdg-open" "open")]
    (p/$ ~cmd ~url)
    true))


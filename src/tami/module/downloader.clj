(ns tami.downloader
  "Downloader module"
  (:require
    [babashka.process :as p]
    [clojure.string :as str]
    [tami.util :as util]
    [tami.fs :as fs]
    [clojure.java.io :as io]))

(defmulti dl
  "pull `url` content to local filesystem based on map `args`. "
  {:arglists '([url args])
   :user/comment "`args` might be subjective to what the url refer to"}
  (fn [url _]
    (cond
      (str/includes? url "git") :git
      (str/includes? url "youtube") :yt)))

(defmethod dl :git [url args]
  (let [[_ _ _ user repo] (str/split url #"/")
        form (str "%s/" (or (:form args) "%s/%s"))
        dpath (str/lower-case (format form (:root args) user repo))]
    (spit (str (System/getProperty "user.home") "/.local/runtime/history/downloads.log")
          (str (util/datetime) " GIT: " user "/" repo))
    (str "git" " " "clone" " " url " " dpath)))

(def testgit ["https://github.com/clojure/tools.cli" {:root "/tmp" :form "%s-%s"}])
; (download (first testgit) (second testgit))

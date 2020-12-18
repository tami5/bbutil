(ns tami.module.git
  (:require [tami.cli :as cli]
            [clojure.string :as str]
            [clojure.java.shell :as shell]))

(defn find-current-user-repo
  "Returns github user/repository of current directory"
  [opts]
  (if (:help opts)
    opts
    (let [{:keys [out exit err]} (shell/sh "git" "config" "remote.origin.url")]
      (if (zero? exit)
        ;; Can handle gh:atom/atom, https://github.com/atom/atom.git or git@github.com:atom/atom.git
        (if-let [user-repo (second (re-find #"(?:gh|github.com)(?::|/)([^/]+/[^/.\s]+)" out))]
          user-repo
          (cli/error "Failed to determine current directory's repository" (pr-str {:out out})))
        (cli/error "Failed to determine current directory's repository" (pr-str {:error err :out out}))))))

(defn find-current-branch []
  "Find the current branch in current working dir."
  (if-let [branch 
           (->> (cli/sh "git" "branch")
                str/split-lines
                (filter #(str/starts-with? % "*"))
                first
                (re-find #"\S+$"))]
    branch
    (cli/error "Unable to detect current tree")))

(defn open-github-url 
  "Given a repository and commit, tree or file, open git repo in default browser."
  [{:keys [repository commit tree file]}]
  (let [url-base (str "https://github.com/" repository)
        url 
        (cond
          commit (str url-base "/commit/" commit)
          tree   (str url-base "/tree/" (find-current-branch))
          file (str url-base "/blob/" (find-current-branch) "/" file)
          :else url-base)]
    (doto url cli/open-url)))

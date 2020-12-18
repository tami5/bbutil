(ns tami.os
  "Functions and wrappers relating to interacting with
   the user and the operating system."
  (:require [clojure.string :as str]
            [babashka.process :as p]
            [clojure.java.io :as io]))

(defn getprop [prop]
  (System/getProperty prop))

(def uname
  (condp #(str/includes? %2 %1) (getprop "os.name")
    "Linux" :linux
    "MacOS" :mac
    "Bsd" :bsd
    :unknown))

(def distro
  (let [e? #(.exists (io/file %))]
    (cond
      (e? "/etc/lsb-release") :ubuntu
      (e? "/etc/debian_release") :debian
      (e? "/etc/arch-release") :archlinux
      (= "mac" (uname)) :mac
      :else :unknown)))

(defn clip-string
  "Copy `str` to user clipboard, return true when done."
  [str]
  (-> (p/$ echo -n ~str) (p/$ xclip -sel clip) p/check :exit (= 0)))

(defn notify-user
  "Notify user with `heading` and `msg` when task return true."
  [bool heading msg]
  (when bool
    (-> ["notify-send" heading msg "-t" 3000]
        p/process
        :exit)
    true))

(defn prompt-user
  "Prompt the user for selecting an item from a list.
  Return selection striped from space."
  ;; TODO: based on env variable and uname or distro, choose what menu to use.
  [heading items]
  (let [runner ["dmenu" "-l" "3" "-i" "-p" heading]]
    (-> (p/$ echo ~(str/join "\n" items))
        (p/process runner)
        :out
        slurp
        str/trim)))

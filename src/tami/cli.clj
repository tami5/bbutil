(ns tami.cli
  "cli and system related functions and wrappers"
  (:require [clojure.java.shell :as shell]
            [babashka.process :refer [process check $ pb start] :as p]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.tools.cli :as cli]))

(defn print-summary
  "
  Print help summary given args and opts strings
  "
  [args-string options-summary]
  (println (format "Usage: %s [OPTIONS]%s\nOptions:\n%s"
                   (.getName (io/file *file*))
                   args-string
                   options-summary)))

(defn error
  "
  Print error message(s) and exit
  "
  [& msgs]
  (apply println "Error:" msgs)
  (System/exit 1))

(defn run-command
  "
  Processes a command's functionality given a cli options definition, arguments
  and primary command fn. This handles option parsing, handles any errors with
  parsing and then passes parsed input to command fn
  "
  [command-fn args cli-opts & parse-opts-options]
  (let [{:keys [errors] :as parsed-input}
        (apply cli/parse-opts args cli-opts parse-opts-options)]
    (if (seq errors)
      (error (str/join "\n" (into ["Options failed to parse:"] errors)))
      (command-fn parsed-input))))

(defn sh
  "
  Wrapper around a shell command which fails fast like bash's -e flag.
  Takes following options: * :dry-run: Prints command instead of executing it
  "
  [& args*]
  (let [default-opts {:is-error-fn #(-> % :exit (not= 0))}
        [options args] (if (map? (last args*))
                         [(merge default-opts (last args*))
                          (drop-last args*)]
                         [default-opts args*])]
    (if (:dry-run options)
      (apply println "Command: " args)
      (let [{:keys [out err] :as res}
            (apply shell/sh
              (concat args [:dir (:dir options)]))]
        (if ((:is-error-fn options) res)
          (error (format "Command '%s' failed with:\n%s"
                         (str/join " " args)
                         (str out "\n" err)))
          out)))))

(defn exec
  "
  Hands over current process to new command. Similar to exec in shell script.
  Thanks to https://github.com/borkdude/babashka/issues/299
  "
  ([cmd-and-args] (exec cmd-and-args nil))
  ([cmd-and-args env-vars]
   (let [pb (doto (ProcessBuilder. cmd-and-args)
                  (.inheritIO))
         _ (when env-vars
             (doto (.environment pb)
                   (.putAll env-vars)))
         proc (.start pb)]
     (-> (Runtime/getRuntime)
         (.addShutdownHook (Thread. #(.destroy proc))))
     (System/exit (.waitFor proc)))))

(defn read-stdin-edn
  "
  Read edn on *in* into a coll. Useful to convert a `bb -I --stream` cmd to
   a script that doesn't require that invocation.
  "
  []
  (take-while #(not (identical? ::EOF %))
              (repeatedly #(edn/read {:eof ::EOF} *in*))))

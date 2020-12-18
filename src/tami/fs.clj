(ns tami.fs
  "File system related utils"
  (:require
    [babashka.process :refer [$ process]]
    [clojure.string :as str]
    [tami.util :as util]
    [clojure.java.io :as io]
    [clojure.zip :as zip]
    [clojure.core.async :as a]))

(defn cwd
  "Returns current working directory."
  []
  (System/getProperty "user.dir"))

(defn expand
  "Given a `path` with tilde `~`, `.` or environment variable.
  Expand it or return self."
  [path]
  (let [tilde? (str/starts-with? path "~")
        dot?   (str/starts-with? path ".")
        evar?  (str/starts-with? path "$")]
    (cond
      tilde? (str/replace path #"~" (System/getProperty "user.home"))
      dot?   (.getCanonicalPath (java.io.File. path))
      evar?  (let [rep (re-find #"[^\$][^/]*" path)
                   val (System/getenv rep)]
               (if val
                 (-> (str/replace path rep val)
                     (str/replace #"\$" ""))))
      :else path)))

(defn dirname
  "Returns parent dir of `path`"
  [path]
  (.getParent (java.io.File. path)))

(defn basename
  "Returns the final segment/file part of the `path`. If `trim`
  then trim extension."
  ([path] (.getName (io/file (expand path))))
  ([path trim]
   (let [base (.getName (io/file (expand path)))]
     (cond (string? trim)
           (if (.endsWith base trim)
             (subs base 0 (- (count base) (count trim)))
             base)
           trim (let [dot (.lastIndexOf base ".")]
                  (if (pos? dot) (subs base 0 dot) base))
           :else base))))

;; Predicates -----------------------------------------------------------------
(defmacro ^:private ? [s path] `(if ~path (. ~path ~s) false))

(defn exists?
  "Predicate that returns true if the given `path` exists"
  [path]
  (? exists (io/file (expand path))))

(defn isdir?
  "Predicate that returns true if the given `path` is a directory"
  [path]
  (? isDirectory (io/file (expand path))))

(defn isfile? ;; FIXME
  "Predicate that returns true if the given `path` is a file"
  [path]
  (? isFile (io/file (expand path))))

(defn executable?
  "Return true if `path` is executable."
  [path]
  (? canExecute (io/file (expand path))))

(defn readable?
  "Return true if `path` is readable."
  [path]
  (? canRead (io/file (expand path))))

(defn writeable?
  "Return true if `path` is writeable."
  [path]
  (? canWrite (io/file (expand path))))

(defn hidden?
  "Return true if `path` is hidden."
  [path]
  (? isHidden (io/file (expand path))))

;; ----------------------------------------------------------------------------

(defn ls
  "List files and directories in `path`, if `rec` include nested level"
  ;; TODO: add pattern matching
  [path & rec]
  (let [p (io/file (expand path))]
    (if-not rec
      (seq (.list p))
      (map #(.getPath %)
        (file-seq p)))))

(defn rm
  "Delete given path weather its a file or directory."
  [path & [silently]]
  (when (exists? path)
    (let [fp (expand path)]
      (when (isdir? (io/file fp))
        (doseq [p2 (.listFiles (io/file fp))]
          (rm p2 silently)))
      (io/delete-file fp silently))))

(defn cp
  [from to]
  (let [f (expand from)
        t (expand to)]
    (when (exists? f)
      (if (isfile? f)
        (io/copy (io/file f)
                 (io/file t))
        (-> (process ["cp" "-r" f t])))
      to)))

(defn mv
  "Rename `old` to `new`."
  [old new]
  (let [o (expand old)
        n (expand new)]
    (.renameTo (io/file o)
               (io/file n))))

(defn mkdir
  "Create a directory from the given `path.` `path` can be nested directory
  tree, relative, with env variable like $HOME/, or signle dirname, which will
  create the path in current directory."
  ;; TODO: add options regarding premisions
  [path]
  (? mkdirs (io/file (expand path))))

(defn touch
  "Create new file."
  ;; TODO: add options regarding premisions
  [file]
  (? createNewFile (io/file (expand file))))

(defn ensure
  "Given a path, ensure it exists then return the path. if kind is provided
  then skip checking for the type of the path"
  ([path] (let [kind (if (re-find #"\.\w+" path) :file :dir)]
            (ensure path kind)))
  ([path kind]
   (when-not (exists? path)
     (if (= :dir kind)
       (mkdir path)
       (touch path)))
   (expand path)))

(defn link
  "Given a `source` and `target`, create new symbolic link."
  ([source target]
   (link source target true))
  ([source target force]
   (let [s (expand source)
         t (expand target)]
     (when (and force (exists? t)) ;; TODO: should change to moving the file to backup dir.
       (slurp (:out (process ["rm" "-rf" t]))))
     (if-not (exists? t)
       (-> (process ["ln" "-s" s t]) :out slurp str/blank?)
       (throw (Exception. (str "File or directory exists at " "'" t "'")))))))

(defn readfile
  "Read file content into seq."
  ;; FIXME: delete empty items
  [file]
  (letfn [(helper [rdr]
                  (lazy-seq
                    (if-let [line (.readLine rdr)]
                      (cons line (helper rdr))
                      (do (.close rdr) nil))))]
         (helper (io/reader file))))

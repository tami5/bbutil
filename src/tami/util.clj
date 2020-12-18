(ns tami.util
  "General Utils")

(defn datetime
  "Returns a data-time formated string. If `f` is provide
  then it will be used for formating"
  ([] (datetime (name 'yyyy-MM-dd_hh:mm)))
  ([f] (.format (java.time.ZonedDateTime/now)
           (java.time.format.DateTimeFormatter/ofPattern f))))

(defn uuid
  "Generates uuid and return it as a string."
  [] (str (java.util.UUID/randomUUID)))

(defn log
  "Log type, activity and date to .local/libtam/history."
  [type activity])

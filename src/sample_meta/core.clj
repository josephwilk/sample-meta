(ns sample-meta.core
  (:import [java.security MessageDigest])
  (:require [clojure.java.jdbc :as j]
            [clojure.java.io :as io]
            [sample-meta.aubio :as dsp]))

(def ^{:dynamic true} *default-hash* "SHA-256")

(defn hexdigest
  "Returns the hex digest of an object. Expects a string as input."
  ([input] (hexdigest input *default-hash*))
  ([input hash-algo]
     (if (string? input)
       (let [hash (MessageDigest/getInstance hash-algo)]
         (. hash update (.getBytes input))
         (let [digest (.digest hash)]
           (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))
       (do
         (println "Invalid input! Expected string, got" (type input))
         nil))))

(defn compare-sha256 [obj ref-hash]
  "Compare an object to a hash; true if (= (hash obj) ref-hash)."
  (= ref-hash (hexdigest obj "SHA-256")))

(def sha256 (fn [input] (hexdigest input "SHA-256")))

(def mysql-db {:subprotocol "mysql"
               :subname "//127.0.0.1:3306/repl_electric_samples"
               :user "root"
               :password ""})

(def sample-root "/Users/josephwilk/Workspace/music/samples/")

(defn find-type [sample-path]
  (let [parts (clojure.string/split sample-path #"/")
        path (clojure.string/join "/" (butlast parts))
        ]
    (if (re-find #"(?i)one-shot|one_shot|oneshot|one shot" sample-path)
      "one_shot"
      (if (re-find #"(?i)loop|loops" path)
        "loop"
        "unknown"))))

(defn find-sample-set [sample-root]
  (map
   (fn [file]
     (let [p (.getPath file)
           file-parts (clojure.string/split p #"/")
           collection (let [collection (nth file-parts 6)]
                        (if (clojure.string/includes? collection ".wav")
                          "root"
                          collection))]
        [(sha256 p) collection (find-type p) (find-note p) p]))
   (filter #(.endsWith (.getName %) ".wav") (file-seq (io/file sample-root)))))

(defn find-note [sample]
  (let [file (last (clojure.string/split sample #"/"))
        match (re-find #"(?i)_A_|_A#_|_B_|_C_|_C#_|_D_|_D#_|_E_|_F_|_F#_|_G_|_G#_|^A_|^A_|^A#_|^B_|^C_|^C#_|^D_|^D#_|^E_|^F_|^F#_|^G_|^G#_" file)]
    (when match
      (let [match-parts (char-array (str match))
            note (loop [note ""
                        parts match-parts]
                   (if (seq parts)
                     (let [part  (str (first parts))]
                       (if (re-find #"_" part)
                         (recur note (drop 1 parts))
                         (recur (str note part) (drop 1 parts))))
                     note))]
        note))))

(str (first (char-array "asdasd")))

(defn import-samples [path]
  (let [batch-size 1000
        samples (find-sample-set path)
        insert-fn (fn [s] (j/insert-multi! mysql-db :samples ["guid" "collection" "type" "note" "path"] s))]
    (println (str "total samples: " (count samples)))
    (loop [samples samples]
      (print ".")
      (let [s (take batch-size samples)]
        (when (seq s)
          (insert-fn s)
          (recur (drop batch-size samples)))))
    :DONE))

(defn import-onsets []
  (let [results (j/query mysql-db "select id,path from samples limit 10")

        ]
    (doseq [result results]
      (println  (dsp/onsets (:path result))))
    ;;(j/insert! :onsets [:id :onset_time] [(:id result) (dsp/onsets (:path result))])
    ))


(comment
  (j/execute! mysql-db "TRUNCATE samples;")
  (find-note "/Users/josephwilk/Workspace/music/samples/Abstract/One Shots/Tonal/C#_DryTone_SP.wav")
  (import-samples sample-root)
  (import-onsets)
  )

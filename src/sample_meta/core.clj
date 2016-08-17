(ns sample-meta.core
  (:import [java.security MessageDigest])
  (:require [clojure.java.jdbc :as j]
            [clojure.java.io :as io]

            [sample-meta.aubio :as dsp]
            [sample-meta.sox   :as dsp-stats]))

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

(defn find-drum [part parts]
  (let [file (last parts)
        snare (re-find #"(?i)snare" file)
        hat (re-find #"(?i)hat" file)
        kick (re-find #"(?i)kick" file)
        perc (re-find #"(?i)perc" file)
        claps (re-find #"(?i)claps" file)]
    (if snare
      "snare_hit"
      (if hat
        "hat_hit"
        (if kick
          "kick_hit"
          (if perc
            "perc_hit"
            (when claps
              "clap_hit")))))))


(defn find-drum-type [sample-path parts] (find-drum sample-path parts))
(defn find-unknown [path parts]          (find-drum path parts))
(defn find-oneshot [path parts]          (find-drum path parts))

(defn find-type [sample-path]
  (let [parts (clojure.string/split sample-path #"/")
        path (clojure.string/join "/" (butlast parts))]
    (if (re-find #"(?i)one-shot|one_shot|oneshot|one shot" sample-path)
      ["one_shot" (find-oneshot path parts)]
      (if (re-find #"(?i)loop|loops" path)
        ["loop"]
        (if (re-find #"(?i)drum hits|drum_hits" path)
          ["drum_hit" (find-drum-type path parts)]
          ["unknown" (find-unknown path parts)])))))

(defn find-note [sample]
  (let [file (last (clojure.string/split sample #"/"))
        file  (first (clojure.string/split file #"\..+$"))

        ;;Avert your gaze. Imagine this was a wonderful parser which extracted notes.
        octave-match (re-find #"(?i)0_A_|_A(?:#|s)_|_B_|_C_|_C(?:#|s)_|_D_|_D_|_E_|_F_|_F(?:#|s)_|_G_|_G(?:#|s)_|^A_|^A_|^A(?:#|s)_|^B_|^C_|^C(?:#|s)_|^D_|^D(?:#|s)_|^E_|^F_|^F(?:#|s)_|^G_|^G(?:#|s)_|_A$|_A(?:#|s)$|_B$|_C$|_C(?:#|s)$|_D$|_D(?:#|s)$|_E$|_F$|_F(?:#|s)$|_G$|_G(?:#|s)$|_A\d+_|_A(?:#|s)\d+_|_A(?:#|s)\d+_|_B\d+_|_C\d+_|_C(?:#|s)\d+_|_D\d+_|_D(?:#|s)\d+_|_E\d+_|_F\d+_|_F(?:#|s)\d+_|_G\d+_|_G(?:#|s)\d+_|^A\d+_|^A\d+_|^A(?:#|s)\d+_|^B\d+_|^C\d+_|^C(?:#|s)\d+_|^D\d+_|^D(?:#|s)\d+_|^E\d+_|^F\d+_|^F(?:#|s)\d+_|^G\d+_|^G(?:#|s)\d+_|_A\d+$|_A(?:#|s)\d+$|_B\d+$|_C\d+$|_C(?:#|s)\d+$|_D\d+$|_D(?:#|s)\d+$|_E\d+$|_F\d+$|_F(?:#|s)\d+$|_G\d+$|_G(?:#|s)\d+$" file)
        note-match octave-match]
    (when note-match
      (let [match-parts (char-array (str note-match))
            note (loop [note ""
                        parts match-parts
                        octave ""]
                   (if (seq parts)
                     (let [part  (str (first parts))]
                       (if (re-find #"_" part)
                         (recur note (drop 1 parts) octave)

                         (if (re-find #"\d" part)
                           (let [octave (str octave part)]
                             (recur note (drop 1 parts) octave))
                           (recur (str note part) (drop 1 parts) octave))))
                     {:note (clojure.string/replace note #"s" "#") :octave octave}))
            octave (if (clojure.string/blank? (:octave note))
                     nil
                     (Integer/parseInt (:octave note)))]
        {:note (:note note) :octave octave}))))

(comment
  (find-note "/Users/josephwilk/Workspace/music/samples/Ambi/chords/70bpm/am_chrd70_noir_C#3.wav")
  )

(defn find-collection [sample]
  (let [file-parts (clojure.string/split sample #"/")
        collection (nth file-parts 6)]
    (if (clojure.string/includes? collection ".wav")
      "root"
      (clojure.string/lower-case collection))))

(defn find-filename [sample]
  (let [file-parts (clojure.string/split sample #"/")
        filename (last file-parts)
        filename-without-extension (first (clojure.string/split filename #"\..*$"))]
    (clojure.string/lower-case filename-without-extension)))

(defn find-bpm [sample]
  (let [filename (find-filename sample)
        matches (re-matches #"^(\d+)_.*" filename)
        matches (or matches (re-find #"bpm(\d+)|(\d+)bpm" sample))]
    (when (seq matches)
      (Integer/parseInt (last matches)))))

(defn find-length [sample] (get (dsp/info sample) :length))

(defn all-wavs [root] (filter #(.endsWith (.getName %) ".wav") (file-seq (io/file sample-root))))

(defn sample-data-set [samples]
  (map
   (fn [file]
     (let [;;p (.getPath file)
           collection (find-collection file)
           {note :note octave :octave} (find-note file)
           filename (find-filename file)
           bpm (find-bpm file)
           length (find-length file)

           types (find-type file)
           main-type (first types)
           sub-type (or (second types) main-type)

           stats (dsp-stats/stats file)

           pitch-stats (dsp/find-pitch file -80.0) ;;Filter out quiet sounds
           notes (:notes pitch-stats)

           note-1 (nth notes 0 nil)
           note-2 (nth notes 1 nil)
           note-3 (nth notes 2 nil)
           note-4 (nth notes 3 nil)]
       (println notes)
       [(sha256 file) file collection filename length main-type sub-type note octave bpm

        (get stats "Rough note")

        note-1 note-2 note-3 note-4

        (get stats "Mean amplitude")
        (get stats "Maximum amplitude")
        (get stats "Minimum amplitude")

        (get stats "RMS amplitude")
        (get stats "Volume adjustment")
        (get stats "RMS delta")
        ]))
   samples))

(defn abs [x]
  (if (> x 0) x (* -1 x)))

(defn import-samples
  ([] (import-samples (all-wavs sample-root)))
  ([samples]
     (let [batch-size 1000
           samples (sample-data-set samples)
           insert-fn
           (fn [s]
             (j/insert-multi!
              mysql-db :samples
              ["guid"  "path" "collection" "filename" "length" "type" "subtype" "note" "octave" "bpm"
               "rough_note"

               "note1"
               "note2"
               "note3"
               "note4"

               "mean_amplitude" "max_amplitude" "min_amplitude"
               "rms_amplitude"
               "volume_adjustment"
               "rms_delta"] s))]
       (loop [samples samples]
         (flush)
         (let [s (take batch-size samples)]
           (when (seq s)
             (insert-fn s)
             (print ".")
             (recur (drop batch-size samples)))))
               :DONE)))


(defn import-samples-with-scales []
  (let [results (j/query mysql-db "select id,path from samples")]
    (println (count results))
    (doall
     (pmap
      (fn [result]
        (let [sample (:path result)

              stats (dsp/find-pitch sample -80.0)

              scales (:scale stats)
              notes  (:notes stats)

              note-1 (nth notes 0)
              note-2 (nth notes 1)
              note-3 (nth notes 2)
              note-4 (nth notes 3)

              collection (find-collection sample)
              filename (find-filename sample)]
          (doseq [scale scales]
            (j/insert! mysql-db
                       :samples_scales
                       [:sample_id :path :collection :filename :scale :root :note1 :note2 :note3 :note4]
                       [(:id result) (:path result) collection filename (:scale scale) (:root scale) note-1 note-2 note-3 note-4]))))
      results))))


(defn import-onsets []
  (let [results (j/query mysql-db "select id,path from samples")]
    (doall
     (pmap
      (fn [result]
        (let [onsets (:onsets (dsp/onsets (:path result)))
              collection (find-collection (:path result))
              filename (find-filename (:path result))]
          (doseq [onset onsets]
            (j/insert! mysql-db :onsets [:sample_id :path :collection :filename :onset_time] [(:id result) (:path result) collection filename onset]))))
      results))))

(defn import-notes
  ([] (import-notes 0.3 256))
  ([onset-threshold hop-size]
     (let [results (j/query mysql-db "select id,path from samples")]
       (doall
        (pmap
         (fn [result]
           (let [note-data (dsp/notes (:path result) onset-threshold hop-size)
                 notes (:notes note-data)
                 onset (:onset note-data)
                 collection (find-collection (:path result))
                 filename (find-filename (:path result))
                 table (if (or (< onset-threshold 0.3)
                               (< hop-size 256))
                         :notes_fine
                         :notes)]
             (doseq [note notes]
               (let [length (abs (- (:offset note) (:onset note)))
                     perc (if (< length 0.05) 1 0)]
                 (j/insert! mysql-db table [:sample_id :path :collection :filename :onset :offset :length :midi :note :octave :perc] [(:id result) (:path result) collection filename (:onset  note) (:offset note) length (:midi note) (:note note)  (:octave note) perc])))))
         results)))))

(defn import-cuts []
  (let [results (j/query mysql-db "select id,path from samples")]
    (doall
     (pmap
      (fn [result]
        (let [note-data (dsp/cuts (:path result))
              beats (:beats note-data)
              collection (find-collection (:path result))
              filename (find-filename (:path result))]
          (doseq [beat beats]
            (j/insert! mysql-db :cuts [:sample_id :path :collection :filename :beat] [(:id result) (:path result) collection filename beat]))))
      results))))

(defn import-track []
  (let [results (j/query mysql-db "select id,path from samples")]
    (doall
     (pmap
      (fn [result]
        (let [note-data (dsp/cuts (:path result))
              beats (:beats note-data)
              collection (find-collection (:path result))
              filename (find-filename (:path result))]
          (doseq [beat beats]
            (j/insert! mysql-db :track [:sample_id :path :collection :filename :beat] [(:id result) (:path result) collection filename beat])
                 beats)))
      results))))

(comment
;;  (j/execute! mysql-db "TRUNCATE samples;")
  (j/execute! mysql-db "TRUNCATE samples_scales;")

  (j/execute! mysql-db "TRUNCATE onsets;")
  (j/execute! mysql-db "TRUNCATE notes;")
  (j/execute! mysql-db "TRUNCATE notes_fine;")
  (j/execute! mysql-db "TRUNCATE cuts;")
  (j/execute! mysql-db "TRUNCATE track;")


  (find-note "/Users/josephwilk/Workspace/music/samples/Abstract/One Shots/Tonal/C#3_DryTone_SP.wav")

  (find-note "/Users/josephwilk/Workspace/music/samples/Abstract/One Shots/Tonal/38_C_StutteringFM8_SP.wav")

  (find-sample-set "/Users/josephwilk/Workspace/music/samples/")

  (find-filename "/Users/josephwilk/Workspace/music/samples/Abstract/One Shots/Tonal/C#_DryTone_SP")

  (do
    (let [existing-samples (set (map :path (j/query mysql-db "SELECT path from samples;")))
          fs-samples (set (map (fn [f] (.getPath f)) (all-wavs sample-root)))
          new-samples (clojure.set/difference fs-samples existing-samples)]
      (println (str "New samples:" (first new-samples)))
      (import-samples new-samples)
      )


    ;;(import-notes 0.1 64)
    (import-samples-with-scales)
    )

  (do
    (import-onsets)
    ;;(def n (future (import-notes)))
    (import-cuts)
    (import-track)
    (import-onsets)
    )
  )

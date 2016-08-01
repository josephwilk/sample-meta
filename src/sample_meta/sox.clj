(ns sample-meta.sox
  (:require [clojure.java.shell :as shell]
            [overtone.music.pitch :as pitch]))

(defn truncate
  [s n]
    (subs s 0 (min (count s) n)))

(defn stats [sample]
  (let [o (shell/sh "sox" sample "-n" "stat" "-rms")]
    (if (= (:exit o) 0)
      ;;For some reason this comes to err...
      (let [out (->
                 (:err o)
                 (clojure.string/split #"\n"))
            out (reduce (fn [acc s]
                          (let [s (clojure.string/replace s #"\s+" " ")
                                data (->> (clojure.string/split s #":")
                                          (map clojure.string/trim))
                                string-float (last data)
                                num (when (and
                                           (not (clojure.string/blank? string-float))
                                           (not= string-float "Can't guess the type"))
                                      (try (Double/parseDouble string-float)
                                           (catch Exception e
                                             (println e)
                                             nil)))]
                            (assoc acc (first data) num)))
                        {}
                        out)
            out (assoc out "Rough note" (try
                                          (truncate
                                           (name (pitch/find-note-name (pitch/hz->midi (get out "Rough frequency"))))
                                           8)
                                          (catch Exception e
                                            nil)
                                          ))]

        out)
      (do
        (println (:err o))
        {}))))

(comment
  (stats "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav")
  (shell/sh "sox" "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav" "-n" "stat" "-rms")
  (clojure.string/replace "asdas       " #"\s+" " ")


  (stats  "/Users/josephwilk/Workspace/music/samples/Alto/Samples/Mic 1 Cardioid/Phrases/Vowel/80 minor/4 E/vor_alto_phrases_vowel_120_minor_E_10.wav")

)

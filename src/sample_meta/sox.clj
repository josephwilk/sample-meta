(ns sample-meta.sox
  (:require [clojure.java.shell :as shell]
            [overtone.music.pitch :as pitch]))

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
                                _ (println data)
                                num (try (Double/parseDouble (last data))
                                         (catch Exception e
                                           (println e)
                                           0.0))]
                            (assoc acc (first data) num)))
                        {}
                        out)
            out (assoc out "Rough note" (pitch/find-note-name (pitch/hz->midi (get out "Rough frequency"))))]

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

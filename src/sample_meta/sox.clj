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
                             num (Double/parseDouble (last data))]
                            (assoc acc (first data) num)))
                        {}
                        out)
            out (assoc out "Rough note" (pitch/find-note-name (pitch/hz->midi (get out "Rough frequency"))))]

        {:stats out})
      (do
        (println (:err o))
        {:stats []}))))

(comment
  (stats "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav")
  (shell/sh "sox" "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav" "-n" "stat" "-rms")
  (clojure.string/replace "asdas       " #"\s+" " ")
)

(ns sample-meta.aubio
  "Using https://aubio.org/"
  (:require [clojure.java.shell :as shell]))

(defn onsets [sample]
  (let [o (shell/sh "aubioonset" sample)]
    (if (= (:exit o) 0)
      (let [out (->> (clojure.string/split (:out o) #"\n")
                     (map (fn [string]
                            (if (clojure.string/blank? string)
                              0.0
                              (Double/parseDouble string)
                              ))))]
        {:onsets out})
      (do
        (println (:err o))
        {:onsets []})
      )))

(defn notes [sample]
  (let [o (shell/sh "aubionotes" sample)
        [head & [tail]] (clojure.string/split (:out o) #"\n")
        tails (->> (clojure.string/split tail #"\t")
                  (map (fn [string] (Double/parseDouble string))))]
    {:onset head
     :notes tails}))

(comment
  (notes "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav")
  (onsets "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav")

  (onsets "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Mel")


  )

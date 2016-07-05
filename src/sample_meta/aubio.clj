(ns sample-meta.aubio
  "Using https://aubio.org/"
  (:require [clojure.java.shell :as shell]
            [overtone.music.pitch :as pitch]))

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
  (let [o (shell/sh "aubionotes" sample)]
    (if (= (:exit o) 0)
      (let [[head & [tail]] (clojure.string/split (:out o) #"\n")
            head (Double/parseDouble head)
            data (if (seq tail)
                   (let [tails (->> (clojure.string/split tail #"\t")
                                    (map (fn [string] (Double/parseDouble string))))
                         data (partition 3 tails)
                         data (map (fn [[midi on off]]
                                     (let [note-name (name (pitch/find-note-name (Math/round midi)))]
                                       {:midi midi :onset on :offset off :note note-name})) data)]
                     data)
                   [{:midi 0.0 :onset head :offset 0.0 :note ""}])]
        {:onset head
         :notes data})
      (do
        (println (:err o))
        {:onset 0.0 :notes {:midi 0.0 :onset 0.0 :offset 0.0 :note ""}}))))

(comment
  (notes "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav")
  (onsets "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav")

  (onsets "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Mel")

  (notes "/Users/josephwilk/Workspace/music/samples/33ReverseFX_Wav_SP/Samples/ReverseTexture_02_SP.wav")
  )

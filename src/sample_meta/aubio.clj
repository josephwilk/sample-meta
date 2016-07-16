(ns sample-meta.aubio
  "Using https://aubio.org/ or afinfo"
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

(defn info [sample]
  (let [o (shell/sh "afinfo" sample)]
    (if (= (:exit o) 0)
      (let [out (->> (:out o)
                     (re-find #"estimated duration: (\d+.\d+) sec")
                     (last)
                     (Double/parseDouble))]
        {:length out})
      {})))

(defn track [sample]
  (let [o (shell/sh "aubiotrack" sample)]
    (if (= (:exit o) 0)
      (let [out (->> (clojure.string/split (:out o) #"\n")
                     (map (fn [string]
                            (if (clojure.string/blank? string)
                              0.0
                              (Double/parseDouble string)))))]
        {:beats out})
      (do
        (println (:err o))
        {:beats []}))))


(defn cuts [sample]
  (let [o (shell/sh "aubiocut" sample)]
    (if (= (:exit o) 0)
      (let [out (->> (clojure.string/split (:out o) #"\n")
                     (map (fn [string]
                            (if (clojure.string/blank? string)
                              0.0
                              (Double/parseDouble string)))))]
        {:beats out})
      (do
        (println (:err o))
        {:beats []}))))


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
                                     (let [note-name (name (pitch/find-note-name (Math/round midi)))
                                           octave (Integer/parseInt (str (last note-name)))
                                           note-name (-> note-name
                                                         butlast
                                                         clojure.string/join
                                                         clojure.string/upper-case
                                                         (clojure.string/replace #"AB" "G#"))]
                                       {:midi midi :onset on :offset off :note note-name :octave octave})) data)]
                     data)
                   [{:midi 0.0 :onset head :offset 0.0}])]
        {:onset head
         :notes data})
      (do
        (println (:err o))
        {:onset 0.0 :notes {:midi 0.0 :onset 0.0 :offset 0.0}}))))

(comment
  (notes "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav")
  (onsets "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav")

  (onsets "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Mel")

  (info "/Users/josephwilk/Workspace/music/samples/33ReverseFX_Wav_SP/Samples/ReverseTexture_02_SP.wav")

  (notes "/Users/josephwilk/Workspace/music/samples/33ReverseFX_Wav_SP/Samples/ReverseTexture_02_SP.wav")
  (cuts "/Users/josephwilk/Workspace/music/samples/33ReverseFX_Wav_SP/Samples/ReverseTexture_02_SP.wav")
  (track "/Users/josephwilk/Workspace/music/samples/33ReverseFX_Wav_SP/Samples/ReverseTexture_02_SP.wav")
  )

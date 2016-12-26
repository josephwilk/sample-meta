(ns sample-meta.aubio
  "Using https://aubio.org/ or afinfo"
  (:require [clojure.java.shell :as shell]
            [overtone.music.pitch :as pitch]

            [clojure.data :as data]))

(defn note-without-flat [note]
  (-> note
      pitch/find-note-name
      name
      (clojure.string/replace "Ab" "G#")
      (clojure.string/replace "Eb" "D#")
      (clojure.string/replace "Bb" "A#")))

(defn find-note [freq-str]
  (let [freq (Double/parseDouble freq-str)]
    (when (> freq 0.0)
      (let [note (pitch/hz->midi freq)]
        (note-without-flat note)))))

(defn scale-notes [root scale]
  (->>
   (take 7 (drop 7 (pitch/scale-field root scale)))
   (map note-without-flat)
   (map (fn [note] (clojure.string/join (re-find #"[^\d]+" (name note))))))
  )

(def all-scales
  (map (fn [[note scale]] {:scale scale :root note :notes (vec (scale-notes note scale))})
       (mapcat (fn [n]
                 (map (fn [s] [n s])
                      ;;(keys pitch/SCALE)
                      [:major :minor]
                      ))
               [:C :C# :D :D# :E :F :F# :G :G# :A :A# :B] )))

(defn find-scale [notes]
  (if (seq notes)
    (let [scales-scored
          (reverse
           (sort-by
            :matches
            (filter
             (fn [scale] (= (:matches scale) 3));;Match a chord
             (map
              (fn [{scale-notes :notes root :root scale :scale}
                  ]
                (let [matches (clojure.set/intersection (set scale-notes) (set notes))
                      differences (clojure.set/difference (set notes) (set scale-notes))]
                  ;; (println root scale matches (count (vec matches)))
                  {:root (name root) :scale (name scale) :matches (count (vec matches)) :diffs (count (vec differences))}
                  ))
              all-scales))))

          top-score (get (first scales-scored) :matches)
          top-matching-candidates (take-while (fn [s] (= top-score (:matches s))) scales-scored)

          lowest-diff (get (first scales-scored) :diffs)

          top-candidates (sort-by :diffs top-matching-candidates)
          top-candidates (take-while (fn [s] (= lowest-diff (:diffs s))) top-candidates)]

      top-candidates)
    {:root nil :scale nil :notes nil}))

(comment
  (first all-scales)
  (data/diff ["F" "F#" "A#" "C" "C#" "F" "F#"] ["A" "B" "C" "D" "E" "F" "G"])
  (find-scale ["A#" "B" "D#" "C" "D"])
)

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

(defn find-pitch
  ([sample] (find-pitch sample -90.0))
  ([sample silence-threshold]
     (let [o ;;(shell/sh "aubiopitch" sample "-s" (str silence-threshold))
           (shell/sh "aubiopitch" sample "-p" "yin" "-s" (str silence-threshold))
           ]
        (if (= (:exit o) 0)
          (let [out
                (->> (clojure.string/split (:out o) #"\n")
                     (map #(clojure.string/split %1 #"\s"))
                     (map
                      (fn [[t s]]
                        (try
                          (when-let [note (find-note s)]
                            (re-find #"[^\d]+" note))
                          (catch Exception e
                            (println e)
                            []))))
                     (reduce (fn [frequencies note]
                               (if note
                                 (assoc frequencies note (inc (get frequencies note 0)))
                                 frequencies)) {})
                     (map (fn [x] (println x) x))

                     (sort-by val)
                     reverse
                     (filter (fn [[note score]] (> score 5))) ;;remove weaker notes
                     (map first)
                     (map name)
                     vec)]

            ;;(println (find-scale (map first out)))
            {:notes out ;;:scale (find-scale out)
             })
          (do
            (println (:err o))
            {:notes [] :scale {}})))))

(defn- extract-note-onset [[midi on off]]
  (let [note-name (name (pitch/find-note-name (Math/round midi)))
        octave (Integer/parseInt (str (last note-name)))
        note-name (-> note-name
                      butlast
                      clojure.string/join
                      clojure.string/upper-case
                      (clojure.string/replace #"AB" "G#")
                      (clojure.string/replace #"EB" "D#")
                      (clojure.string/replace #"BB" "A#"))]
    {:midi midi :onset on :offset off :note note-name :octave octave}))

(defn notes
  ([sample] (notes sample 0.3))
  ([sample res] (notes sample res 256))
  ([sample resolution hop-size]
     (let [o (shell/sh "aubionotes"
                       ;;(str "-t " resolution)
                       (str "-H" hop-size)
                       sample)
           [first-onset & note-onsets] (clojure.string/split (:out o) #"\n")]
       (if (and
            (not (clojure.string/blank? first-onset))
            (= (:exit o) 0))
         (let [first-onset (Double/parseDouble first-onset)
               data (if (seq note-onsets)
                      (let [onsets (->>
                                    note-onsets
                                    (map (fn [line] (clojure.string/split line #"\t")))
                                    (flatten)
                                    (map (fn [string] (Double/parseDouble string)))
                                    (partition 3)
                                    (map extract-note-onset))]
                        onsets)
                      [{:midi 0.0 :onset first-onset :offset 0.0}])]
           {:onset first-onset
            :notes data
            :threshold resolution})
         (do
           ;;            (println (:err o))
            {:onset 0.0 :notes {:midi 0.0 :onset 0.0 :offset 0.0}
             :threshold resolution})))))

(comment
  (notes "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav" 0.3 64)
  (onsets "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav")

  (println (find-pitch "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav" -80.0))

  (shell/sh "aubionotes" (str "-t " 0.1) "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Melodic/Found Sound/120_C_Lamp_01_SP.wav")

  (onsets "/Users/josephwilk/Workspace/music/samples/Abstract/Loops/Mel")

  (info "/Users/josephwilk/Workspace/music/samples/33ReverseFX_Wav_SP/Samples/ReverseTexture_02_SP.wav")

  (notes "/Users/josephwilk/Workspace/music/samples/Dirty/P021-P030/35_P26_RR01_SP.wav")

  (notes "/Users/josephwilk/Workspace/music/samples/33ReverseFX_Wav_SP/Samples/ReverseTexture_02_SP.wav")

  (cuts "/Users/josephwilk/Workspace/music/samples/33ReverseFX_Wav_SP/Samples/ReverseTexture_02_SP.wav")
  (track "/Users/josephwilk/Workspace/music/samples/33ReverseFX_Wav_SP/Samples/ReverseTexture_02_SP.wav")
  )

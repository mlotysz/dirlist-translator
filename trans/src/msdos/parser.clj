(ns msdos.parser
  (:require [instaparse.core :as insta]
            [clojure.core.reducers :as r]
            [iota :as iota]))

(def parser-definition "resources/msdoslist.ebnf")
(def data-file "../data/dirlist.unix")
(def extensions
  "Known file extensions to be investigated"
  ["dwg" "dxf"                  ;; AutoCad 2D
   "prt" "asm" "drw"            ;; Pro/E (+ extra 1-999)
   "ipt" "iam" "idw"            ;; Inventor
   "stp" "step" "igs" "iges"    ;; export 3D
   "sldprt" "sldasm" "slddrw"   ;; SolidWorks
   "par" "psm" "asm"            ;; SolidEdge
   ])

(def stop-words-exact
  #{"CAD" "GSBCS" "Japan" "NOT" "OldVersion" "NTITEST" "AIP" "SNB" "PF" "ISO" "SNBx" "GEVIND" "GB"
    "DIN" "SMC" "JIS" "Jill" "jill" "JillArmUpper" "JillPalm" "JillLegLower" "BSI" "DEFAULT" "STK" "XXXX" "TANDHJUL" "AIT" "DEKSEL" "Frank" "Autodesk" "INA"
    "Workspaces" "manget" "Uni-Helper" "en-US" "en-us" "dwgviewr"})

(def known-names
  #{"Systec" "QINGDAO" "SNIT" "peterb" "Soliflex" "KS-ADAPTERPLA" })

(def english-words
  #{"Rooms" "Floor" "Cooling System" "molding machine" "Concrete Pillars" "Part"
    "Assembly" "Ball-Screw" "Tube and Pipe" "MANIFOLD" "Rack" "Generic" "handwheels"
    "Moulding floor" "Hand" "Root" "Template" "Top" "Washer" "Led" "OldVersions" "test" "unnamed"
    "Glider" "Alpha" "mangnet" "Drawing" "Gear" "Motor" "Library" "harness" "Feet" "Travers" "brush" "Metric"
    "WASHER" "Process" "recommended" "Lock" "Fodder" "English" "GOST" "rod" "JillTorseauLower" "top"
    "standatd" "Div" "PLXshort" "Break" "backdrop" "Standard" "standard" "Hoved" "Libraries" "UserDataCache"
    "files" "Molding machines" "Snap On bord" "Design Accelerator" "Piovan gravimetric blender" "Drying system Piovan" "Content Center Files"
    "Mold Design" "piping runs" "hole" "installation tower" "Workspace" " reload"})

(def parse
  (insta/parser (slurp parser-definition)))

(defn predicate-test
  [fns t]
  (some true? ((apply juxt fns) t)))

(defn known-extension? [^String line]
  (reduce (fn [curr ^String ext]
            (or curr (.endsWith line ext))) false extensions))

(defn string-contains?
  [words ^String token]
  (reduce (fn [curr word]
            (or curr (.contains token word))) false words))

(defn only-alpha?
  [^String s]
  (re-matches #"[a-zA-Z \-]+" s))

(defn garbage?
  "Things we don't want to translate"
  [s]
  (predicate-test
   [#(known-extension? %)
    #(not (only-alpha? %))
    #(> 3 (.length %1))
    #(contains? stop-words-exact %)
    #(string-contains? english-words %)
    #(string-contains? known-names %)]
   s))

(defn name-token
  "Parser returns :name for interesting tokens here we extract values "
  [tokens]
  (->> tokens
       (filter #(= :name (first %)))
       (map second)
       (filter #(not (garbage? %)))
       ))

(def process
  (->> (iota/seq data-file)
          (r/filter known-extension?)
          (r/map parse)
          (r/map name-token)
          (r/reduce into #{})
          ))

(defn save-translation-list
  [out data]
  (spit out data))

(comment

  (predicate-test [integer? pos?] 0)

  (contains? known-names "Systec")

  (known-extension? "dwg")
  (garbage? "dwg")
  (garbage? "CAD")
  (garbage? "Rooms")
  (only-alpha? "aaasas")
  process

  (save-translation-list "translate.txt" process)

  ;;
  )

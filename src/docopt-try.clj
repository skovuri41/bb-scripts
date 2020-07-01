(require '[docopt.core :as docopt])
(def usage "Naval Fate.
Usage:
  naval_fate ship new <name>
  naval_fate ship <name> move <lat> <long> [--speed=<kn>]
  naval_fate mine (set|remove) <lat> <long> [--moored|--drifting]
  naval_fate -h | --help
  naval_fate --version
Options:
  -h --help     Show this screen.
  --version     Show version.
  --speed=<kn>  Speed in knots [default: 10].
  --moored      Moored (anchored) mine.
  --drifting    Drifting mine.")

;; (docopt/docopt usage
;;                *command-line-args*
;;                (fn [arg-map] (clojure.pprint/pprint arg-map)))


(docopt/docopt usage
               *command-line-args*
               (fn [arg-map] (cond
                              (or (nil? arg-map)
                                  (arg-map "--help")) (println (:doc     usage))
                              (arg-map "--version")   (println (:version usage))
                              (arg-map "mine")        (println (if (arg-map "set") "Set" "Remove")
                                                               (cond
                                                                 (arg-map "--moored")   "moored"
                                                                 (arg-map "--drifting") "drifting")
                                                               "mine at (" (arg-map "<x>") ", " (arg-map "<y>") ").")
                              (arg-map "new")         (println "Create new"
                                                               (let [[name & more-names :as names] (arg-map "<name>")]
                                                                 (if (seq more-names)
                                                                   (str "ships " (clojure.string/join ", " names))
                                                                   (str "ship " name)))
                                                               ".")
                              (arg-map "shoot")       (println "Shoot at (" (arg-map "<x>") "," (arg-map "<y>") ").")
                              (arg-map "move")        (println "Move" (first (arg-map "<name>"))
                                                               "to (" (arg-map "<x>") "," (arg-map "<y>")
                                                               (if-let [speed (arg-map "--speed")]
                                                                 (str " ) at " speed " knots.")
                                                                 " )."))
                              true                    (throw (Exception. "This ought to never happen.\n")))))


;; (let [arg-map (docopt args)] ; with only one argument, docopt parses -main's docstring.
;;   (cond
;;     (or (nil? arg-map)
;;         (arg-map "--help")) (println (:doc     (meta #'-main)))
;;     (arg-map "--version")   (println (:version (meta #'-main)))
;;     (arg-map "mine")        (println (if (arg-map "set") "Set" "Remove")
;;                                      (cond
;;                                        (arg-map "--moored")   "moored"
;;                                        (arg-map "--drifting") "drifting")
;;                                      "mine at (" (arg-map "<x>") ", " (arg-map "<y>") ").")
;;     (arg-map "new")         (println "Create new"
;;                                      (let [[name & more-names :as names] (arg-map "<name>")]
;;                                        (if (seq more-names)
;;                                          (str "ships " (clojure.string/join ", " names))
;;                                          (str "ship " name)))
;;                                      ".")
;;     (arg-map "shoot")       (println "Shoot at (" (arg-map "<x>") "," (arg-map "<y>") ").")
;;     (arg-map "move")        (println "Move" (first (arg-map "<name>"))
;;                                      "to (" (arg-map "<x>") "," (arg-map "<y>")
;;                                      (if-let [speed (arg-map "--speed")]
;;                                        (str " ) at " speed " knots.")
;;                                        " )."))
;;     true                    (throw (Exception. "This ought to never happen.\n"))))

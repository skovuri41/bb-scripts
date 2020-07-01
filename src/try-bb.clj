#!/usr/local/bin/bb
(ns medley-test
  {:author "shyam k"}
  (:require [clojure.data.xml :as xml]
            [clojure.string :as str]
            [babashka.classpath :refer [add-classpath]]
            [clojure.java.shell :refer [sh]]
            [clojure.java.io :as io]
            [clojure.pprint :refer [print-table]]
            [clojure.tools.cli :refer [parse-opts]]
            [cheshire.core :as json]
            [babashka.curl :as curl]
            [babashka.pods :as pods]))

(comment
  (add-classpath "/nas/projects/babashka/medley-1.3.0.jar")
  (require '[medley.core :as m])

  (pods/load-pod "/nas/projects/babashka/pod-babashka-hsqldb")
  (require '[pod.babashka.hsqldb :as jdbc]
           '[pod.babashka.hsqldb.sql :as sql])

  (m/index-by :id [{:id 1} {:id 2}])
  (m/abs -4)
  (m/map-keys inc {1 :A 2 :b 3 :c})
  (m/map-kv (fn [k v] [(name k) (inc v)]) {:a 1 :b 2})
  (m/map-keys name (sorted-map :a 1 :b 2))
  (m/least 3 2 5 -1 0 2)
  (m/greatest 3 2 5 -1 0 2)
  (m/deep-merge {:a {:b [1 2]}} {:a {:b [3 4]}})

  (m/interleave-all [1 2 3] [4 5 6])
  (m/insert-nth 1 :a [1 2 3 4])
  (m/remove-nth 1 [1 2 3 4])
  (m/replace-nth 1 :a [1 2 3 4])
  (m/map-kv-keys + (sorted-map 1 2, 2 4))
  (m/map-kv-vals + (sorted-map 1 2, 2 4))
  (m/filter-kv (fn [k v] (= v 2)) (sorted-map "a" 1 "b" 2))
  (m/filter-vals even? (sorted-map :a 1 :b 2))


  (->
    (curl/get "https://postman-echo.com/get" {:query-params {"q" "clojure"}})
    :body
    (json/parse-string true)
    :args)

  (:body (curl/get "https://postman-echo.com/basic-auth" {:basic-auth ["postman" "password"]}))

  (->
    (curl/post "https://postman-echo.com/post"
               {:form-params {"filename" "somefile" "file" (io/file "/nas/projects/babashka/icon.png")}})
    :body
    (json/parse-string)
    (get "files")
    (contains? "icon.png")))

(def cli-options
  ;; An option with a required argument
  [["-p" "--port PORT" "Port number"
    :default 80
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ;; A non-idempotent option (:default is applied first)
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc]    ; Prior to 0.4.1, you would have to use:
   ;; :assoc-fn (fn [m k _] (update-in m [k] inc))
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(:options (parse-opts *command-line-args* cli-options))
(:options
 (parse-opts ["-p" "65537" "-h" "-vvvvv" "--invalid-opt"] cli-options))

(defn run-shell-cmd [ & args]
  (let [{:keys [exit out err] :as result} (apply shell/sh args)]
    (when-not (zero? exit)
      (println "ERROR running command\nSTDOUT:")
      (println out "\nSTDERR:")
      (println err)
      (throw (ex-info "Error while runing shell command" {:status exit})))
    result))


(def cli-options
  [["-p" "--port PORT" "Port number"
    :default 80
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-H" "--hostname HOST" "Remote host"
    :default (InetAddress/getByName "localhost")
    ;; Specify a string to output in the default column in the options summary
    ;; if the default value's string representation is very ugly
    :default-desc "localhost"
    :parse-fn #(InetAddress/getByName %)]
   ;; If no required argument description is given, the option is assumed to
   ;; be a boolean option defaulting to nil
   [nil "--detach" "Detach from controlling process"]
   ["-v" nil "Verbosity level; may be specified multiple times to increase value"
    ;; If no long-option is specified, an option :id must be given
    :id :verbosity
    :default 0
    ;; Use :update-fn to create non-idempotent options (:default is applied first)
    :update-fn inc]
   ["-f" "--file NAME" "File names to read"
    :multi true ; use :update-fn to combine multiple instance of -f/--file
    :default []
    ;; with :multi true, the :update-fn is passed both the existing parsed
    ;; value(s) and the new parsed value from each option
    :update-fn conj]
   ;; A boolean option that can explicitly be set to false
   ["-d" "--[no-]daemon" "Daemonize the process" :default true]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> [""
        "Usage: program-name [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  start    Start a new server"
        "  stop     Stop an existing server"
        "  status   Print a server's status"
        ""
        "Please refer to the manual page for more information."]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing the command:\n\n"
       (string/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors          ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      (and (= 1 (count arguments))
           (#{"start" "stop" "status"} (first arguments)))
      {:action (first arguments) :options options}
      :else           ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [action options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "start"  (identity "start")
        "stop"   (identity "stop")
        "status" (identity "status")))))

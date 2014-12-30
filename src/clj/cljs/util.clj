;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns cljs.util
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.edn :as edn])
  (:import [java.io File]
           [java.net URL]))

;; next line is auto-generated by the build-script - Do not edit!
(def ^:dynamic *clojurescript-version*)

(defn clojurescript-version 
  "Returns clojurescript version as a printable string."
  []
  (if (bound? #'*clojurescript-version*)
    (str
     (:major *clojurescript-version*)
     "."
     (:minor *clojurescript-version*)
     (when-let [i (:incremental *clojurescript-version*)]
       (str "." i))
     (when-let [q (:qualifier *clojurescript-version*)]
       (str "-" q))
     (when (:interim *clojurescript-version*)
       "-SNAPSHOT"))
    ""))

(defn compiled-by-version [^File f]
  (with-open [reader (io/reader f)]
    (let [match (->> reader line-seq first
                     (re-matches #".*ClojureScript (\d+\.\d+-\d+).*$"))]
      (and match (second match)))))

(defn build-options [^File f]
  (with-open [reader (io/reader f)]
    (let [match (->> reader line-seq first
                  (re-matches #".*ClojureScript \d+\.\d+-\d+ (.*)$"))]
          (and match (edn/read-string (second match))))))

(defn munge-path [ss]
  (clojure.lang.Compiler/munge (str ss)))

(defn ns->relpath [s]
  (str (string/replace (munge-path s) \. \/) ".cljs"))

(defn path-seq
  [file-str]
  (->> File/separator
       java.util.regex.Pattern/quote
       re-pattern
       (string/split file-str)))

(defn to-path
  ([parts]
     (to-path parts File/separator))
  ([parts sep]
    (apply str (interpose sep parts))))

(defn ^File to-target-file
  ([target-dir ns-info]
    (to-target-file target-dir ns-info "js"))
  ([target-dir ns-info ext]
    (let [relative-path (string/split (munge-path (str (:ns ns-info))) #"\.")
          parents (cond-> (butlast relative-path)
                    target-dir (conj target-dir))]
      (io/file (io/file (to-path parents))
        (str (last relative-path) (str "." ext))))))

(defn mkdirs
  "Create all parent directories for the passed file."
  [^File f]
  (.mkdirs (.getParentFile (.getCanonicalFile f))))

(defn output-directory
  ([opts] (output-directory opts "out"))
  ([opts default]
    (or (:output-dir opts) default)))

(defn file? [f]
  (instance? File f))

(defn url? [f]
  (instance? URL f))

(defn ^String filename [^File f]
  (.getName f))

(defn ^String path [^URL url]
  (.getPath url))

(defn ^String ext
  "Given a file or url return the file extension."
  [f]
  (let [s (cond
            (file? f) (filename f)
            (url? f) (path f))]
    (last (string/split s #"\."))))

(defn debug-prn
  [& args]
  (.println System/err (apply str args)))

(defmacro measure
  "Like cljs.core/time but toggleable and takes a message string."
  {:added "1.0"}
  ([msg expr] `(measure true ~msg ~expr))
  ([enable msg expr]
    `(if ~enable
       (let [start# (. System (nanoTime))
             ret# ~expr]
         (debug-prn (str ~msg ", elapsed time: " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
         (debug-prn "")
         ret#)
       ~expr)))

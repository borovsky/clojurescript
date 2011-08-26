;;  Copyright (c) Rich Hickey. All rights reserved.
;;  The use and distribution terms for this software are covered by the
;;  Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;  which can be found in the file epl-v10.html at the root of this distribution.
;;  By using this software in any fashion, you are agreeing to be bound by
;;  the terms of this license.
;;  You must not remove this notice, or any other, from this software.

(ns ^{:doc "Receive - Eval - Print - Loop

  Receive a block of JS (presumably generated by a ClojureScript compiler)
  Evaluate it naively
  Print the result of evaluation to a string
  Send the resulting string back to the server Loop!"

      :author "Bobby Calderwood and Alex Redington"}
  clojure.browser.repl
  (:require [clojure.browser.net   :as net]
            [clojure.browser.event :as event]))

(defn log-obj [obj]
  (.log js/console obj))

(defn evaluate-javascript
  "Process a single block of JavaScript received from the server"
  [block]
  (log-obj (str "evaluating: " block))
  (let [result (try {:status :success :value (str (js* "eval(~{block})"))}
                    (catch js/Error e {:status :exception :value (pr-str e)}))]
    (log-obj (str "result: " result))
    (pr-str result)))

(defn send-result [connection url data]
  (net/transmit connection url "POST" data nil 0))

(defn start-evaluator
  "Start the REPL server connection."
  [url]
  (if-let [repl-connection (net/xpc-connection)]
    (let [connection (net/xhr-connection)]
      (event/listen connection
                    :success
                    (fn [e]
                      (net/transmit
                       repl-connection
                       :evaluate-javascript
                       (.getResponseText e/currentTarget
                                         ()))))

      (net/register-service repl-connection
                            :send-result
                            (partial send-result
                                     connection
                                     url))
      (net/connect repl-connection
                   #(log-obj "Child REPL channel connected."))

      (js/setTimeout #(send-result connection url "ready") 50))
    (js/alert "No 'xpc' param provided to child iframe.")))

(defn connect
  "Connects to a REPL server from an HTML document. After the
  connection is made, the REPL will evaluate forms in the context of
  the document that called this function."
  [repl-server-url]
  (let [repl-connection (net/xpc-connection
                         {:peer_uri repl-server-url})]
    (net/register-service repl-connection
                          :evaluate-javascript
                          (fn [js]
                            (net/transmit
                             repl-connection
                             :send-result
                             (evaluate-javascript js))))
    (net/connect repl-connection
                 #(log-obj "Parent REPL channel connection.")
                 (fn [iframe]
                   (set! iframe.style.display
                         "none")))))

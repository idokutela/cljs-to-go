(ns to-go.core
  "
  # A cps transformer

  A very simple utility that allows one to use node-style async
  functions in clojurescript go blocks. It does *not* use channels,
  but returns control to the go-block *immediately* when the function
  calls its continuation.

  Node-style async functions are functions `f` with the signature
  `(...args, cb)`, where `cb`, the *continuation*, is called when the
  function is done. For the purposes of this library, `cb` has the
  signature `(err, ...vals)`. If calling `f` results in an error, `cb`
  is called with `err` non-null, and `vals` are ignored. On success,
  `err` is `null`, and `...vals` contain the result of the computation.

  This library exposes two macros: `<node` and `<<node`. Both **must**
  be used inside go blocks. The macros take as arguments a node-style
  function `f`, and whatever arguments `f` should be called with. They
  arrange that `f` is called with these arguments, and on completion,
  do one of the following:

   - if `f` is completed with an error, the error is thrown,

   - if `f` is completed with 0 values, control is returned to the go
     block with the value `nil`,

   - if `f` is completed with 1 value, control is returned to the go
     block with that value,

   - otherwise, control is returned to the go block with the values
     returned wrapped in a list.

  `<node` and `<<node` differ only in one respect: `<<node` returns
  control flow *immediately* on completion of `f`, whereas `<node` may
  only return control later in the event-loop.

  **Warning** : *In order to accomplish the immediate return of
  control, `<<node` uses internal implementation details in
  `core.async`. Only use `<<node` if you know you need the immediacy,
  and if you do, make sure to pin the version of `core.async` so as to
  avoid unexpected breakages.*

  An example:

      (ns some.module
        (:require
          [cljs.core.async :refer [go]]
          [fs :as fs]
          [to-go.core :refer [<node]]))

      (go
        (try
          (let [contents (<node (.-read fs) \\\"somefile.txt\\\")]
            (print contents)
	    (catch Object e
	      (println \"Error reading file!\"))))
  "
  (:require
   [cljs.core.async :as async]
   [cljs.core.async.impl.protocols :as impl])
  (:require-macros [to-go.core :refer [<node <<node]]))

(defn -as-deref [x]
  "INTERNAL API: Wraps `x` in a deref."
  (reify cljs.core/IDeref
    (-deref [_] x)))



(defn -handle-result
  "INTERNAL API: processes the result of a node-function call."
  [[err result]]
  (if (some? err) (throw err) result))



;;; ReadPorts have one method: take!
;;;
;;; This takes a single argument, the handler: a function that is used
;;; to notify the listener when a value is available.
;;;
;;; If the value is not immediately available, nil is
;;; returned. Otherwise, one returns a deref boxing the value.
;;;
;;; If one receives the value later, one should notify the
;;; handler. This has two stages: first, one checks that it is active,
;;; with impl/active?. If so, one commits the value. This is done in
;;; two stages: first one gets the committer by calling impl/commit on
;;; the handler, then one calls the result with the (unboxed) value.


(defn -ReadPort<-node
  "INTERNAL API: Returns a ReadPort that wraps node function call."
  [f & args]
  (let [thunk (apply partial f args)
        val (atom nil)
        the-handler (atom nil)
        port (reify
               impl/ReadPort
               (take! [_ ^not-native handler]
                 (if (not ^boolean (impl/active? handler))
                   nil
                   (if-some [v @val]
                     (do
                       #_((impl/commit handler) v)
                       (-as-deref v))
                     (do
                       (assert (nil? @the-handler)
                               "One may only take once from a node fn!")
                       (reset! the-handler handler)
                       nil)))))]
    (thunk (fn [err & res]
             (let [res (if (<= (count res) 1)
                         (first res)
                         res)]
               (reset! val [err res])
               (when-some [handler @the-handler]
                 (reset! the-handler nil)
                 (when (impl/active? handler)
                   ((impl/commit handler) @val))))))
    port))

(defn ch<-node
  "INTERNAL API: calls a node function and returns a channel with the result."
  [f & args]
  (let [ch (async/promise-chan)
        thunk (apply partial f args)]
    (thunk
     (fn [err & res]
       (let [res (if (<= (count res) 1)
                   (first res)
                   res)]
         (async/put! ch [err res]))))
    ch))

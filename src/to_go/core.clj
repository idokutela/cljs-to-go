(ns to-go.core)

(defmacro <node
  "Calls node-style `f` with `args`, and places the result in the go block."
  [f & args]
  `(-handle-result
    (cljs.core.async/<!
     (ch<-node ~f ~@args))))

(defmacro <<node
  "Calls node-style `f` with `args`, and places the result in the go block immediately on completion."
  [f & args]
  `(-handle-result
    (cljs.core.async/<!
     (-ReadPort<-node ~f ~@args))))

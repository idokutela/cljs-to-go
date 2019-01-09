# A cps transformer

A very simple utility that allows one to use node-style async
functions in clojurescript go blocks. It does *not* use channels, but
returns control to the go-block *immediately* when the function calls
its continuation.

Node-style async functions are functions `f` with the signature
`(...args, cb)`, where `cb`, the *continuation*, is called when the
function is done. For the purposes of this library, `cb` has the
signature `(err, ...vals)`. If calling `f` results in an error, `cb`
is called with `err` non-null, and `vals` are ignored. On success,
`err` is `null`, and `...vals` contain the result of the computation.

This library exposes two macros: `<node` and `<<node`. Both **must**
be used inside go blocks. The macros take as arguments a node-style
function `f`, and whatever arguments `f` should be called with. They
arrange that `f` is called with these arguments, and on completion, do
one of the following:

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

**Warning** : *In order to accomplish the immediate return of control,
`<<node` uses internal implementation details in `core.async`. Only
use `<<node` if you know you need the immediacy, and if you do, make
sure to pin the version of `core.async` so as to avoid unexpected
breakages.*

## Example

```cljs
(go
  (try
    (let [filecontents (<node (.-read fs) \"somefile.txt\")]
      #_(... do something with the contents))
	  (catch Object e
	    (println \"Error reading file!\"))))
```

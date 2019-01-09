(ns to-go.test
  (:require [to-go.core :refer [<node <<node]]
            [cljs.test :refer-macros [deftest is testing async]]
            [cljs.core.async :refer [go <!]]))

(deftest <node-test-immediate-cb
  (async
   done
   (testing "<node returns control for node-style functions which callback immediately"
     (let [f (fn [& args]
               (let [cb (last args)
                     args (butlast args)]
                 (apply cb args)))]
       (go
         (is (nil? (<node f)))
         (is (= 32 (<node f nil 32)))
         (is (= '(foo bar baz) (<node f nil 'foo 'bar 'baz)))
         (try (<node f (js/Error. "Footloose"))
              (assert nil)
              (catch js/Object e (is (= "Footloose" (.-message e)))))
         (try (<node f (js/Error. "Foo") 32)
              (assert nil)
              (catch js/Object e (is (= "Foo" (.-message e)))))
         (try (<node f (js/Error. "Bar") 32 12 "Hello")
              (assert nil)
              (catch js/Object e (is (= "Bar" (.-message e)))))
         (println "Done?")
         (done))))))


(deftest <node-test-delayed-cb
  (async
   done
   (testing "<node returns control for node-style functions that delay callback"
     (let [f (fn [& args]
               (let [cb (last args)
                     args (butlast args)]
                 (-> js/Promise .resolve
                     (.then #(apply cb args)))))]
       (go
         (is (nil? (<node f)))
         (is (= 32 (<node f nil 32)))
         (is (= '(foo bar baz) (<node f nil 'foo 'bar 'baz)))
         (try (<node f (js/Error. "Footloose"))
              (assert nil)
              (catch js/Object e (is (= "Footloose" (.-message e)))))
         (try (<node f (js/Error. "Foo") 32)
              (assert nil)
              (catch js/Object e (is (= "Foo" (.-message e)))))
         (try (<node f (js/Error. "Bar") 32 12 "Hello")
              (assert nil)
              (catch js/Object e (is (= "Bar" (.-message e)))))
         (println "Done?")
         (done))))))


(deftest <<node-test-immediate-cb
  (async
   done
   (testing "<<node returns control for node-style functions which callback immediately"
     (let [f (fn [& args]
               (let [cb (last args)
                     args (butlast args)]
                 (apply cb args)))]
       (go
         (is (nil? (<<node f)))
         (is (= 32 (<<node f nil 32)))
         (is (= '(foo bar baz) (<<node f nil 'foo 'bar 'baz)))
         (try (<<node f (js/Error. "Footloose"))
              (assert nil)
              (catch js/Object e (is (= "Footloose" (.-message e)))))
         (try (<<node f (js/Error. "Foo") 32)
              (assert nil)
              (catch js/Object e (is (= "Foo" (.-message e)))))
         (try (<<node f (js/Error. "Bar") 32 12 "Hello")
              (assert nil)
              (catch js/Object e (is (= "Bar" (.-message e)))))
         (done))))))


(deftest <<node-test-delayed-cb
  (async
   done
   (testing "<<node returns control for node-style functions that delay callback"
     (let [f (fn [& args]
               (let [cb (last args)
                     args (butlast args)]
                 (-> js/Promise .resolve
                     (.then #(apply cb args)))))]
       (go
         (is (nil? (<<node f)))
         (is (= 32 (<<node f nil 32)))
         (is (= '(foo bar baz) (<<node f nil 'foo 'bar 'baz)))
         (try (<<node f (js/Error. "Footloose"))
              (assert nil)
              (catch js/Object e (is (= "Footloose" (.-message e)))))
         (try (<<node f (js/Error. "Foo") 32)
              (assert nil)
              (catch js/Object e (is (= "Foo" (.-message e)))))
         (try (<<node f (js/Error. "Bar") 32 12 "Hello")
              (assert nil)
              (catch js/Object e (is (= "Bar" (.-message e)))))
         (done))))))


(deftest <<node-test-immediate-return
  (async
   done
   (testing "<<node returns control immediately"
     (let [val (atom 0)
           f (fn [& args]
               (let [cb (last args)
                     args (butlast args)]
                 (-> js/Promise .resolve
                     (.then #(apply cb args)))
                 (-> js/Promise .resolve (.then #(swap! val inc)))))

           g (fn [& args]
               (let [cb (last args)
                     args (butlast args)]
                 (-> js/Promise .resolve (.then #(swap! val inc)))
                 (apply cb args)))]

       (go
         (<<node f)
         ; the increment has not had a chance
         (is (= 0 @val))
         (<<node g)
         ; the increment has not had a chance
         (is (= 0 @val))
         (<<node g)
         ; the increment has not had a chance
         (is (= 0 @val))
         (<<node f)
         ; the pending increments go because the function blocks internally,
         ; but the new one does not
         (is (= 3 @val))
         (done))))))

(ns dmm.examples.nov-4-2016-experiment
  (:require [dmm.core :as dc
             :refer [v-accum v-identity
                     down-movement up-movement
                     rec-map-sum]]
            [clojure.core.async :as async]))

;;; building on top of oct_28_2016_experiment experience we are now
;;; trying to implement something similar to the duplicate
;;; characters detector from the "Programming Patterns" paper

(def interface-channel (async/chan))

;;; reader is a neuron type, but we are only going to have one neuron
;;; of this type, because it uses this particular channel

(defn reader [dummy]
  (let [reader-output {:signal ((async/alts!! [interface-channel] :default {}) 0)}]
   (clojure.pprint/pprint reader-output)
   reader-output))

(def v-reader (var reader))

;;; writer is a gadget to send smth to that neuron

(defn writer [input-string delay-ms]
  (loop [input-chars (seq input-string)]
    (if (seq input-chars)
      (do
        (Thread/sleep delay-ms)
        (async/>!! interface-channel {(first input-chars) 1}) ; (clojure.pprint/pprint {(first input-chars) 1})
        (recur (rest input-chars))))))

;;; printer is a neuron type, an active neuron of this type
;;; prints its input om each iteration; otherrwise it is an identity neuron

(defn printer [input]
  (clojure.pprint/pprint input)
  input)

(def v-printer (var printer))

;;; the network 4 neurons

(def the-network-matrix-hook
  {v-accum {:self {:accum {v-accum {:self {:single 1}}}}}})

(def the-reader-hook ; let's hook it from the network matrix itself
  {v-reader {:the-reader {:dummy {v-accum {:self {:single 1}}}}}})

(def the-reader-accum
  {v-accum {:the-reader-accum {:accum {v-accum {:the-reader-accum {:single 1}}}}}})

(def the-printer ; let's hook it from the reader accum (it will be silent
                 ; until it becomes non-zero, which is OK
  {v-printer {:the-printer {:to-print {v-accum {:the-reader-accum {:single 1}}}}}})

;;; connect the-reader to an input of the-reader-accum

(def input-to-accum-link
  {v-accum {:the-reader-accum {:delta {v-reader {:the-reader {:signal 1}}}}}})

;;; initial network

(def start-matrix
  (rec-map-sum the-network-matrix-hook the-reader-hook the-reader-accum
                        the-printer input-to-accum-link))

; (def init-output {v-accum {:self {:single init-matrix}}})

(def init-output
  {v-accum {:self {:single start-matrix}}})

;;; a network cycle with delay

(defn network-cycle [initial-output delay-ms]
   (Thread/sleep delay-ms)
   (up-movement (down-movement initial-output)))

(defn network-run [initial-output delay-ms n-iter]
  (loop [n 1
         current-output initial-output]
    (if (< n n-iter)
      (recur (inc n) (network-cycle current-output delay-ms)))))

;;; (network-run init-output 1000 10)

;;; to run an experiment

;;; (future (network-run init-output 1000 300))  ; run for 5 map-indexed

;;; ;;; (writer "hey 235" 4321) ; only do this when a network or other consumer
                                ; for this channel is running to avoid blocking;
                                ; make sure that the delay is larger than
                                ; the network delay

;;; **** a fragment of log from proto-repl:

;;; {:signal {:step 1}}
;;; {:to-print {:step 11}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 12}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 13}}
;;; dmm.examples.oct-28-2016-experiment=>
;;; true 
;;; {:signal {:a 1}}
;;; {:to-print {:step 14}}
;;; {:signal {:step 1}}
;;; :to-print {:step 15}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 15, :a 1}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 16, :a 1}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 17, :a 1}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 18, :a 1}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 19, :a 1}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 20, :a 1}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 21, :a 1}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 22, :a 1}}
;;; dmm.examples.oct-28-2016-experiment=>
;;; true 
;;; {:signal {:a 1}}
;;; {:to-print {:step 23, :a 1}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 24, :a 1}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 24, :a 2}}
;;; {:signal {:step 1}}
;;; {:to-print {:step 25, :a 2}}

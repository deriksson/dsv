;;; se/abc/dsv/read.clj: Delimiter-separated values parser

;; by Daniel Eriksson, daniel.eriksson@abc.se
;; 12 July 2009

;; Copyright (c) Daniel Eriksson, 2009. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns #^{:author "Daniel Eriksson",
       :doc "Delimiter-separated values parser

            This library parses delimiter-separated values (DSV) and renders them as collections.",
       :see-also [["http://en.wikipedia.org/wiki/Delimiter-separated_values", "Delimiter-separeted values"]
		  ["http://en.wikipedia.org/wiki/Comma-separated_values", "Comma-separated values"]
		  ["http://tools.ietf.org/html/rfc4180", "RFC 4180"]]}
  se.abc.dsv.read 
  (:use [clojure.test :only (deftest- is are)] 
	[clojure.contrib.error-kit :as err :only (deferror with-handler handle)]))

(err/deferror malformed-csv [] [msg]
	      {:msg msg})

(defn- pos [c tuple relation]
  (str "(" 
       "Line: " (inc (count relation)) ", " 
       "Column: " (inc (count tuple)) ", "
       "Character: \"" c "\")"))

;; This function is implemented as a finite state machine. There are four states
;; (:escaped, :relation, :tuple, :quotation), and five signals (character, carriage
;; return, delimiter, end-of-file, end-of-line and quote). Every combination of 
;; state and signal maps to an action and a new state, unless the transition
;; leads to an end state or the signal is not allowed in the state context.
;; 
;; References: 
;; Finite state machine (Wikipedia), http://en.wikipedia.org/wiki/Finite-state_machine
(defn read-csv
  "Parses comma separated values (CSV) and returns a vector of vectors of strings
   representing a dataset.

   Raises malformed-csv error if the input is not well formed. In valid CSV the number
   of fields should be the same on each line. This is not enforced."
  ([csv] (read-csv csv \,))
  ([csv delim] (read-csv csv delim \"))
  ([csv delim quote]
     (loop [state :tuple, s csv, field "", tuple [], relation []]
       (let [c (first s)]
	 (cond (= state :escaped)
	       (cond (= c \return)  (recur :escaped (rest s) field tuple relation)
		     (= c delim)    (recur :tuple (rest s) "" (conj tuple field) relation)
		     (nil? c)       (conj relation (conj tuple field))
		     (= c \newline) (recur :relation (rest s) "" [] (conj relation (conj tuple field)))
		     (= c quote)    (recur :quotation (rest s) (str field c) tuple relation)
		     :else          (err/raise malformed-csv (str "Invalid character after escape " 
								  (pos c tuple relation) ".")))
	       (= state :relation)
	       (cond (= c \return)  (recur :tuple (rest s) field tuple relation)
		     (= c delim)    (recur :tuple (rest s) "" (conj tuple field) relation)
		     (nil? c)       relation
		     (= c \newline) (recur :relation (rest s) "" [] (conj relation (conj tuple field)))
		     (= c quote)    (recur :quotation (rest s) field tuple relation)
		     :else          (recur :tuple (rest s) (str field c) tuple relation))
	       (= state :tuple)
	       (cond (= c \return)  (recur :tuple (rest s) field tuple relation)
		     (= c delim)    (recur :tuple (rest s) "" (conj tuple field) relation)
		     (nil? c)       (conj relation (conj tuple field))
		     (= c \newline) (recur :relation (rest s) "" [] (conj relation (conj tuple field)))
		     (= c quote)    (recur :quotation (rest s) field tuple relation)
		     :else          (recur :tuple (rest s) (str field c) tuple relation))
	       (= state :quotation)
	       (cond (= c \return)  (recur :quotation (rest s) (str field c) tuple relation)
		     (= c delim)    (recur :quotation (rest s) (str field c) tuple relation)
		     (nil? c)       (err/raise malformed-csv "Quote not closed.")
		     (= c \newline) (recur :quotation (rest s) (str field c) tuple relation)
		     (= c quote)    (recur :escaped (rest s) field tuple relation)
		     :else          (recur :quotation (rest s) (str field c) tuple relation))
	       :else (err/raise malformed-csv (str "Invalid state: \"" state "\" " (pos c tuple relation) ".")))))))

;;; TESTS

;; Run these tests with the following commands:
;; (require :reload-all 'se.abc.dsv.read)
;; (clojure.test/run-tests 'se.abc.dsv.read)

;; Bind clojure.test/*load-tests* to false to omit these
;; tests from production code.

(deftest- handles-custom-delim
  (are [in delim out] (= (read-csv in delim) out)  
       "a,b\n" \, [["a" "b"]]
       "a;b\n" \; [["a" "b"]]
       "a\tb\n" \tab [["a" "b"]]
       "a|b\n" \| [["a" "b"]]))

(deftest- parses-valid-csv
  (are [in out] (= (read-csv in) out)
       "a" [["a"]]
       "a,b" [["a" "b"]]
       "aaa,bbb,ccc" [["aaa" "bbb" "ccc"]]
       "a,b,c,d," [["a" "b" "c" "d" ""]]
       "a\n" [["a"]]
       "a,b\n" [["a" "b"]]
       "aaa,bbb,ccc\n" [["aaa" "bbb" "ccc"]]
       "a,b,c,d,\n" [["a" "b" "c" "d" ""]]
       "a,b\np,q\n" [["a" "b"]["p" "q"]]
       "a,b\np,q" [["a" "b"]["p" "q"]]
       "\",\"" [[","]]
       "\"\r\"" [["\r"]]
       "a,b\r\np,q" [["a" "b"]["p" "q"]]
       "\"\"\"\"" [["\""]]
       "a,b\n" [["a" "b"]]
       "a,b\nc,d\n" [["a" "b"]["c" "d"]]
       "\"a\",b\n" [["a" "b"]]
       "\"\n\",b\n" [["\n" "b"]]
       "\",\",b\n" [["," "b"]]
       "\",\",b\r\n" [["," "b"]]
       "a,b\r\nc,d\r\n" [["a" "b"]["c" "d"]]
       "\"\r\",b\n" [["\r" "b"]]
       "a,b\nc,d" [["a" "b"]["c" "d"]]
       "a,b,c\np,q,r\nx,y,z" [["a" "b" "c"]["p" "q" "r"]["x" "y" "z"]]
       "\"\"\"a\"\"\",b" [["\"a\"" "b"]]
       ))

(deftest- rejects-malformed-csv
  (are [txt] (nil? (err/with-handler (read-csv txt) (err/handle malformed-csv [msg] nil)))
       "\"\"\""
       "a\n\r"))

;;; se/abc/dsv/write.clj: Delimiter-separated values generator

;; by Daniel Eriksson, daniel.eriksson@abc.se
;; 9 July 2009

;; Copyright (c) Daniel Eriksson, 2009. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns #^{:author "Daniel Eriksson",
       :doc "Delimiter-separated values generator

            This library generates delimiter-separated values (DSV) from collections. The 
            client specifies delimiters and strategies for protection from delimiter 
            collision. There is a convenience function for generating comma-separated values 
            (CSV), a special case of DSV.",
       :see-also [["http://en.wikipedia.org/wiki/Delimiter-separated_values", 
		   "Delimiter-separeted values"]
		  ["http://en.wikipedia.org/wiki/Comma-separated_values", 
		   "Comma-separated values"]
		  ["http://tools.ietf.org/html/rfc4180", 
		   "RFC 4180"]]}
  se.abc.dsv.write 
  (:require [clojure.contrib.str-utils2 :as s :only (replace)])
  (:use [clojure.test :only (deftest- is are)]
	[clojure.contrib.str-utils :only (str-join)]))

(defn- dsv 
  "Generates delimiter-separated values (DSV) from a two-dimensional collection.

  Normally only two-dimensional DSV is useful, but any dimension is supported. 
  The number of dimensions is determined by the dataset. The client should specify
  as many delimiters as there are dimensions in the dataset.

  The first arg contains a dataset, e.g. a vector of vectors. The second arg is a
  function that prevents delimiter collision, for instance by embedding or escaping 
  field values. The following arguments are delimiters. In a two-dimensional 
  dataset the first delimiter separates records and the second fields.

  Example:
  (dsv [[1 \"p\"][2 \"q\"]] #(str \"\\\"\" % \"\\\"\") \"\\n\" \",\")"
  [dataset collision & delim] 
  (if (coll? dataset) 
    (str-join (first delim) 
	      (map #(apply dsv % collision (rest delim)) dataset))
    (collision dataset)))

(defn- enclose [value re quote] 
  (if (re-find re value) (str quote value quote) value))

(defn- escape [quote s] 
  (s/replace s (str quote) (str quote quote)))

;; TODO: Escape the quote parameter in the regular expression when necessary.
(defn- csv-collision [quote] 
  (fn [val] 
    (if (string? val) 
      (enclose ((partial escape quote) val) (re-pattern (str "[,\n" quote "]")) quote)
      val)))

;; TODO: Parameterise the delimiter collision function using multimethods.
(defn write-csv
  "Generates comma-separated values (CSV) from a two-dimensional collection.

  Specification of CSV format:
  1. Each record is one line terminated by a line feed.
  2. Fields are separated by commas. 
  3. Fields with embedded commas, double-quote characters or line breaks
     are enclosed within double-quote characters.
  4. Embedded double-quote characters are represented by a pair of double-quote
     characters.

  Example:
  (write-csv [[1 \"a\"][2 \"b\"]])"
  ([dataset] (write-csv dataset \,))
  ([dataset delim] (write-csv dataset delim \"))
  ([dataset delim quote]
     (dsv dataset (csv-collision quote) \newline delim)))

;;; TESTS

;; Run these tests with the following commands:
;; (require :reload-all 'se.abc.dsv.write)
;; (clojure.test/run-tests 'se.abc.dsv.write)

;; Bind clojure.test/*load-tests* to false to omit these
;; tests from production code.

(deftest- generates-simple-dsv
  (is (= "a,b\nc,d" (dsv [["a" "b"]["c" "d"]] str "\n" ","))))

(deftest- escapes-comma 
  (is (= "\"a,\",b\nc,d" 
	 (dsv [["a," "b"]["c" "d"]] #(enclose % #"," \") "\n" ","))))

(deftest- encloses-comma 
  (is (= "\"a,\"" (enclose "a," #"[\",\n]" \"))))

(deftest- encloses-lf 
  (is (= "\"a\n\"" (enclose "a\n" #"[\",\n]" \"))))

(deftest- does-not-enclose 
  (is (= "a"(enclose "a" #"[\",\n]" \"))))

(deftest- generates-three-dimensions
  (is (= "cedar,d;cedar,d\ncedar,d;cedar,d" 
	 (dsv [[["cedar" "d"]["cedar" "d"]][["cedar" "d"]["cedar" "d"]]] 
	      str "\n" ";" ","))))

(deftest- generates-csv
  (are [in out] (= (write-csv in) out)  
       [["a," "b"]["c" "d"]] "\"a,\",b\nc,d" 
       [["a" "b"]["c" "d"]] "a,b\nc,d" 
       [["a\n" "b"]["c" "d"]] "\"a\n\",b\nc,d"
       [["\"a\"" "b"]["c" "d"]] "\"\"\"a\"\"\",b\nc,d"
       [[1 "b"][2 "d"]] "1,b\n2,d"))

(deftest- handles-delimiters
  (are [in delim out] (= (write-csv in delim) out)  
       [["a," "b"]["c" "d"]] \, "\"a,\",b\nc,d" 
       [["a" "b"]["c" "d"]] \, "a,b\nc,d" 
       [["a\n" "b"]["c" "d"]] \, "\"a\n\",b\nc,d"
       [["\"a\"" "b"]["c" "d"]] \, "\"\"\"a\"\"\",b\nc,d"
       [[1 "b"][2 "d"]] \, "1,b\n2,d"
       [[1 "b"][2 "d"]] \| "1|b\n2|d"
       [[1 "b"][2 "d"]] \; "1;b\n2;d"))

(deftest- handles-quotes
  (are [in quote out] (= (write-csv in \, quote) out)  
       [["a," "b"]["c" "d"]] \' "'a,',b\nc,d" 
       [["a," "b"]["c" "d"]] \" "\"a,\",b\nc,d"
       [["a'" "b"]["c" "d"]] \' "'a''',b\nc,d"))

(deftest- escapes
  (are [in out] (= (escape \" in) out) 
       "\"" "\"\""
       "a" "a"))
